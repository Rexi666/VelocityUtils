package org.rexi.velocityUtils.managers;

import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.commands.banSystem.*;
import org.rexi.velocityUtils.listeners.BrandListener;
import org.rexi.velocityUtils.listeners.MotdListener;
import org.rexi.velocityUtils.utils.DateUtils;
import org.rexi.velocityUtils.utils.DiscordWebhook;
import org.rexi.velocityUtils.utils.tebex.TebexService;
import org.spongepowered.configurate.ConfigurationNode;

public class CommandManager {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final VelocityUtils plugin;
    private final DatabaseManager databaseManager;
    private final BanManager banManager;
    private final LuckPerms luckPerms;
    private final DiscordWebhook webhook;
    private final BrandListener brandListener;
    private final AlertManager alertManager;
    private final PluginMessageManager pluginMessageManager;
    private final TebexService tebexService;
    private final MotdListener motdListener;

    private String version;
    private String author;

    public CommandManager(
            ProxyServer server,
            ConfigManager configManager,
            VelocityUtils plugin,
            DatabaseManager databaseManager,
            BanManager banManager,
            LuckPerms luckPerms,
            DiscordWebhook webhook,
            BrandListener brandListener,
            AlertManager alertManager,
            PluginMessageManager pluginMessageManager,
            TebexService tebexService,
            MotdListener motdListener,

            String version, String author
    ) {
        this.server = server;
        this.configManager = configManager;
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.banManager = banManager;
        this.luckPerms = luckPerms;
        this.webhook = webhook;
        this.brandListener = brandListener;
        this.alertManager = alertManager;
        this.pluginMessageManager = pluginMessageManager;
        this.tebexService = tebexService;
        this.motdListener = motdListener;

        this.version = version;
        this.author = author;
    }

