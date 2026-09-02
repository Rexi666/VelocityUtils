package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class VIgnoreCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public VIgnoreCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
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

        if (plugin.isPlayerInDisabledServer(player)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (!player.hasPermission("velocityutils.vignore")) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(configManager.getMessage("ignore_usage"));
            return;
        }

        String targetName = args[0];
        String playerName = player.getUsername();

        if (targetName.equalsIgnoreCase(playerName)) {
            player.sendMessage(configManager.getMessage("ignore_self"));
            return;
        }

        Player target = server.getPlayer(targetName).orElse(null);

        if (target == null) {
            player.sendMessage(configManager.getMessage("ignore_offline", "{player}", targetName));
            return;
        }

        if (target.hasPermission("velocityutils.vignore.bypass")) {
            player.sendMessage(configManager.getMessage("ignore_bypass", "{player}", targetName));
            return;
        }

        boolean isIgnoring = plugin.checkIgnoredPlayers(player.getUniqueId(), target.getUniqueId());
        if (isIgnoring) {
            plugin.setIgnoredPlayers(player.getUniqueId(), target.getUniqueId(), false);
            player.sendMessage(configManager.getMessage("ignore_removed", "{player}", targetName));
        } else {
            plugin.setIgnoredPlayers(player.getUniqueId(), target.getUniqueId(), true);
            player.sendMessage(configManager.getMessage("ignore_added", "{player}", targetName));
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
