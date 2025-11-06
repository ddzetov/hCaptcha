package ru.zetov.hcaptcha.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import ru.zetov.hcaptcha.Main;
import ru.zetov.hcaptcha.utils.ConfigManager;
import ru.zetov.hcaptcha.model.CaptchaSession;
import ru.zetov.hcaptcha.model.CaptchaSound;

import java.util.*;

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
            long lastPassed = plugin.getDatabase().getLastPassed(player.getUniqueId());
            long cooldown = plugin.getConfigManager().getCaptchaCooldown();
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
            player.sendMessage(plugin.getConfigManager().getMessageNoCommand());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();

        if (activeCaptcha.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getMessageNoMenu());
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
        ConfigManager cfg = plugin.getConfigManager();
        int attempts = cfg.getMaxAttempts();
        CaptchaSound question = CaptchaSound.random();

        CaptchaSession session = new CaptchaSession(question, attempts);
        activeCaptcha.put(player.getUniqueId(), session);

        player.sendMessage(cfg.getMessageQuestion());
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

        long delayTicks = plugin.getConfigManager().getResendDelayMillis() / 50;
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
        long timeoutTicks = plugin.getConfigManager().getTimeoutMillis() / 50;

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (session.isActive() && activeCaptcha.containsKey(player.getUniqueId())) {
                player.kickPlayer(plugin.getConfigManager().getMessageTimeout());
                stopCaptcha(player, session);
            }
        }, timeoutTicks);

        session.setTimeoutTaskId(taskId);
    }

    private void handleAnswer(Player player, String message) {
        CaptchaSession session = activeCaptcha.get(player.getUniqueId());
        if (session == null) return;

        String answer = message.trim();
        List<String> options = session.getCurrentQuestion().getOptions();
        boolean isCorrect = false;

        if (answer.matches("\\d+")) {
            try {
                int index = Integer.parseInt(answer) - 1;
                if (index >= 0 && index < options.size()) {
                    String selectedOption = options.get(index);
                    isCorrect = session.getCurrentQuestion().isCorrect(selectedOption);
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        } else {
            isCorrect = session.getCurrentQuestion().isCorrect(answer);
        }

        if (isCorrect) {
            player.sendMessage(plugin.getConfigManager().getMessageCorrect());
            stopCaptcha(player, session);
            plugin.getDatabase().setLastPassed(player.getUniqueId(), System.currentTimeMillis());
        } else {
            handleWrongAnswer(player, session);
        }
    }

    private void handleWrongAnswer(Player player, CaptchaSession session) {
        session.decrementAttempts();
        if (session.getAttempts() <= 0) {
            player.kickPlayer(plugin.getConfigManager().getMessageKick());
            stopCaptcha(player, session);
            return;
        }

        player.sendMessage(plugin.getConfigManager().getMessageWrong());

        CaptchaSound newQuestion = CaptchaSound.random();
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