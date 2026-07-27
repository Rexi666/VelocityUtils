package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.utils.BanData;

import java.util.List;
import java.util.Map;

public class CheckBanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final VelocityUtils plugin;

    public CheckBanCommand(ConfigManager configManager, VelocityUtils plugin) {
        this.configManager = configManager;
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

        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("usage_checkban"));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = plugin.loadBan(targetName, null);

        if (checkban != null) {
            source.sendMessage(configManager.getMessage("checkban_banned",
                    "{player}", targetName,
                    "{banned_by}", checkban.getBannedBy(),
                    "{reason}", checkban.getReason()));
        } else {
            if (plugin.banCache.containsKey(targetName)) {
                for (Map.Entry<String, List<String>> entry : plugin.subIpBanCache.entrySet()) {
                    if (entry.getValue().contains(targetName)) {
                        source.sendMessage(configManager.getMessage("checkban_banned_ip",
                                "{ip_playername}", entry.getKey(),
                                "{player}", targetName,
                                "{banned_by}", plugin.banCache.get(entry.getKey()).getBannedBy(),
                                "{reason}", plugin.banCache.get(entry.getKey()).getReason()));
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
