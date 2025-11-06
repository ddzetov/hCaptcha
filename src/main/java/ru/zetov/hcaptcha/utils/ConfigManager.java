package ru.zetov.hcaptcha.utils;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import ru.zetov.hcaptcha.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigManager {

    private final Main plugin;

    private long captchaCooldown;
    private long timeoutMillis;
    private int maxAttempts;
    private long resendDelayMillis;
    private String messagePrefix;
    private String messageQuestion;
    private String messageCorrect;
    private String messageWrong;
    private String messageKick;
    private String messageNoCommand;
    private String messageNoMenu;
    private String messageTimeout;
    private List<SoundEntry> soundEntries;


    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.captchaCooldown = config.getLong("settings.cooldown-seconds", 10800) * 1000L;
        this.maxAttempts = config.getInt("settings.attempts", 3);
        this.resendDelayMillis = config.getLong("settings.resend-delay", 2) * 1000L;

        this.messagePrefix = color(config.getString("messages.prefix", "&6[Captcha]&r "));
        this.messageQuestion = color(config.getString("messages.question", "&eКакое животное издаёт этот звук?"));
        this.messageCorrect = color(config.getString("messages.correct", "&aВерно! Вы прошли капчу."));
        this.messageWrong = color(config.getString("messages.wrong", "&cНеверно! Попробуйте снова."));
        this.messageKick = color(config.getString("messages.kick", "&cВы не прошли капчу!"));
        this.messageNoCommand = color(config.getString("messages.no-command", "&cВы не можете использовать команды, пока не пройдёте капчу!"));
        this.messageNoMenu = color(config.getString("messages.no-menu", "&cВы не можете открывать меню пока проходите каптчу"));
        this.messageTimeout = color(config.getString("messages.timeout", "&cВремя на прохождение капчи истекло!"));
        this.timeoutMillis = config.getLong("settings.timeout", 30) * 1000L;

        this.soundEntries = new ArrayList<>();

        if (config.contains("sounds")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("sounds")).getKeys(false)) {
                try {
                    String name = config.getString("sounds." + key + ".name");
                    String soundString = config.getString("sounds." + key + ".sound");
                    Sound sound = Sound.valueOf(Objects.requireNonNull(soundString).toUpperCase());
                    this.soundEntries.add(new SoundEntry(Objects.requireNonNull(name).toLowerCase(), sound));
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при загрузке звука: " + key);
                }
            }
        }

        plugin.getLogger().info("Конфиг загружен (" + soundEntries.size() + " звуков).");
    }

    public long getCaptchaCooldown() {
        return captchaCooldown;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getResendDelayMillis() {
        return resendDelayMillis;
    }

    public String getMessageQuestion() {
        return messagePrefix + messageQuestion;
    }

    public String getMessageCorrect() {
        return messagePrefix + messageCorrect;
    }

    public String getMessageWrong() {
        return messagePrefix + messageWrong;
    }

    public String getMessageKick() {
        return messageKick;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getMessageTimeout() {
        return messageTimeout;
    }


    public String getMessageNoCommand() {
        return messagePrefix + messageNoCommand;
    }

    public String getMessageNoMenu() {
        return messageNoMenu;
    }

    public List<SoundEntry> getSoundEntries() {
        return soundEntries;
    }

    private String color(String msg) {
        return msg == null ? "" : HexSupport.format(msg);
    }

    public static class SoundEntry {
        private final String name;
        private final Sound sound;

        public SoundEntry(String name, Sound sound) {
            this.name = name;
            this.sound = sound;
        }

        public String getName() {
            return name;
        }

        public Sound getSound() {
            return sound;
        }
    }
}