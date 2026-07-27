package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
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
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length != 1) {
            source.sendMessage(configManager.getMessage("usage_unban"));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = plugin.loadBan(targetName, null);

        if (checkban == null) {
            source.sendMessage(configManager.getMessage("not_banned",
                    "{player}", targetName));
            return;
        }

        plugin.removeBan(checkban);

        List<String> subIps = plugin.subIpBanCache.getOrDefault(targetName, null);

        if (subIps != null) {
            for (String subIp : subIps) {
                plugin.banCache.remove(subIp);
            }
        }

        source.sendMessage(configManager.getMessage("unban_success",
                "{player}", targetName));

        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        Component finalMessage = configManager.getMessage("unban_notify",
                "{player}", targetName,
                "{unbanned_by}", fromName);
        server.getConsoleCommandSource().sendMessage(finalMessage);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("velocityutils.bansystem.notify")) {
                player.sendMessage(finalMessage);
            }
        }
    }
}
