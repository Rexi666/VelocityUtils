package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.listeners.BrandListener;

import java.util.List;

public class VelocityUtilsCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final VelocityUtils plugin;
    private final BrandListener brandListener;

    public VelocityUtilsCommand(ConfigManager configManager, VelocityUtils plugin, BrandListener brandListener) {
        this.configManager = configManager;
        this.plugin = plugin;
        this.brandListener = brandListener;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
            if (source.hasPermission("velocityutils.admin") || source instanceof ConsoleCommandSource) {
                configManager.loadConfig();
                configManager.loadMessages();
                plugin.registerMoveCommands();
                plugin.registerCommands();
                plugin.registerMessagesCommands();
                brandListener.sendBrandToAll();
                plugin.startRegularAlerts();
                plugin.refreshTebex();
                source.sendMessage(configManager.getMessage("configuration_reloaded"));
            } else {
                source.sendMessage(configManager.getMessage("no_permission"));
            }
        } else {
            source.sendMessage(configManager.getMessage("velocityutils_usage"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            // No se ha escrito nada aún, sugerimos reload
            return List.of("reload");
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }
        return List.of();
    }

}
