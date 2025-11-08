package ru.zetov.hcaptcha.listener;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.zetov.hcaptcha.Main;

import java.sql.Statement;

public class CaptchaCommand implements CommandExecutor {

    private final Main plugin;

    public CaptchaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eИспользование: /hcaptcha clearbd");
            return true;
        }

        if (args[0].equalsIgnoreCase("clearbd")) {
            if (!sender.hasPermission("hcaptcha.admin")) {
                sender.sendMessage("§cYou dont have permission to use this command!");
                return true;
            }

            try (Statement stmt = plugin.database.getConnection().createStatement()) {
                stmt.executeUpdate("DELETE FROM captcha_data");
                sender.sendMessage("§aБаза данных капчи успешно очищена!");
            } catch (Exception e) {
                sender.sendMessage("§cОшибка при очистке базы данных, проверь консоль.");
                e.printStackTrace();
            }
            return true;
        }

        sender.sendMessage("§eНеизвестная подкоманда. Используйте: /hcaptcha clearbd");
        return true;
    }
}
