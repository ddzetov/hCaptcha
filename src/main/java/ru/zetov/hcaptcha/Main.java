package ru.zetov.hcaptcha;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.zetov.hcaptcha.utils.ConfigManager;
import ru.zetov.hcaptcha.data.DatabaseManager;
import ru.zetov.hcaptcha.listener.CaptchaListener;

public final class Main extends JavaPlugin {

    public DatabaseManager database;
    public ConfigManager configManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.load();

        database = new DatabaseManager(this);
        database.init();

        Bukkit.getPluginManager().registerEvents(new CaptchaListener(this), this);
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }
}
