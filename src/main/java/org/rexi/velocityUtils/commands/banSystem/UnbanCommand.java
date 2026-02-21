package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.utils.BanData;

import java.util.List;

public class UnbanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public UnbanCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vunban <player>
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vunban"))) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("no_permission")));
            return;
        }

        if (args.length != 1) {
            String usage = configManager.getMessage("usage_unban");
            source.sendMessage(deserializeLegacy(usage));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = plugin.loadBan(targetName, null);

        if (checkban == null) {
            String not_banned = configManager.getMessage("not_banned");
            not_banned = not_banned.replace("{player}", targetName);
            source.sendMessage(deserializeLegacy(not_banned));
            return;
        }

        plugin.removeBan(checkban);

        List<String> subIps = plugin.subIpBanCache.getOrDefault(targetName, null);

        if (subIps != null) {
            for (String subIp : subIps) {
                plugin.banCache.remove(subIp);
            }
        }

        source.sendMessage(deserializeLegacy(configManager.getMessage("unban_success")
                .replace("{player}", targetName)));

        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        String message = configManager.getMessage("unban_notify");
        message = message.replace("{player}", targetName)
                .replace("{unbanned_by}", fromName);
        Component finalMessage = deserializeLegacy(message);
        server.getConsoleCommandSource().sendMessage(finalMessage);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("velocityutils.bansystem.notify")) {
                player.sendMessage(finalMessage);
            }
        }
    }

    private Component deserializeLegacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }
}
