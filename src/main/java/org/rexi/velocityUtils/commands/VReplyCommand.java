package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.List;
import java.util.UUID;

public class VReplyCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public VReplyCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
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

        if (args.length < 1) {
            player.sendMessage(configManager.getMessage("reply_usage"));
            return;
        }

        UUID targetUUID = plugin.messageReplies.get(player.getUniqueId());

        if (targetUUID == null) {
            player.sendMessage(configManager.getMessage("reply_offline"));
            return;
        }

        Player target = server.getPlayer(targetUUID).orElse(null);

        if (target == null || plugin.isPlayerInDisabledServer(target)) {
            player.sendMessage(configManager.getMessage("reply_offline"));
            return;
        }

        String targetName = target.getUsername();
        String playerName = player.getUsername();

        if (player.getUniqueId().equals(targetUUID)) {
            player.sendMessage(configManager.getMessage("msg_self"));
            return;
        }

        String message = String.join(" ", args);

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
}
