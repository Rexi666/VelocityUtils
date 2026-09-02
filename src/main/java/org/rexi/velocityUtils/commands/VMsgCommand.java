package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.*;

public class VMsgCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public VMsgCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
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

        if (!player.hasPermission("velocityutils.private_message")) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (plugin.isPlayerInDisabledServer(player)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(configManager.getMessage("msg_usage"));
            return;
        }

        String targetName = args[0];
        String playerName = player.getUsername();

        if (targetName.equalsIgnoreCase(playerName)) {
            player.sendMessage(configManager.getMessage("msg_self"));
            return;
        }

        Player target = server.getPlayer(targetName).orElse(null);

        if (target == null || plugin.isPlayerInDisabledServer(target)) {
            player.sendMessage(configManager.getMessage("msg_offline", "{player}", targetName));
            return;
        }

        boolean ignoring = plugin.checkIgnoredPlayers(target.getUniqueId(), player.getUniqueId());
        if (ignoring) {
            player.sendMessage(configManager.getMessage("msg_ignoring", "{player}", targetName));
            return;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        String playerServer = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessageString("server_unknown"));
        String targetServer = target.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessageString("server_unknown"));

        Component finalMessage = configManager.getMessage("msg", "{player}", playerName, "{target}", targetName, "{message}", message, "{player_server}", playerServer, "{target_server}", targetServer);

        plugin.messageReplies.put(target.getUniqueId(), player.getUniqueId());
        plugin.messageReplies.put(player.getUniqueId(), target.getUniqueId());

        player.sendMessage(finalMessage);
        target.sendMessage(finalMessage);

        List<UUID> spies1 = plugin.spyPlayers.get(target.getUniqueId());
        if (spies1 != null) {
            for (UUID uuid : spies1) {
                Player spy = server.getPlayer(uuid).orElse(null);
                if (spy != null) {
                    spy.sendMessage(finalMessage);
                }
            }
        }

        List<UUID> spies2 = plugin.spyPlayers.get(player.getUniqueId());
        if (spies2 != null) {
            for (UUID uuid : spies2) {
                Player spy = server.getPlayer(uuid).orElse(null);
                if (spy != null) {
                    spy.sendMessage(finalMessage);
                }
            }
        }

        List<UUID> globalSpies = plugin.spyGlobalPlayers;
        for (UUID uuid : globalSpies) {
            Player spy = server.getPlayer(uuid).orElse(null);
            if (spy != null) {
                spy.sendMessage(finalMessage);
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
