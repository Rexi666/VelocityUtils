package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class ServerWhitelistCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public ServerWhitelistCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocityutils.serverwhitelist.command")) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(configManager.getMessage("serverwhitelist_usage"));
            return;
        }

        if (args[0].equalsIgnoreCase("add") && args.length == 2) {
            String serverName = args[1];

            if (server.getServer(serverName).isEmpty()) {
                source.sendMessage(configManager.getMessage("serverwhitelist_server_not_found",
                        "{server}", serverName)
                );
                return;
            }

            List<String> servers = configManager.getStringList("serverwhitelist.active_servers");

            if (servers.contains(serverName)) {
                source.sendMessage(configManager.getMessage("serverwhitelist_already_on_list",
                        "{server}", serverName)
                );
                return;
            }

            servers.add(serverName);
            configManager.setList("serverwhitelist.active_servers", servers);

            source.sendMessage(configManager.getMessage("serverwhitelist_server_added",
                    "{server}", serverName)
            );
        } else if (args[0].equalsIgnoreCase("remove") && args.length == 2) {
            String serverName = args[1];

            List<String> servers = configManager.getStringList("serverwhitelist.active_servers");

            if (!servers.contains(serverName)) {
                source.sendMessage(configManager.getMessage("serverwhitelist_server_not_on_list",
                        "{server}", serverName)
                );
                return;
            }

            servers.remove(serverName);
            configManager.setList("serverwhitelist.active_servers", servers);

            source.sendMessage(configManager.getMessage("serverwhitelist_server_removed",
                    "{server}", serverName)
            );
        } else if (args[0].equalsIgnoreCase("list") && args.length == 1) {
            List<String> servers = configManager.getStringList("serverwhitelist.active_servers");

            if (servers.isEmpty()) {
                source.sendMessage(configManager.getMessage("serverwhitelist_list_empty"));
                return;
            }

            source.sendMessage(configManager.getMessage("serverwhitelist_list_header"));

            for (String serverName : servers) {
                source.sendMessage(configManager.getMessage("serverwhitelist_list_format",
                        "{server}", serverName));
            }
        } else {
            source.sendMessage(configManager.getMessage("serverwhitelist_usage"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        List<String> whitelistedServers =
                configManager.getStringList("serverwhitelist.active_servers");

        if (args.length == 0) {
            return List.of("add", "remove", "list");
        }

        if (args.length == 1) {
            for (String option : List.of("add", "remove", "list")) {
                if (option.startsWith(args[0].toLowerCase())) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase();

            if (args[0].equalsIgnoreCase("add")) {
                server.getAllServers().forEach(registeredServer -> {
                    String name = registeredServer.getServerInfo().getName();

                    if (!whitelistedServers.contains(name)
                            && name.toLowerCase().startsWith(input)) {
                        suggestions.add(name);
                    }
                });
            }

            if (args[0].equalsIgnoreCase("remove")) {
                for (String name : whitelistedServers) {
                    if (name.toLowerCase().startsWith(input)) {
                        suggestions.add(name);
                    }
                }
            }

            return suggestions;
        }

        return List.of();
    }
}
