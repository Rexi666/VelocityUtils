package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.velocity.Metrics;
import org.rexi.velocityUtils.api.VelocityUtilsAPI;
import org.rexi.velocityUtils.api.VelocityUtilsAPIImpl;
import org.rexi.velocityUtils.api.VelocityUtilsProvider;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.commands.banSystem.*;
import org.rexi.velocityUtils.listeners.*;
import org.rexi.velocityUtils.utils.BanData;
import org.slf4j.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    private final BrandListener brandListener;
    private LuckPerms luckPerms = null;
    private VelocityUtilsAPI api;

    private DiscordWebhook webhook;

    private ScheduledTask alertsTask;
    private int currentAlertIndex = 0;
    private List<String> alertList = new ArrayList<>();

    private final ChannelIdentifier STAFFCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "staffchat");
    private final ChannelIdentifier ADMINCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "adminchat");
    public final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    public final Set<UUID> adminChatToggled = ConcurrentHashMap.newKeySet();
    private final ChannelIdentifier PLACEHOLDER_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "placeholders");
    private final ChannelIdentifier ALERT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "alerts");
    private final ChannelIdentifier SERVEREXECUTE_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "serverexecute");

    private final Map<UUID, StaffSession> staffSessions = new ConcurrentHashMap<>();

    public final Map<String, List<String>> pendingCommands = new HashMap<>();

    public List<UUID> disabledSC = new ArrayList<>();

    boolean isMySQL = false;

    public Map<String, BanData> banCache = new ConcurrentHashMap<>();
    public Map<String, List<String>> subIpBanCache = new ConcurrentHashMap<>();

    @Inject
    public VelocityUtils(ProxyServer server, PluginContainer plugin) {
        this.server = server;
        this.plugin = plugin;
        this.configManager = new ConfigManager();
        this.brandListener = new BrandListener(configManager, server);
        this.webhook = new DiscordWebhook(configManager);
    }

    @Inject public Logger logger;
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
        server.getChannelRegistrar().register(SERVEREXECUTE_CHANNEL);
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.from("minecraft:brand"));

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
        server.getEventManager().register(this, brandListener);

        server.getEventManager().register(this, new PluginMessageListenerStaffChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerAdminChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerPlaceholders(server));
        server.getEventManager().register(this, new PluginMessageListenerAlerts(server, configManager));

        server.getEventManager().register(this, new ServerExecuteListener(this, server));

        registerCommands();
        registerMoveCommands();
        registerMessagesCommands();
        startRegularAlerts();

        Metrics metrics = metricsFactory.make(this, 26742);

        this.api = new VelocityUtilsAPIImpl(this, server, configManager, luckPerms, webhook);
        VelocityUtilsProvider.register(this.api);

        server.sendMessage(Component.text("VelocityUtils has been activated").color(NamedTextColor.GREEN));
        server.sendMessage(Component.text("Thank you for using Rexi666 plugins").color(NamedTextColor.BLUE));
    }

    public VelocityUtilsAPI getAPI() {
        return api;
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
                new VelocityUtilsCommand(configManager, server, this, brandListener));

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vu").build(),
                new VelocityUtilsCommand(configManager, server, this, brandListener));

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
                    new FindCommand(configManager, server, this));
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
        if (configManager.getBoolean("serverexecute.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("serverexecute").build(),
                    new ServerExecuteCommand(configManager, server, this)
            );
        }
        if (configManager.getBoolean("togglesc.enabled")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("togglesc").build(),
                    new ToggleScCommand(this, configManager)
            );
        }
        if (configManager.getBoolean("ban_system.enabled") && configManager.getBoolean("ban_system.commands.vban")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vban").build(),
                    new BanCommand(configManager, server, this)
            );
        }
        if (configManager.getBoolean("ban_system.enabled") && configManager.getBoolean("ban_system.commands.vbanip")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vbanip").build(),
                    new BanIpCommand(configManager, server, this)
            );
        }
        if (configManager.getBoolean("ban_system.enabled") && configManager.getBoolean("ban_system.commands.vunban")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vunban").build(),
                    new UnbanCommand(configManager, server, this)
            );
        }
        if (configManager.getBoolean("ban_system.enabled") && configManager.getBoolean("ban_system.commands.vkick")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vkick").build(),
                    new KickCommand(configManager, server, this)
            );
        }
        if (configManager.getBoolean("ban_system.enabled") && configManager.getBoolean("ban_system.commands.vcheckban")) {
            server.getCommandManager().register(
                    server.getCommandManager().metaBuilder("vcheckban").build(),
                    new CheckBanCommand(configManager, server, this)
            );
        }
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        try {
            Component motd;
            if (configManager.isMaintenanceMode()) {
                motd = configManager.getMaintenanceMotd();
                ServerPing ping = event.getPing();
                ServerPing updatePing = ping.asBuilder().description(motd).build();
                event.setPing(updatePing);
            } else if (configManager.getBoolean("motd.enabled")) {
                motd = configManager.getMotd();
                ServerPing ping = event.getPing();
                ServerPing updatePing = ping.asBuilder().description(motd).build();
                event.setPing(updatePing);
            }
        } catch (Exception e) {
            logger.error("Error trying to update MOTD", e);
        }
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        // Comprobar ban
        if (configManager.getBoolean("ban_system.enabled")) {
            String playerName = event.getUsername().toLowerCase();
            BanData cached = banCache.get(playerName);
            if (cached != null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(banDenyMessage(cached, event.getUsername())));
                String message = configManager.getMessage("try_join_ban");
                message = message.replace("{player}", event.getUsername())
                        .replace("{reason}", cached.getReason());
                Component finalMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                server.getConsoleCommandSource().sendMessage(finalMessage);

                for (Player player : server.getAllPlayers()) {
                    if (player.hasPermission("velocityutils.bansystem.notify")) {
                        player.sendMessage(finalMessage);
                    }
                }
                return;
            }

            String ip = ((InetSocketAddress) event.getConnection()
                    .getRemoteAddress()).getAddress().getHostAddress();

            BanData ban = loadBan(playerName, ip);

            if (ban != null) {
                banCache.put(playerName, ban);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(banDenyMessage(ban, event.getUsername())));

                boolean ipBanFromOtherAccount = ban.getIpBan() && !ban.getName().equals(playerName);

                if (ipBanFromOtherAccount) {
                    String bannedName = ban.getName();
                    List<String> subIpBans = subIpBanCache.getOrDefault(bannedName, new ArrayList<>());
                    if (!subIpBans.contains(playerName)) {
                        subIpBans.add(playerName);
                    }
                    subIpBanCache.put(bannedName, subIpBans);

                    String message = configManager.getMessage("try_join_banip");
                    message = message.replace("{player}", event.getUsername())
                            .replace("{ip_playername}", bannedName)
                            .replace("{reason}", ban.getReason());
                    Component finalMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                    server.getConsoleCommandSource().sendMessage(finalMessage);

                    for (Player player : server.getAllPlayers()) {
                        if (player.hasPermission("velocityutils.bansystem.notify")) {
                            player.sendMessage(finalMessage);
                        }
                    }
                } else {
                    String message = configManager.getMessage("try_join_ban");
                    message = message.replace("{player}", event.getUsername())
                            .replace("{reason}", ban.getReason());
                    Component finalMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
                    server.getConsoleCommandSource().sendMessage(finalMessage);

                    for (Player player : server.getAllPlayers()) {
                        if (player.hasPermission("velocityutils.bansystem.notify")) {
                            player.sendMessage(finalMessage);
                        }
                    }
                }
                return;
            }
        }

        // Comprobar mantenimiento
        if (configManager.isMaintenanceMode()) {
            List<String> allowedPlayers = configManager.getAllowedPlayers();
            String username = event.getUsername();
            if (!allowedPlayers.contains(username)) {
                String under_maintenance = configManager.getMessage("maintenance_not_on_list");
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(under_maintenance)));
            }
        }
    }

    public BanData loadBan(String name, String ip) {
        try (Connection conn = getConnection()) {
            var stmt = conn.prepareStatement("""
            SELECT name, ip, ipban, banned_by, banned_at, reason
            FROM player_bans
            WHERE LOWER(name) = ?
            UNION ALL
            SELECT name, ip, ipban, banned_by, banned_at, reason
            FROM player_bans
            WHERE ip = ? AND ipban = true
            LIMIT 1
        """);

            stmt.setString(1, name);
            stmt.setString(2, ip);

            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new BanData(
                        rs.getString("name"),
                        rs.getString("ip"),
                        rs.getBoolean("ipban"),
                        rs.getString("banned_by"),
                        Instant.ofEpochMilli(rs.getLong("banned_at")),
                        rs.getString("reason")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Component banDenyMessage(BanData ban, String name) {
        List<String> original = configManager.getStringList("ban_system.screen_messages.ban");

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        .withZone(ZoneId.systemDefault());

        String joined = String.join("\n", original)
                .replace("{player}", name)
                .replace("{banned_by}", ban.getBannedBy())
                .replace("{banned_at}", formatter.format(ban.getBannedAt()))
                .replace("{reason}", ban.getReason());

        return LegacyComponentSerializer.legacyAmpersand().deserialize(joined);
    }
    public Component kickDenyMessage(String player, String kickedBy, String reason) {
        List<String> original = configManager.getStringList("ban_system.screen_messages.kick");

        String joined = String.join("\n", original)
                .replace("{player}", player)
                .replace("{kicked_by}", kickedBy)
                .replace("{reason}", reason);

        return LegacyComponentSerializer.legacyAmpersand().deserialize(joined);
    }

    public void saveBan(BanData banData) {
        String sql = """
        INSERT INTO player_bans (name, ip, ipban, banned_by, banned_at, reason)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(name) DO UPDATE SET
            ip = excluded.ip,
            ipban = excluded.ipban,
            banned_by = excluded.banned_by,
            banned_at = excluded.banned_at,
            reason = excluded.reason
    """;

        if (isUsingMySQL()) {
            sql = """
            INSERT INTO player_bans (name, ip, ipban, banned_by, banned_at, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                ip = VALUES(ip),
                ipban = VALUES(ipban),
                banned_by = VALUES(banned_by),
                banned_at = VALUES(banned_at),
                reason = VALUES(reason)
        """;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, banData.getName().toLowerCase());
            stmt.setString(2, banData.getIp());
            stmt.setBoolean(3, banData.getIpBan());
            stmt.setString(4, banData.getBannedBy());
            stmt.setTimestamp(5, Timestamp.from(banData.getBannedAt()));
            stmt.setString(6, banData.getReason());

            stmt.executeUpdate();

            banCache.put(banData.getName().toLowerCase(), banData);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBan(BanData banData) {
        String sql = "DELETE FROM player_bans WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String name = banData.getName().toLowerCase();

            stmt.setString(1, name);
            stmt.executeUpdate();

            banCache.remove(name);

        } catch (SQLException e) {
            e.printStackTrace();
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
        String playerBans;

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
            name VARCHAR(16) NOT NULL,
            last_join TIMESTAMP,
            player_ip VARCHAR(45)
        );
        """;

            playerBans = """
        CREATE TABLE IF NOT EXISTS player_bans (
            name VARCHAR(20) PRIMARY KEY,
            ip VARCHAR(45),
            ipban BOOLEAN NOT NULL,
            banned_by VARCHAR(20) NOT NULL,
            banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            reason VARCHAR(16)
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
            name TEXT NOT NULL,
            last_join TEXT,
            player_ip TEXT
        );
        """;

            playerBans = """
        CREATE TABLE IF NOT EXISTS player_bans (
            name TEXT PRIMARY KEY,
            ip TEXT,
            ipban BOOLEAN NOT NULL,
            banned_by TEXT NOT NULL,
            banned_at TEXT DEFAULT CURRENT_TIMESTAMP,
            reason TEXT
        );
        """;
        }

        try (var conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(staffTimeTable);
            stmt.execute(playerInfoTable);
            stmt.execute(playerBans);

            try {
                if (dbType.equals("mysql")) {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN IF NOT EXISTS last_join TIMESTAMP");
                } else {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN last_join TEXT");
                }
            } catch (SQLException ignore) {
                // si ya existe, SQLite lanza error → lo ignoramos
            }
            try {
                if (dbType.equals("mysql")) {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN IF NOT EXISTS player_ip TIMESTAMP");
                } else {
                    stmt.execute("ALTER TABLE player_info ADD COLUMN player_ip TEXT");
                }
            } catch (SQLException ignore) {
                // si ya existe, SQLite lanza error → lo ignoramos
            }
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

            File oldDb = new File("plugins/VelocityUtils/stafftime.db");
            File finalDb = new File("plugins/VelocityUtils/data.db");
            if (oldDb.exists() && !finalDb.exists()) {
                if (oldDb.renameTo(finalDb)) {
                    logger.info("[VelocityUtils] Renaming database to data.db");
                } else {
                    logger.warn("[VelocityUtils] stafftime.db couldnt be renamed");
                    finalDb = oldDb;
                }
            }

            try {
                Class.forName("org.sqlite.JDBC"); // Cargar driver SQLite
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite driver not found", e);
            }

            return DriverManager.getConnection("jdbc:sqlite:" + finalDb.getPath());
        }
    }

    public Map<UUID, StaffSession> getStaffSessions() {
        return staffSessions;
    }

    public boolean isUsingMySQL() {
        return isMySQL;
    }

    public void sendCommandToServer(RegisteredServer toserver, String command) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(out)) {
            data.writeUTF("execute");
            data.writeUTF(command);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        toserver.sendPluginMessage(
                MinecraftChannelIdentifier.from("velocityutils:serverexecute"),
                out.toByteArray()
        );
    }

    public void startRegularAlerts() {
        if (alertsTask != null) {
            alertsTask.cancel();
            alertsTask = null;
        }

        if (!configManager.getBoolean("regular_alerts.enabled")) {
            return;
        }

        int delay = configManager.getInt("regular_alerts.delay_seconds");

        loadAlerts();

        alertsTask = server.getScheduler()
                .buildTask(this, () -> sendNextAlert())
                .delay(delay, TimeUnit.SECONDS)
                .repeat(delay, TimeUnit.SECONDS)
                .schedule();
    }

    private void loadAlerts() {
        alertList.clear();
        currentAlertIndex = 0;

        ConfigurationNode alertsNode = configManager.getRootNode().node("regular_alerts", "alerts");

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : alertsNode.childrenMap().entrySet()) {
            alertList.add(entry.getKey().toString());
        }
    }

    private void sendNextAlert() {
        if (alertList.isEmpty()) {
            return;
        }

        String alert = alertList.get(currentAlertIndex);
        List<String> messages = configManager.getStringList("regular_alerts.alerts." + alert + ".message");

        String action = configManager.getString("regular_alerts.alerts." + alert + ".action");
        String hover = configManager.getString("regular_alerts.alerts." + alert + ".hover");
        String click_action = configManager.getString("regular_alerts.alerts." + alert + ".click_action");

        if (click_action == null ||
                (!click_action.equalsIgnoreCase("OPEN_URL") && !click_action.equalsIgnoreCase("RUN_COMMAND"))) {
            click_action = "NONE";
        }

        if (messages == null || messages.isEmpty()) {
            logger.warn("Error trying to send regular alert '{}': message is empty: ", alert);
            return;
        }

        if (click_action.equalsIgnoreCase("OPEN_URL") || click_action.equalsIgnoreCase("RUN_COMMAND")) {
            if (action == null || action.isEmpty() || hover == null || hover.isEmpty()) {
                logger.warn("Error trying to send regular alert '{}': action or hover message is missing or empty: ", alert);
                return;
            }
        }

        for (Player player : server.getAllPlayers()) {
            for (String line : messages) {
                Component messageLine = legacy(line);

                if (click_action.equalsIgnoreCase("OPEN_URL")) {
                    messageLine = messageLine
                            .clickEvent(ClickEvent.openUrl(action))
                            .hoverEvent(HoverEvent.showText(legacy(hover)));
                } else if (click_action.equalsIgnoreCase("RUN_COMMAND")) {
                    messageLine = messageLine
                            .clickEvent(ClickEvent.runCommand(action))
                            .hoverEvent(HoverEvent.showText(legacy(hover)));
                }
                player.sendMessage(messageLine);
            }
        }

        currentAlertIndex++;

        if (currentAlertIndex >= alertList.size()) {
            currentAlertIndex = 0;
        }
    }

    private net.kyori.adventure.text.Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
