package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.listeners.MotdListener;
import org.rexi.velocityUtils.managers.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final ProxyServer server;
    private final MotdListener motdListener;
    private final VelocityUtils plugin;

    public MaintenanceCommand(ConfigManager configManager, ProxyServer server, MotdListener motdListener, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.motdListener = motdListener;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Verificar si el usuario tiene permisos para ejecutar el comando
        if (!source.hasPermission("velocityutils.maintenance")) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (source instanceof Player p && plugin.isPlayerInDisabledServer(p)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(configManager.getMessage("maintenance_usage"));
            return;
        }

        // Comando: /maintenance on
        if (args[0].equalsIgnoreCase("on")) {
            configManager.setBoolean("maintenance.active", true);
            motdListener.changeMaintenanceActive(true);

            List<Player> players = server.getAllPlayers().stream().toList();

            for (Player player : players) {
                if (!player.hasPermission("velocityutils.maintenance.bypass")) {
                    player.disconnect(configManager.getMessage("maintenance_not_on_list"));
                }
            }

            source.sendMessage(configManager.getMessage("maintenance_activated"));
        }
        // Comando: /maintenance off
        else if (args[0].equalsIgnoreCase("off")) {
            configManager.setBoolean("maintenance.active", false);
            motdListener.changeMaintenanceActive(false);

            source.sendMessage(configManager.getMessage("maintenance_deactivated"));
        }
        // Comando no reconocido
        else {
            source.sendMessage(configManager.getMessage("maintenance_usage"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
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

