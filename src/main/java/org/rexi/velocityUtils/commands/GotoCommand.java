package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.Optional;

public class GotoCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public GotoCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Solo jugadores pueden usar este comando
        if (!(source instanceof Player player)) {
            String no_console = configManager.getMessage("no_console");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_console));
            return;
        }

        // Verificar permiso opcional
        if (!player.hasPermission("velocityutils.goto")) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        // Sintaxis
        if (args.length != 1) {
            String goto_usage = configManager.getMessage("goto_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_usage));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            String goto_player_not_found = configManager.getMessage("goto_player_not_found");
            goto_player_not_found = goto_player_not_found.replace("{player}", targetName);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_player_not_found));
            return;
        }

        Player target = targetOpt.get();

        // Obtener servidor del jugador objetivo
        Optional<String> serverNameOpt = target.getCurrentServer()
                .map(s -> s.getServerInfo().getName());

        if (serverNameOpt.isEmpty()) {
            String goto_server_not_found = configManager.getMessage("goto_server_not_found");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_server_not_found));
            return;
        }

        String targetServer = serverNameOpt.get();
        String currentServer = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("");

        if (targetServer.equalsIgnoreCase(currentServer)) {
            String goto_same_server = configManager.getMessage("goto_same_server");
            goto_same_server = goto_same_server.replace("{player}", target.getUsername());
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_same_server));
            return;
        }

        server.getServer(targetServer).ifPresentOrElse(dest -> {
            player.createConnectionRequest(dest).fireAndForget();
            String goto_connecting = configManager.getMessage("goto_connecting");
            goto_connecting = goto_connecting.replace("{player}", target.getUsername());
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_connecting));
        }, () -> {
            String goto_server_not_found = configManager.getMessage("goto_server_not_found");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(goto_server_not_found));
        });
    }
}
