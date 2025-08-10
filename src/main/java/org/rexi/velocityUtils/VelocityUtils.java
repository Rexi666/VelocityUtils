package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.velocity.Metrics;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.listeners.*;
import org.slf4j.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.spongepowered.configurate.ConfigurationNode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "velocityutils",
        name = "VelocityUtils",
        version = BuildConstants.VERSION,
        authors = {"Rexi666"},
        dependencies = {@Dependency(id = "luckperms", optional = true)})
public class VelocityUtils {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final PluginContainer plugin;
    private LuckPerms luckPerms = null;

    private DiscordWebhook webhook;

    private final ChannelIdentifier STAFFCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "staffchat");
    private final ChannelIdentifier ADMINCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "adminchat");
    public final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    public final Set<UUID> adminChatToggled = ConcurrentHashMap.newKeySet();
    private final ChannelIdentifier PLACEHOLDER_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "placeholders");
    private final ChannelIdentifier ALERT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "alerts");

    private final Map<UUID, StaffSession> staffSessions = new ConcurrentHashMap<>();

    boolean isMySQL = false;

    @Inject
    public VelocityUtils(ProxyServer server, PluginContainer plugin) {
        this.server = server;
        this.plugin = plugin;
        this.configManager = new ConfigManager();
        this.webhook = new DiscordWebhook(configManager);
    }

    @Inject private Logger logger;
    @Inject private Metrics.Factory metricsFactory;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        server.getChannelRegistrar().register(STAFFCHAT_CHANNEL);
        server.getChannelRegistrar().register(ADMINCHAT_CHANNEL);
        server.getChannelRegistrar().register(PLACEHOLDER_CHANNEL);
        server.getChannelRegistrar().register(ALERT_CHANNEL);

        configManager.loadConfig();

        createTables();

        new UpdateChecker(server, plugin, configManager, BuildConstants.VERSION, "https://raw.githubusercontent.com/Rexi666/VelocityUtils/main/latest-version.txt").checkForUpdates();

        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("[VelocityUtils] LuckPerms detected.");
        } catch (IllegalStateException e) {
            this.luckPerms = null;
            logger.warn("[VelocityUtils] LuckPerms not detected.");
        }

        server.getEventManager().register(this, new ChatListener(this));
        server.getEventManager().register(this, new StaffConnectionListener(this, staffSessions, configManager, server, luckPerms, webhook, new DateUtils(configManager)));

        server.getEventManager().register(this, new PluginMessageListenerStaffChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerAdminChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerPlaceholders(server));
        server.getEventManager().register(this, new PluginMessageListenerAlerts(server, configManager));

        registerCommands();
        registerMoveCommands();
        registerMessagesCommands();

        Metrics metrics = metricsFactory.make(this, 26742);

        System.out.println(Component.text("The plugin has been activated").color(NamedTextColor.GREEN));
        System.out.println(Component.text("Thank you for using Rexi666 plugins").color(NamedTextColor.BLUE));
    }

    public void registerMoveCommands() {
        if (configManager.getBoolean("movecommands.enabled")) {
            ConfigurationNode moveCommandsNode = configManager.getRootNode().node("movecommands");
            if (!moveCommandsNode.virtual()) {
                for (ConfigurationNode commandNode : moveCommandsNode.childrenMap().values()) {
                    String commandName = commandNode.key().toString();
                    server.getCommandManager().register(commandName, new MoveCommand(configManager, server, commandName));
                }
            }
        }
    }
    public void registerMessagesCommands() {
        if (configManager.getBoolean("messagescommands.enabled")) {
            ConfigurationNode messagesCommandsNode = configManager.getRootNode().node("messagescommands");
            if (!messagesCommandsNode.virtual()) {
                for (ConfigurationNode commandNode : messagesCommandsNode.childrenMap().values()) {
                    String commandName = commandNode.key().toString();
                    server.getCommandManager().register(commandName, new MessagesCommand(configManager, server, commandName));
                }
            }
        }
    }

    public void registerCommands() {
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("velocityutils").build(),
                new VelocityUtilsCommand(configManager, server, this));

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vu").build(),
                new VelocityUtilsCommand(configManager, server, this));

        if (configManager.getBoolean("alert.enabled")) {
            server.getCommandManager().register("alert", new AlertCommand(configManager,server));
        }

        if (configManager.getBoolean("maintenance.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("maintenance").build(),
                    new MaintenanceCommand(configManager, server));
        }

        if (configManager.getBoolean("report.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("report").build(),
                    new ReportCommand(configManager, server, webhook)
            );
        }

        if (configManager.getBoolean("goto.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("goto").build(),
                    new GotoCommand(configManager, server));
        }

        if (configManager.getBoolean("find.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("find").build(),
                    new FindCommand(configManager, server));
        }

        if (configManager.getBoolean("stafflist.enabled")) {
            server.getCommandManager().register("stafflist", new StaffListCommand(configManager, server, luckPerms));
        }

        if (configManager.getBoolean("staffchat.enabled")) {
            server.getCommandManager().register("staffchat", new StaffChatCommand(this, configManager, server, webhook, luckPerms));

            server.getCommandManager().register("sc", new StaffChatCommand(this, configManager, server, webhook, luckPerms));

        }
        if (configManager.getBoolean("adminchat.enabled")) {
            server.getCommandManager().register("adminchat", new AdminChatCommand(this, configManager, server, webhook, luckPerms));

            server.getCommandManager().register("ac", new AdminChatCommand(this, configManager, server, webhook, luckPerms));

        }

        if (configManager.getBoolean("stafftime.command.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("stafftime").build(),
                    new StaffTimeCommand(configManager, server, this, new DateUtils(configManager)));
        }

        if (configManager.getBoolean("vlist.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vlist").build(),
                    new VListCommand(configManager, server, luckPerms));
        }

        if (configManager.getBoolean("helpop.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("helpop").build(),
                    new HelpopCommand(configManager, server, webhook)
            );
        }

        if (configManager.getBoolean("stream.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("stream").build(),
                    new StreamCommand(configManager, server, luckPerms)
            );
        }
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        try {
            Component motd;
            if (configManager.isMaintenanceMode()) {
                motd = configManager.getMaintenanceMotd();  // Obtén el MotD de mantenimiento
                ServerPing ping = event.getPing();
                ServerPing updatePing = ping.asBuilder().description(motd).build();
                event.setPing(updatePing);
            } else if (configManager.getBoolean("motd.enabled")) {
                motd = configManager.getMotd();  // Obtén el MotD normal
                ServerPing ping = event.getPing();
                ServerPing updatePing = ping.asBuilder().description(motd).build();
                event.setPing(updatePing);
            }
        } catch (Exception e) {
            logger.error("Error al actualizar el MOTD en ProxyPingEvent", e);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (configManager.isMaintenanceMode()) {
            List<String> allowedPlayers = configManager.getAllowedPlayers();
            String username = event.getPlayer().getUsername();
            if (!allowedPlayers.contains(username)) {
                String under_maintenance = configManager.getMessage("maintenance_not_on_list");
                event.setResult(LoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(under_maintenance)));
            }
        }
    }

    @Subscribe
    public void PostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("velocityutils.admin")) {
            new UpdateChecker(server, plugin, configManager, BuildConstants.VERSION, "https://raw.githubusercontent.com/Rexi666/VelocityUtils/main/latest-version.txt").checkForUpdatesPlayer(player);
        }
    }

    public Set<UUID> getStaffChatToggled() {
        return staffChatToggled;
    }

    public Set<UUID> getAdminChatToggled() {
        return adminChatToggled;
    }

    private void createTables() {
        String dbType = configManager.getString("database.type").toLowerCase();

        if (!dbType.equalsIgnoreCase("mysql")) {
            dbType = "sqlite";
        }

        String staffTimeTable;
        String playerInfoTable;

        if (dbType.equals("mysql")) {
            staffTimeTable = """
        CREATE TABLE IF NOT EXISTS staff_time_daily (
            uuid VARCHAR(36) NOT NULL,
            date DATE NOT NULL,
            duration_seconds INT NOT NULL,
            PRIMARY KEY (uuid, date)
        );
        """;

            playerInfoTable = """
        CREATE TABLE IF NOT EXISTS player_info (
            uuid VARCHAR(36) PRIMARY KEY,
            name VARCHAR(16) NOT NULL
        );
        """;
        } else {
            staffTimeTable = """
        CREATE TABLE IF NOT EXISTS staff_time_daily (
            uuid TEXT NOT NULL,
            date TEXT NOT NULL,
            duration_seconds INTEGER NOT NULL,
            PRIMARY KEY (uuid, date)
        );
        """;

            playerInfoTable = """
        CREATE TABLE IF NOT EXISTS player_info (
            uuid TEXT PRIMARY KEY,
            name TEXT NOT NULL
        );
        """;
        }

        try (var conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(staffTimeTable);
            stmt.execute(playerInfoTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        String dbType = configManager.getString("database.type").toLowerCase();

        if (!dbType.equalsIgnoreCase("mysql")) {
            dbType = "sqlite";
        } else {
            isMySQL = true;
        }

        if (dbType.equals("mysql")) {
            String host = configManager.getString("database.mysql.host");
            int port = configManager.getInt("database.mysql.port");
            String database = configManager.getString("database.mysql.database");
            String username = configManager.getString("database.mysql.username");
            String password = configManager.getString("database.mysql.password");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Cargar driver MySQL
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found", e);
            }

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            return DriverManager.getConnection(url, username, password);
        } else {
            try {
                Class.forName("org.sqlite.JDBC"); // Cargar driver SQLite
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite driver not found", e);
            }

            return DriverManager.getConnection("jdbc:sqlite:plugins/VelocityUtils/stafftime.db");
        }
    }

    public Map<UUID, StaffSession> getStaffSessions() {
        return staffSessions;
    }

    public boolean isUsingMySQL() {
        return isMySQL;
    }

}
