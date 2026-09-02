package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.*;

public class VSpyCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public VSpyCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(configManager.getMessage("no_console"));
            return;
        }

        if (!player.hasPermission("velocityutils.vspy")) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (plugin.isPlayerInDisabledServer(player)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        String playerName = player.getUsername();

        if (args.length >= 1) {
            String targetName = args[0];

            if (targetName.equalsIgnoreCase(playerName)) {
                player.sendMessage(configManager.getMessage("spy_self"));
                return;
            }

            Player target = server.getPlayer(targetName).orElse(null);

            if (target == null) {
                player.sendMessage(configManager.getMessage("spy_offline", "{player}", targetName));
                return;
            }

            UUID playerUUID = player.getUniqueId();
            UUID targetUUID = target.getUniqueId();

            List<UUID> spies = plugin.spyPlayers.get(targetUUID);

            if (spies != null && spies.contains(playerUUID)) {
                spies.remove(playerUUID);
                plugin.spyPlayers.remove(targetUUID);
                if (!spies.isEmpty()) {
                    plugin.spyPlayers.put(targetUUID, spies);
                }
                player.sendMessage(configManager.getMessage("spy_player_disabled", "{player}", targetName));
            } else {
                plugin.spyPlayers.computeIfAbsent(targetUUID, k -> new ArrayList<>()).add(playerUUID);
                player.sendMessage(configManager.getMessage("spy_player_enabled", "{player}", targetName));
            }

        } else {
            UUID playerUUID = player.getUniqueId();

            if (plugin.spyGlobalPlayers.contains(playerUUID)) {
                plugin.spyGlobalPlayers.remove(playerUUID);
                player.sendMessage(configManager.getMessage("spy_disabled"));
            } else {
                plugin.spyGlobalPlayers.add(playerUUID);
                player.sendMessage(configManager.getMessage("spy_enabled"));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            List<String> suggestions = new ArrayList<>();

            for (Player player : server.getAllPlayers()) {
                // No sugerirse a sí mismo
                if (source instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                suggestions.add(player.getUsername());
            }

            Collections.sort(suggestions);
            return suggestions;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();

            for (Player player : server.getAllPlayers()) {
                // No sugerirse a sí mismo
                if (source instanceof Player p && p.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                if (player.getUsername().toLowerCase(Locale.ROOT).startsWith(input)) {
                    suggestions.add(player.getUsername());
                }
            }

            Collections.sort(suggestions);
            return suggestions;
        }

        return Collections.emptyList();
    }
}
