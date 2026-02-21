package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.utils.BanData;

import java.util.List;
import java.util.Map;

public class CheckBanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public CheckBanCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vcheckban <player>
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vcheckban"))) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("no_permission")));
            return;
        }

        if (args.length < 1) {
            String usage = configManager.getMessage("usage_checkban");
            source.sendMessage(deserializeLegacy(usage));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = plugin.loadBan(targetName, null);

        if (checkban != null) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("checkban_banned")
                    .replace("{player}", targetName)
                    .replace("{banned_by}", checkban.getBannedBy())
                    .replace("{reason}", checkban.getReason())));
        } else {
            if (plugin.banCache.containsKey(targetName)) {
                for (Map.Entry<String, List<String>> entry : plugin.subIpBanCache.entrySet()) {
                    if (entry.getValue().contains(targetName)) {
                        source.sendMessage(deserializeLegacy(configManager.getMessage("checkban_banned_ip")
                                .replace("{ip_playername}", entry.getKey())
                                .replace("{player}", targetName)
                                .replace("{banned_by}", plugin.banCache.get(entry.getKey()).getBannedBy())
                                .replace("{reason}", plugin.banCache.get(entry.getKey()).getReason())));
                        return;
                    }
                }
                source.sendMessage(deserializeLegacy(configManager.getMessage("checkban_not_banned")
                        .replace("{player}", targetName)));
            } else {
                source.sendMessage(deserializeLegacy(configManager.getMessage("checkban_not_banned")
                        .replace("{player}", targetName)));
            }
        }
    }

    private Component deserializeLegacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }
}
