package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

public class VelocityUtilsCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public VelocityUtilsCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (invocation.arguments().length > 0 && invocation.arguments()[0].equalsIgnoreCase("reload")) {
            if (source.hasPermission("velocityutils.admin") || source instanceof ConsoleCommandSource) {
                configManager.loadConfig();
                plugin.registerMoveCommands();
                Component motd = configManager.getMotd();
                String configuration_reloaded = configManager.getMessage("configuration_reloaded");
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(configuration_reloaded));
            } else {
                String no_permission = configManager.getMessage("no_permission");
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            }
        } else {
            String velocityutils_usage = configManager.getMessage("velocityutils_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(velocityutils_usage));
        }
    }
}