    public void registerCommands() {
        var commandManager = server.getCommandManager();

        commandManager.register(
                commandManager.metaBuilder("velocityutils").build(),
                new VelocityUtilsCommand(configManager, brandListener, this, alertManager, tebexService, motdListener, version, author, plugin));

        commandManager.register(
                commandManager.metaBuilder("vu").build(),
                new VelocityUtilsCommand(configManager, brandListener, this, alertManager, tebexService, motdListener, version, author, plugin));

        if (enabled("alert")) {
            commandManager.register("alert",
                    new AlertCommand(configManager,server, plugin, pluginMessageManager));
        }

        if (enabled("maintenance")) {
            commandManager.register(
                    commandManager.metaBuilder("maintenance").build(),
                    new MaintenanceCommand(configManager, server, motdListener, plugin));
        }

        if (enabled("report")) {
            commandManager.register(
                    commandManager.metaBuilder("report").build(),
                    new ReportCommand(configManager, server, webhook, plugin)
            );
        }

        if (enabled("goto")) {
            commandManager.register(
                    commandManager.metaBuilder("goto").build(),
                    new GotoCommand(configManager, server, plugin));
        }

        if (enabled("find")) {
            commandManager.register(
                    commandManager.metaBuilder("find").build(),
                    new FindCommand(configManager, server, plugin, databaseManager));
        }

        if (enabled("stafflist")) {
            commandManager.register("stafflist",
                    new StaffListCommand(configManager, server, luckPerms, plugin));
        }

        if (enabled("staffchat")) {
            commandManager.register("staffchat",
                    new StaffChatCommand(plugin, configManager, server, webhook, luckPerms));

            commandManager.register("sc",
                    new StaffChatCommand(plugin, configManager, server, webhook, luckPerms));

        }
        if (enabled("adminchat")) {
            commandManager.register("adminchat",
                    new AdminChatCommand(plugin, configManager, server, webhook, luckPerms));

            commandManager.register("ac",
                    new AdminChatCommand(plugin, configManager, server, webhook, luckPerms));

        }

        if (enabled("stafftime.command")) {
            commandManager.register(
                    commandManager.metaBuilder("stafftime").build(),
                    new StaffTimeCommand(configManager, server, plugin, new DateUtils(configManager), databaseManager));
        }

        if (enabled("vlist")) {
            commandManager.register(
                    commandManager.metaBuilder("vlist").build(),
                    new VListCommand(configManager, server, luckPerms, plugin));
        }

        if (enabled("helpop")) {
            commandManager.register(
                    commandManager.metaBuilder("helpop").build(),
                    new HelpopCommand(configManager, server, webhook, plugin)
            );
        }

        if (enabled("stream")) {
            commandManager.register(
                    commandManager.metaBuilder("stream").build(),
                    new StreamCommand(configManager, server, luckPerms, plugin)
            );
        }
        if (enabled("serverexecute")) {
            commandManager.register(
                    commandManager.metaBuilder("serverexecute").build(),
                    new ServerExecuteCommand(configManager, server, plugin, pluginMessageManager)
            );
        }
        if (enabled("togglesc")) {
            commandManager.register(
                    commandManager.metaBuilder("togglesc").build(),
                    new ToggleScCommand(plugin, configManager)
            );
        }
        if (enabled("ban_system")) {
            if (configManager.getBoolean("ban_system.commands.vban")) {
                commandManager.register(
                        commandManager.metaBuilder("vban").build(),
                        new BanCommand(configManager, server, banManager, plugin)
                );
            }

            if (configManager.getBoolean("ban_system.commands.vbanip")) {
                commandManager.register(
                        commandManager.metaBuilder("vbanip").build(),
                        new BanIpCommand(configManager, server, databaseManager, banManager, plugin)
                );
            }

            if (configManager.getBoolean("ban_system.commands.vunban")) {
                commandManager.register(
                        commandManager.metaBuilder("vunban").build(),
                        new UnbanCommand(configManager, server, banManager, plugin)
                );
            }

            if (configManager.getBoolean("ban_system.commands.vkick")) {
                commandManager.register(
                        commandManager.metaBuilder("vkick").build(),
                        new KickCommand(configManager, server, banManager, plugin)
                );
            }

            if (configManager.getBoolean("ban_system.commands.vcheckban")) {
                commandManager.register(
                        commandManager.metaBuilder("vcheckban").build(),
                        new CheckBanCommand(configManager, banManager, plugin)
                );
            }
        }

        if (enabled("serverwhitelist")) {
            commandManager.register(
                    commandManager.metaBuilder("serverwhitelist").build(),
                    new ServerWhitelistCommand(configManager, server, plugin));
        }

        if (enabled("private_messages")) {
            if (configManager.getBoolean("private_messages.vmsg")) {
                commandManager.register(
                        commandManager.metaBuilder("vmsg").build(),
                        new VMsgCommand(configManager, server, plugin));
                commandManager.register(
                        commandManager.metaBuilder("vmessage").build(),
                        new VMsgCommand(configManager, server, plugin));
            }
            if (configManager.getBoolean("private_messages.vreply")) {
                commandManager.register(
                        commandManager.metaBuilder("vr").build(),
                        new VReplyCommand(configManager, server, plugin));
                commandManager.register(
                        commandManager.metaBuilder("vreply").build(),
                        new VReplyCommand(configManager, server, plugin));
            }
            if (configManager.getBoolean("private_messages.vignore")) {
                commandManager.register(
                        commandManager.metaBuilder("vignore").build(),
                        new VIgnoreCommand(configManager, server, plugin));
            }
            if (configManager.getBoolean("private_messages.vspy")) {
                commandManager.register(
                        commandManager.metaBuilder("vspy").build(),
                        new VSpyCommand(configManager, server, plugin));
            }
        }
    }

    public void registerMoveCommands() {
        if (enabled("movecommands")) {
            ConfigurationNode moveCommandsNode = configManager.getRootNode().node("movecommands");
            if (!moveCommandsNode.virtual()) {
                for (ConfigurationNode commandNode : moveCommandsNode.childrenMap().values()) {
                    String commandName = commandNode.key().toString();
                    server.getCommandManager().register(commandName, new MoveCommand(configManager, server, commandName, plugin));
                }
            }
        }
    }
    public void registerMessagesCommands() {
        if (enabled("messagescommands")) {
            ConfigurationNode messagesCommandsNode = configManager.getRootNode().node("messagescommands");
            if (!messagesCommandsNode.virtual()) {
                for (ConfigurationNode commandNode : messagesCommandsNode.childrenMap().values()) {
                    String commandName = commandNode.key().toString();
                    server.getCommandManager().register(commandName, new MessagesCommand(configManager, server, commandName, pluginMessageManager, plugin));
                }
            }
        }
    }

    private boolean enabled(String path) {
        return configManager.getBoolean(path + ".enabled");
    }
}
