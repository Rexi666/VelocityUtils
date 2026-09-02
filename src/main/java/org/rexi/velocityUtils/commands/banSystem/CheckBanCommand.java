package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.BanManager;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.BanData;

import java.util.List;
import java.util.Map;

public class CheckBanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final BanManager banManager;
    private final VelocityUtils plugin;

    public CheckBanCommand(ConfigManager configManager, BanManager banManager, VelocityUtils plugin) {
        this.configManager = configManager;
        this.banManager = banManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vcheckban <player>
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vcheckban"))) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (source instanceof Player p && plugin.isPlayerInDisabledServer(p)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("usage_checkban"));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = banManager.loadBan(targetName, null);

        if (checkban != null) {
            source.sendMessage(configManager.getMessage("checkban_banned",
                    "{player}", targetName,
                    "{banned_by}", checkban.getBannedBy(),
                    "{reason}", checkban.getReason()));
        } else {
            if (banManager.getBanCache().containsKey(targetName)) {
                for (Map.Entry<String, List<String>> entry : banManager.getSubIpBanCache().entrySet()) {
                    if (entry.getValue().contains(targetName)) {
                        source.sendMessage(configManager.getMessage("checkban_banned_ip",
                                "{ip_playername}", entry.getKey(),
                                "{player}", targetName,
                                "{banned_by}", banManager.getBanCache().get(entry.getKey()).getBannedBy(),
                                "{reason}", banManager.getBanCache().get(entry.getKey()).getReason()));
                        return;
                    }
                }
                source.sendMessage(configManager.getMessage("checkban_not_banned",
                        "{player}", targetName));
            } else {
                source.sendMessage(configManager.getMessage("checkban_not_banned",
                        "{player}", targetName));
            }
        }
    }
}
