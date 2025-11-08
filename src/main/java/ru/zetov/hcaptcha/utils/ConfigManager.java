package ru.zetov.hcaptcha.utils;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import ru.zetov.hcaptcha.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigManager {

    private final Main plugin;

    public long captchaCooldown;
    public long timeoutMillis;
    public int maxAttempts;
    public long resendDelayMillis;
    public String messagePrefix;
    public String messageQuestion;
    public String messageCorrect;
    public String messageWrong;
    public String messageKick;
    public String messageNoCommand;
    public String messageNoMenu;
    public String messageTimeout;
    public List<SoundEntry> soundEntries = new ArrayList<>();

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        captchaCooldown = config.getLong("settings.cooldown-seconds", 10800) * 1000L;
        maxAttempts = config.getInt("settings.attempts", 3);
        resendDelayMillis = config.getLong("settings.resend-delay", 2) * 1000L;
        timeoutMillis = config.getLong("settings.timeout", 30) * 1000L;

        messagePrefix = color(config.getString("messages.prefix", "&6[Captcha]&r "));
        messageQuestion = color(config.getString("messages.question", "&eКакое животное издаёт этот звук?"));
        messageCorrect = color(config.getString("messages.correct", "&aВерно! Вы прошли капчу."));
        messageWrong = color(config.getString("messages.wrong", "&cНеверно! Попробуйте снова."));
        messageKick = color(config.getString("messages.kick", "&cВы не прошли капчу!"));
        messageNoCommand = color(config.getString("messages.no-command", "&cВы не можете использовать команды, пока не пройдёте капчу!"));
        messageNoMenu = color(config.getString("messages.no-menu", "&cВы не можете открывать меню, пока проходите капчу."));
        messageTimeout = color(config.getString("messages.timeout", "&cВремя на прохождение капчи истекло!"));

        soundEntries.clear();
        var section = config.getConfigurationSection("sounds");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String name = config.getString("sounds." + key + ".name", key).toLowerCase();
                    Sound sound = Sound.valueOf(Objects.requireNonNull(config.getString("sounds." + key + ".sound")).toUpperCase());
                    soundEntries.add(new SoundEntry(name, sound));
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при загрузке звука: " + key);
                }
            }
        }

        plugin.getLogger().info("Конфиг успешно загружен (" + soundEntries.size() + " звуков).");
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
