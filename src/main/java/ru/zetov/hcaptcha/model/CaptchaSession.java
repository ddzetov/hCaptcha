package ru.zetov.hcaptcha.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.zetov.hcaptcha.Main;

import java.util.concurrent.atomic.AtomicInteger;

public class CaptchaSession {

    public CaptchaSound currentQuestion;
    private final AtomicInteger attempts;
    private int repeatTaskId = -1;
    private int timeoutTaskId = -1;
    private boolean active = true;

    public CaptchaSession(CaptchaSound question, int attempts) {
        this.currentQuestion = question;
        this.attempts = new AtomicInteger(attempts);
    }

    public void startTasks(Main plugin, Player player) {
        long repeatDelay = plugin.configManager.resendDelayMillis / 50;
        long timeoutDelay = plugin.configManager.timeoutMillis / 50;

        currentQuestion.playSound(player);

        repeatTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!active) {
                cancelAllTasks();
                return;
            }
            currentQuestion.playSound(player);
        }, repeatDelay, repeatDelay);

        timeoutTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (active) {
                player.kickPlayer(plugin.configManager.messageTimeout);
                cancelAllTasks();
            }
        }, timeoutDelay);
    }

    public boolean decrementAndCheck() {
        return attempts.decrementAndGet() > 0;
    }

    public void cancelAllTasks() {
        if (!active) return;
        active = false;

        if (repeatTaskId != -1) {
            Bukkit.getScheduler().cancelTask(repeatTaskId);
            repeatTaskId = -1;
        }
        if (timeoutTaskId != -1) {
            Bukkit.getScheduler().cancelTask(timeoutTaskId);
            timeoutTaskId = -1;
        }
    }
}
