package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MoveCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final String commandName;
    private final Random random = new Random();

    public MoveCommand(ConfigManager configManager, ProxyServer server, String commandName) {
        this.configManager = configManager;
        this.server = server;
        this.commandName = commandName;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(legacy(configManager.getMessage("no_console")));
            return;
        }

        String permission = "velocityutils.movecommand." + commandName.toLowerCase();

        if (!player.hasPermission(permission)) {
            player.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        // Leer datos de la config
        List<String> servers = configManager.getStringList("movecommands." + commandName + ".server");
        String message = configManager.getString("movecommands." + commandName + ".message");

        if (servers.isEmpty()) {
            player.sendMessage(legacy(configManager.getMessage("movecommands_no_servers")));
            return;
        }

        connectToAvailableServer(player, servers, message);
    }

    private void connectToAvailableServer(Player player, List<String> servers, String message) {
        List<String> shuffledServers = new ArrayList<>(servers);
        Collections.shuffle(shuffledServers, random);

        // Comprobar si ya está conectado a alguno de los servidores
        if (player.getCurrentServer().isPresent()) {
            String currentServer = player.getCurrentServer().get().getServerInfo().getName();
            for (String serverName : shuffledServers) {
                if (currentServer.equalsIgnoreCase(serverName)) {
                    player.sendMessage(legacy(configManager.getMessage("movecommands_already_connected")));
                    return;
                }
            }
        }

        // Conectar al primer servidor disponible
        for (String serverName : shuffledServers) {
            var optionalServer = server.getServer(serverName);
            if (optionalServer.isPresent()) {
                optionalServer.get().ping().thenAccept(ping -> {
                    player.createConnectionRequest(optionalServer.get()).connect().thenAccept(result -> {
                        if (result.isSuccessful()) {
                            if (message != null && !message.isEmpty()) {
                                player.sendMessage(legacy(message));
                            }
                        } else {
                            player.sendMessage(legacy(configManager.getMessage("movecommands_server_not_found")));
                        }
                    });
                }).exceptionally(ex -> {
                    player.sendMessage(legacy(configManager.getMessage("movecommands_server_not_found")));
                    return null;
                });
                return; // Intentamos solo el primero disponible
            }
        }

        player.sendMessage(legacy(configManager.getMessage("movecommands_server_not_found")));
    }



    private net.kyori.adventure.text.Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
