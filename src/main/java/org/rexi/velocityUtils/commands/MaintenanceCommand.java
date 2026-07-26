package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final ProxyServer server;

    public MaintenanceCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Verificar si el usuario tiene permisos para ejecutar el comando
        if (!source.hasPermission("velocityutils.maintenance")) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        if (args.length == 0) {
            String maintenance_usage = configManager.getMessage("maintenance_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_usage));
            return;
        }

        // Comando: /maintenance on
        if (args[0].equalsIgnoreCase("on")) {
            configManager.setBoolean("maintenance.active", true);

            List<Player> players = server.getAllPlayers().stream().toList();

            for (Player player : players) {
                if (!player.hasPermission("velocityutils.maintenance.bypass")) {
                    String under_maintenance = configManager.getMessage("maintenance_not_on_list");
                    player.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(under_maintenance));
                }
            }

            String maintenance_activated = configManager.getMessage("maintenance_activated");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_activated));
        }
        // Comando: /maintenance off
        else if (args[0].equalsIgnoreCase("off")) {
            configManager.setBoolean("maintenance.active", false);

            String maintenance_deactivated = configManager.getMessage("maintenance_deactivated");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_deactivated));
        }
        // Comando no reconocido
        else {
            String maintenance_usage = configManager.getMessage("maintenance_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_usage));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> allowedPlayers = configManager.getStringList("maintenance.allowed");
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0) {
            return List.of("on", "off");
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> options = List.of("on", "off");

            for (String option : options) {
                if (option.startsWith(input)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        return List.of();
    }
}

