package ru.zetov.hcaptcha.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.zetov.hcaptcha.Main;
import ru.zetov.hcaptcha.model.CaptchaSession;
import ru.zetov.hcaptcha.model.CaptchaSound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CaptchaListener implements Listener {

    private final Main plugin;
    private final Map<UUID, CaptchaSession> activeCaptcha = new HashMap<>();

    public CaptchaListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long lastPassed = plugin.database.getLastPassed(player.getUniqueId());
            long cooldown = plugin.configManager.captchaCooldown;
            if (System.currentTimeMillis() - lastPassed < cooldown) return;

            Bukkit.getScheduler().runTask(plugin, () -> startCaptcha(player));
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (activeCaptcha.containsKey(event.getPlayer().getUniqueId()) &&
                event.getFrom().distanceSquared(Objects.requireNonNull(event.getTo())) > 0) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (activeCaptcha.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.configManager.messageNoCommand);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (activeCaptcha.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.configManager.messageNoMenu);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activeCaptcha.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> handleAnswer(player, message));
    }

    private void startCaptcha(Player player) {
        int attempts = plugin.configManager.maxAttempts;
        CaptchaSound question = CaptchaSound.random(plugin);

        CaptchaSession session = new CaptchaSession(question, attempts);
        activeCaptcha.put(player.getUniqueId(), session);

        player.sendMessage(plugin.configManager.messageQuestion);
        sendOptions(player, question);

        startRepeatingSound(player, session);
        startTimeoutTimer(player, session);
    }

    private void sendOptions(Player player, CaptchaSound question) {
        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            player.sendMessage(" §7" + (i + 1) + ". §a" + options.get(i));
        }
    }

    private void startRepeatingSound(Player player, CaptchaSession session) {
        if (session.getRepeatTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getRepeatTaskId());
        }

        CaptchaSound question = session.getCurrentQuestion();
        question.playSound(player);

        long delayTicks = plugin.configManager.resendDelayMillis / 50;
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!session.isActive()) {
                Bukkit.getScheduler().cancelTask(session.getRepeatTaskId());
                return;
            }
            question.playSound(player);
        }, delayTicks, delayTicks);

        session.setRepeatTaskId(taskId);
    }

    private void startTimeoutTimer(Player player, CaptchaSession session) {
        long timeoutTicks = plugin.configManager.timeoutMillis / 50;
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (session.isActive() && activeCaptcha.containsKey(player.getUniqueId())) {
                player.kickPlayer(plugin.configManager.messageTimeout);
                stopCaptcha(player, session);
            }
        }, timeoutTicks);

        session.setTimeoutTaskId(taskId);
    }

    private void handleAnswer(Player player, String message) {
        CaptchaSession session = activeCaptcha.get(player.getUniqueId());
        if (session == null) return;

        CaptchaSound question = session.getCurrentQuestion();
        boolean isCorrect = false;

        if (message.matches("\\d+")) {
            try {
                int index = Integer.parseInt(message) - 1;
                List<String> options = question.getOptions();
                if (index >= 0 && index < options.size()) {
                    String selectedOption = options.get(index);
                    isCorrect = question.isCorrect(selectedOption);
                }
            } catch (NumberFormatException ignored) {}
        } else {
            isCorrect = question.isCorrect(message);
        }

        if (isCorrect) {
            player.sendMessage(plugin.configManager.messageCorrect);
            stopCaptcha(player, session);
            plugin.database.setLastPassed(player.getUniqueId(), System.currentTimeMillis());
        } else {
            handleWrongAnswer(player, session);
        }
    }

    private void handleWrongAnswer(Player player, CaptchaSession session) {
        session.decrementAttempts();
        if (session.getAttempts() <= 0) {
            player.kickPlayer(plugin.configManager.messageKick);
            stopCaptcha(player, session);
            return;
        }

        player.sendMessage(plugin.configManager.messageWrong);

        CaptchaSound newQuestion = CaptchaSound.random(plugin);
        session.setCurrentQuestion(newQuestion);
        sendOptions(player, newQuestion);
        startRepeatingSound(player, session);
    }

    private void stopCaptcha(Player player, CaptchaSession session) {
        session.stop();
        if (session.getRepeatTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getRepeatTaskId());
        }
        if (session.getTimeoutTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getTimeoutTaskId());
        }
        activeCaptcha.remove(player.getUniqueId());
    }
}
