package ru.zetov.hcaptcha.model;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;

public class CaptchaSession {

    public CaptchaSound currentQuestion;
    public AtomicInteger attempts;
    public int repeatTaskId = -1;
    public int timeoutTaskId = -1;
    public boolean active = true;

    public CaptchaSession(CaptchaSound question, int attempts) {
        this.currentQuestion = question;
        this.attempts = new AtomicInteger(attempts);
    }

    public void decrementAttempts() {
        attempts.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    public boolean hasAttempts() {
        return attempts.get() > 0;
    }

    public boolean isActive() {
        return active && hasAttempts();
    }

    public void stop() {
        this.active = false;
    }

    public boolean hasRepeatTask() {
        return repeatTaskId != -1;
    }

    public boolean hasTimeoutTask() {
        return timeoutTaskId != -1;
    }

    public void cancelAllTasks() {
        if (hasRepeatTask()) {
            try {
                Bukkit.getScheduler().cancelTask(repeatTaskId);
                repeatTaskId = -1;
            } catch (Exception ignored) {}
        }

        if (hasTimeoutTask()) {
            try {
                Bukkit.getScheduler().cancelTask(timeoutTaskId);
                timeoutTaskId = -1;
            } catch (Exception ignored) {}
        }
    }

    public void cleanup() {
        stop();
        cancelAllTasks();
        currentQuestion = null;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }
}
