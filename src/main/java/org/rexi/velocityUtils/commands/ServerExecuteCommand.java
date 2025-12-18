package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class ServerExecuteCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public ServerExecuteCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player player && !player.hasPermission("velocityutils.serverexecute")) {
            player.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(legacy(configManager.getMessage("serverexecute_usage")));
            return;
        }

        String serverName = args[0];
        String command = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Optional<RegisteredServer> optServer = server.getServer(serverName);

        if (optServer.isEmpty()) {
            source.sendMessage(legacy(configManager.getMessage("serverexecute_server_not_found").replace("{server}", serverName)));
            return;
        }

        RegisteredServer server = optServer.get();

        if (server.getPlayersConnected().isEmpty()) {
            // No hay jugadores -> almacenamos el comando
            plugin.pendingCommands.computeIfAbsent(serverName, k -> new ArrayList<>()).add(command);
        } else {
            plugin.sendCommandToServer(server, command);
        }

        source.sendMessage(legacy(configManager.getMessage("serverexecute_sent")
                .replace("{server}", serverName)
                .replace("{command}", command)));
    }

    private static final LegacyComponentSerializer LEGACY_HEX_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors() // Habilita el soporte de hex
            .useUnusualXRepeatedCharacterHexFormat() // Soporta &x&r&r&g&g&b&b
            .build();
    private Component legacy(String s) {
        return LEGACY_HEX_SERIALIZER.deserialize(s);
    }
}
