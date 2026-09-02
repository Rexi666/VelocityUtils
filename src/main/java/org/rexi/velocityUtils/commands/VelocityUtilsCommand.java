package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.listeners.MotdListener;
import org.rexi.velocityUtils.managers.AlertManager;
import org.rexi.velocityUtils.managers.CommandManager;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.listeners.BrandListener;
import org.rexi.velocityUtils.utils.tebex.TebexService;

import java.util.List;

public class VelocityUtilsCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final BrandListener brandListener;
    private final CommandManager commandManager;
    private final AlertManager alertManager;
    private final TebexService tebexService;
    private final MotdListener motdListener;
    private final VelocityUtils plugin;

    private String version;
    private String author;

    public VelocityUtilsCommand(ConfigManager configManager, BrandListener brandListener, CommandManager commandManager, AlertManager alertManager, TebexService tebexService, MotdListener motdListener, String version, String author, VelocityUtils plugin) {
        this.configManager = configManager;
        this.brandListener = brandListener;
        this.commandManager = commandManager;
        this.alertManager = alertManager;
        this.tebexService = tebexService;
        this.motdListener = motdListener;
        this.plugin = plugin;

        this.version = version;
        this.author = author;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocityutils.admin")) {
            source.sendMessage(configManager.getMessage("no_permission"));
        }

        if (source instanceof Player p && plugin.isPlayerInDisabledServer(p)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(configManager.getMessage("velocityutils_usage"));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            configManager.loadConfig();
            configManager.loadMessages();
            commandManager.registerMoveCommands();
            commandManager.registerCommands();
            commandManager.registerMessagesCommands();
            brandListener.sendBrandToAll();
            alertManager.startRegularAlerts();
            motdListener.reload();

            if (configManager.getBoolean("tebex_link.enabled")
                    && !configManager.getString("tebex_link.secret").equalsIgnoreCase("YOUR_TEBEX_SECRET_KEY")) {
                tebexService.refresh();
            }

            source.sendMessage(configManager.getMessage("configuration_reloaded"));
            return;
        } else if (args[0].equalsIgnoreCase("version")) {
            source.sendMessage(configManager.getMessage("velocityutils_version",
                    "{version}", version,
                    "{author}", author));
            return;
        } else {
            source.sendMessage(configManager.getMessage("velocityutils_usage"));
            return;
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
