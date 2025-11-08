package ru.zetov.hcaptcha.listener;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import ru.zetov.hcaptcha.Main;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptchaCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private static final List<String> SUB_COMMANDS = List.of("clearbd", "reload");

    public CaptchaCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("hcaptcha.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "clearbd" -> clearDatabase(sender);
            case "reload" -> reloadConfig(sender);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void clearDatabase(CommandSender sender) {
        try (Statement stmt = plugin.database.getConnection().createStatement()) {
            stmt.executeUpdate("DELETE FROM captcha_data");
            sender.sendMessage("§aБаза данных капчи успешно очищена!");
        } catch (Exception e) {
            sender.sendMessage("§cОшибка при очистке базы данных. Проверь консоль.");
            plugin.getLogger().severe("Ошибка при очистке базы данных:");
            e.printStackTrace();
        }
    }

    private void reloadConfig(CommandSender sender) {
        plugin.reloadConfig();
        plugin.configManager.load();
        sender.sendMessage("§aКонфиг плагина успешно перезагружен!");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§eИспользование: /hcaptcha <clearbd|reload>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("hcaptcha.admin")) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, completions);
            return completions;
        }
        return Collections.emptyList();
    }
}
