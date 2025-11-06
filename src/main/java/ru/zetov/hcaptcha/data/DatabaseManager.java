package ru.zetov.hcaptcha.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "captcha.db");
            if (!dbFile.exists()) {
                dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS captcha_data (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "last_passed BIGINT)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getLastPassed(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_passed FROM captcha_data WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("last_passed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public void setLastPassed(UUID uuid, long time) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO captcha_data (uuid, last_passed) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, time);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
