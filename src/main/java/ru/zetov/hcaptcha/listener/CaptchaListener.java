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
import ru.zetov.hcaptcha.model.CaptchaSound;
import ru.zetov.hcaptcha.model.CaptchaSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaListener implements Listener {

    private final Main plugin;
    private final Map<UUID, CaptchaSession> activeCaptcha = new ConcurrentHashMap<>();

    public CaptchaListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long lastPassed = plugin.database.getLastPassed(player.getUniqueId());
            long cooldown = plugin.configManager.captchaCooldown;
            if (System.currentTimeMillis() - lastPassed >= cooldown) {
                Bukkit.getScheduler().runTask(plugin, () -> startCaptcha(player));
            }
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        if (activeCaptcha.containsKey(player.getUniqueId())
                && event.getTo() != null
                && event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (activeCaptcha.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.configManager.messageNoCommand);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (activeCaptcha.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.configManager.messageNoMenu);
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
        CaptchaSound question = CaptchaSound.random(plugin);
        CaptchaSession session = new CaptchaSession(question, plugin.configManager.maxAttempts);
        activeCaptcha.put(player.getUniqueId(), session);

        player.sendMessage(plugin.configManager.messageQuestion);
        sendOptions(player, question);
        session.startTasks(plugin, player);
    }

    private void sendOptions(Player player, CaptchaSound question) {
        List<String> options = question.getOptions();
        for (int i = 0; i < options.size(); i++) {
            player.sendMessage(" §7" + (i + 1) + ". §a" + options.get(i));
        }
    }

    private void handleAnswer(Player player, String message) {
        CaptchaSession session = activeCaptcha.get(player.getUniqueId());
        if (session == null) return;

        CaptchaSound question = session.currentQuestion;
        boolean correct = false;

        if (message.matches("\\d+")) {
            int index = Integer.parseInt(message) - 1;
            if (index >= 0 && index < question.getOptions().size()) {
                correct = question.isCorrect(question.getOptions().get(index));
            }
        } else {
            correct = question.isCorrect(message);
        }

        if (correct) {
            player.sendMessage(plugin.configManager.messageCorrect);
            plugin.database.setLastPassed(player.getUniqueId(), System.currentTimeMillis());
            stopCaptcha(player, session);
        } else {
            handleWrongAnswer(player, session);
        }
    }

    private void handleWrongAnswer(Player player, CaptchaSession session) {
        if (!session.decrementAndCheck()) {
            player.kickPlayer(plugin.configManager.messageKick);
            stopCaptcha(player, session);
            return;
        }

        player.sendMessage(plugin.configManager.messageWrong);
        session.currentQuestion = CaptchaSound.random(plugin);
        sendOptions(player, session.currentQuestion);
    }

    private void stopCaptcha(Player player, CaptchaSession session) {
        session.cancelAllTasks();
        activeCaptcha.remove(player.getUniqueId());
    }
}
