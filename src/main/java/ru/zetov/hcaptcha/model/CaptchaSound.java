package ru.zetov.hcaptcha.model;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.zetov.hcaptcha.Main;
import ru.zetov.hcaptcha.utils.ConfigManager;

import java.util.*;

public class CaptchaSound {

    private static final Random RANDOM = new Random();

    private final Main plugin;
    private final Sound sound;
    private final String answer;
    private List<String> cachedOptions;

    public CaptchaSound(Main plugin, Sound sound, String answer) {
        this.plugin = plugin;
        this.sound = sound;
        this.answer = answer;
        this.cachedOptions = null;
    }

    public void playSound(Player player) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    public boolean isCorrect(String input) {
        return input != null && input.equalsIgnoreCase(answer);
    }

    public List<String> getOptions() {
        if (cachedOptions != null) {
            return new ArrayList<>(cachedOptions);
        }

        List<ConfigManager.SoundEntry> allEntries = plugin.configManager.soundEntries;
        List<String> allNames = new ArrayList<>();

        for (ConfigManager.SoundEntry entry : allEntries) {
            allNames.add(entry.getName());
        }

        Collections.shuffle(allNames, RANDOM);

        List<String> options = new ArrayList<>(allNames.subList(0, Math.min(3, allNames.size())));

        if (!options.contains(answer)) {
            options.set(RANDOM.nextInt(options.size()), answer);
        }

        cachedOptions = Collections.unmodifiableList(options);
        return new ArrayList<>(cachedOptions);
    }

    public static CaptchaSound random(Main plugin) {
        List<ConfigManager.SoundEntry> entries = plugin.configManager.soundEntries;
        if (entries.isEmpty()) {
            throw new IllegalStateException("No sound entries configured");
        }

        ConfigManager.SoundEntry entry = entries.get(RANDOM.nextInt(entries.size()));
        return new CaptchaSound(plugin, entry.getSound(), entry.getName());
    }
}
