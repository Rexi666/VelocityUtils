package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.managers.BanManager;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.BanData;

import java.util.List;

public class UnbanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final BanManager banManager;

    public UnbanCommand(ConfigManager configManager, ProxyServer server, BanManager banManager) {
        this.configManager = configManager;
        this.server = server;
        this.banManager = banManager;
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

        BanData checkban = banManager.loadBan(targetName, null);

        if (checkban == null) {
            source.sendMessage(configManager.getMessage("not_banned",
                    "{player}", targetName));
            return;
        }

        banManager.removeBan(checkban);

        List<String> subIps = banManager.getSubIpBanCache().getOrDefault(targetName, null);

        if (subIps != null) {
            for (String subIp : subIps) {
                banManager.getBanCache().remove(subIp);
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
