package ru.zetov.hcaptcha.model;

import java.util.concurrent.atomic.AtomicInteger;

public class CaptchaSession {

    private volatile CaptchaSound currentQuestion;
    private final AtomicInteger attempts;
    private volatile int repeatTaskId = -1;
    private volatile int timeoutTaskId = -1;
    private volatile boolean active = true;

    public CaptchaSession(CaptchaSound question, int attempts) {
        this.currentQuestion = question;
        this.attempts = new AtomicInteger(attempts);
    }

    public CaptchaSound getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(CaptchaSound question) {
        if (!active) {
            throw new IllegalStateException("Cannot change question for inactive session");
        }
        this.currentQuestion = question;
    }

    public int getAttempts() {
        return attempts.get();
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

    public int getRepeatTaskId() {
        return repeatTaskId;
    }

    public void setRepeatTaskId(int repeatTaskId) {
        this.repeatTaskId = repeatTaskId;
    }

    public boolean hasRepeatTask() {
        return repeatTaskId != -1;
    }

    public int getTimeoutTaskId() {
        return timeoutTaskId;
    }

    public void setTimeoutTaskId(int timeoutTaskId) {
        this.timeoutTaskId = timeoutTaskId;
    }

    public boolean hasTimeoutTask() {
        return timeoutTaskId != -1;
    }

    public void cancelAllTasks() {
        if (hasRepeatTask()) {
            try {
                org.bukkit.Bukkit.getScheduler().cancelTask(repeatTaskId);
                repeatTaskId = -1;
            } catch (Exception ignored) {
            }
        }

        if (hasTimeoutTask()) {
            try {
                org.bukkit.Bukkit.getScheduler().cancelTask(timeoutTaskId);
                timeoutTaskId = -1;
            } catch (Exception ignored) {

            }
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