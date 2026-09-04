package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PingCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public PingCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocityutils.ping")) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 1) {
            if (source instanceof Player player) {
                player.sendMessage(configManager.getMessage("ping_self", "{ping}", String.valueOf(player.getPing())));
            } else {
                source.sendMessage(configManager.getMessage("ping_usage_others"));
            }
        } else {
            if (!source.hasPermission("velocityutils.ping.others")) {
                source.sendMessage(configManager.getMessage("no_permission"));
                return;
            }

            String targetName = args[0];
            Player targetPlayer = server.getPlayer(targetName).orElse(null);
            if (targetPlayer == null) {
                source.sendMessage(configManager.getMessage("ping_offline", "{player}", targetName));
                return;
            }
            source.sendMessage(configManager.getMessage("ping_others", "{player}", targetPlayer.getUsername(), "{ping}", String.valueOf(targetPlayer.getPing())));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            if (!source.hasPermission("velocityutils.ping.others")) return List.of(); // No sugerimos nada si no tiene permiso para ver otros jugadores
            // No se ha escrito nada aún, sugerimos todos los jugadores
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            if (!source.hasPermission("velocityutils.ping.others")) return List.of(); // No sugerimos nada si no tiene permiso para ver otros jugadores
            String input = args[0].toLowerCase(Locale.ROOT);
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
