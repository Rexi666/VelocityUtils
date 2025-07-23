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
            configManager.setMaintenanceMode(true);

            List<Player> players = server.getAllPlayers().stream().toList();
            List<String> allowedPlayers = configManager.getAllowedPlayers();

            for (Player player : players) {
                if (!allowedPlayers.contains(player.getUsername())) {
                    String under_maintenance = configManager.getMessage("maintenance_not_on_list");
                    player.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(under_maintenance));
                }
            }

            String maintenance_activated = configManager.getMessage("maintenance_activated");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_activated));
        }
        // Comando: /maintenance off
        else if (args[0].equalsIgnoreCase("off")) {
            configManager.setMaintenanceMode(false);

            String maintenance_deactivated = configManager.getMessage("maintenance_deactivated");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_deactivated));
        }
        // Comando: /maintenance add <nick>
        else if (args[0].equalsIgnoreCase("add") && args.length == 2) {
            String nick = args[1];
            List<String> allowedPlayers = configManager.getAllowedPlayers();
            if (allowedPlayers.contains(nick)) {
                String maintenance_already_on_list = configManager.getMessage("maintenance_already_on_list");
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_already_on_list));
                return;
            }
            allowedPlayers.add(nick);
            configManager.setAllowedPlayers(allowedPlayers);
            String maintenance_player_added = configManager.getMessage("maintenance_player_added");
            maintenance_player_added = maintenance_player_added.replace("{player}", nick);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_player_added));
        }
        // Comando: /maintenance remove <nick>
        else if (args[0].equalsIgnoreCase("remove") && args.length == 2) {
            String nick = args[1];
            List<String> allowedPlayers = configManager.getAllowedPlayers();
            if (!allowedPlayers.contains(nick)) {
                String maintenance_player_not_on_list = configManager.getMessage("maintenance_player_not_on_list");
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_player_not_on_list));
                return;
            }
            allowedPlayers.remove(nick);
            configManager.setAllowedPlayers(allowedPlayers);
            String maintenance_player_removed = configManager.getMessage("maintenance_player_removed");
            maintenance_player_removed = maintenance_player_removed.replace("{player}", nick);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(maintenance_player_removed));
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
        List<String> allowedPlayers = configManager.getAllowedPlayers();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0) {
            return List.of("on", "off", "add", "remove");
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> options = List.of("on", "off", "add", "remove");

            for (String option : options) {
                if (option.startsWith(input)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (subCommand.equals("add")) {
                // Sugerir jugadores que NO están en la lista de permitidos (podrías cambiar esto según convenga)
                for (Player player : server.getAllPlayers()) {
                    String name = player.getUsername();
                    if (!allowedPlayers.contains(name) &&
                            (name.toLowerCase().startsWith(input))) {
                        suggestions.add(name);
                    }
                }
            } else if (subCommand.equals("remove")) {
                // Sugerir jugadores que SÍ están en la lista de permitidos
                for (String name : allowedPlayers) {
                    if (name.toLowerCase().startsWith(input)) {
                        suggestions.add(name);
                    }
                }
                return suggestions;
            }
        }

        return List.of();
    }
}

