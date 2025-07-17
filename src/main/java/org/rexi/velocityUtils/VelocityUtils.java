package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.listeners.ChatListener;
import org.rexi.velocityUtils.listeners.PluginMessageListenerAdminChat;
import org.rexi.velocityUtils.listeners.PluginMessageListenerStaffChat;
import org.rexi.velocityUtils.listeners.StaffConnectionListener;
import org.slf4j.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

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
    private LuckPerms luckPerms = null;

    private DiscordWebhook reportWebhook;
    private DiscordWebhook staffchatWebhook;
    private DiscordWebhook adminchatWebhook;
    private DiscordWebhook staffJoinWebhook;
    private DiscordWebhook staffChangeWebhook;
    private DiscordWebhook staffLeaveWebhook;

    private final ChannelIdentifier STAFFCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "staffchat");
    private final ChannelIdentifier ADMINCHAT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "adminchat");
    public final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    public final Set<UUID> adminChatToggled = ConcurrentHashMap.newKeySet();

    private final Map<UUID, StaffSession> staffSessions = new ConcurrentHashMap<>();


    @Inject
    public VelocityUtils(ProxyServer server) {
        this.server = server;
        this.configManager = new ConfigManager();
    }

    @Inject private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        server.getChannelRegistrar().register(STAFFCHAT_CHANNEL);
        server.getChannelRegistrar().register(ADMINCHAT_CHANNEL);

        configManager.loadConfig();

        createTables();

        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("[VelocityUtils] LuckPerms detected.");
        } catch (IllegalStateException e) {
            this.luckPerms = null;
            logger.warn("[VelocityUtils] LuckPerms not detected.");
        }

        if (configManager.getBoolean("report.discord_hook.enabled")) {
            String reportWebhookUrl = configManager.getString("report.discord_hook.url");
            if (reportWebhookUrl != null && reportWebhookUrl.startsWith("http")) {
                this.reportWebhook = new DiscordWebhook(reportWebhookUrl, configManager);
                reportWebhook.setAvatarUrl(configManager.getString("report.discord_hook.avatar"));
                reportWebhook.setUsername(configManager.getString("report.discord_hook.username"));
                reportWebhook.setTitle(configManager.getString("report.discord_hook.title"));
                reportWebhook.setColorRGB(configManager.getString("report.discord_hook.color_rgb"));
            }
        }
        if (configManager.getBoolean("staffchat.discord_hook.enabled")) {
            String staffchatWebhookUrl = configManager.getString("staffchat.discord_hook.url");
            if (staffchatWebhookUrl != null && staffchatWebhookUrl.startsWith("http")) {
                this.staffchatWebhook = new DiscordWebhook(staffchatWebhookUrl, configManager);
                staffchatWebhook.setAvatarUrl(configManager.getString("staffchat.discord_hook.avatar"));
                staffchatWebhook.setUsername(configManager.getString("staffchat.discord_hook.username"));
                staffchatWebhook.setTitle(configManager.getString("staffchat.discord_hook.title"));
                staffchatWebhook.setColorRGB(configManager.getString("staffchat.discord_hook.color_rgb"));
            }
        }
        if (configManager.getBoolean("adminchat.discord_hook.enabled")) {
            String adminchatWebhookUrl = configManager.getString("adminchat.discord_hook.url");
            if (adminchatWebhookUrl != null && adminchatWebhookUrl.startsWith("http")) {
                this.adminchatWebhook = new DiscordWebhook(adminchatWebhookUrl, configManager);
                adminchatWebhook.setAvatarUrl(configManager.getString("adminchat.discord_hook.avatar"));
                adminchatWebhook.setUsername(configManager.getString("adminchat.discord_hook.username"));
                adminchatWebhook.setTitle(configManager.getString("adminchat.discord_hook.title"));
                adminchatWebhook.setColorRGB(configManager.getString("adminchat.discord_hook.color_rgb"));
            }
        }
        if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.join.enabled")) {
            String staffJoinWebhookUrl = configManager.getString("stafftime.discord_hook.join.url");
            if (staffJoinWebhookUrl != null && staffJoinWebhookUrl.startsWith("http")) {
                this.staffJoinWebhook = new DiscordWebhook(staffJoinWebhookUrl, configManager);
                staffJoinWebhook.setAvatarUrl(configManager.getString("stafftime.discord_hook.join.avatar"));
                staffJoinWebhook.setUsername(configManager.getString("stafftime.discord_hook.join.username"));
                staffJoinWebhook.setTitle(configManager.getString("stafftime.discord_hook.join.title"));
                staffJoinWebhook.setColorRGB(configManager.getString("stafftime.discord_hook.join.color_rgb"));
            }
        }
        if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.change.enabled")) {
            String staffChangeWebhookUrl = configManager.getString("stafftime.discord_hook.change.url");
            if (staffChangeWebhookUrl != null && staffChangeWebhookUrl.startsWith("http")) {
                this.staffChangeWebhook = new DiscordWebhook(staffChangeWebhookUrl, configManager);
                staffChangeWebhook.setAvatarUrl(configManager.getString("stafftime.discord_hook.change.avatar"));
                staffChangeWebhook.setUsername(configManager.getString("stafftime.discord_hook.change.username"));
                staffChangeWebhook.setTitle(configManager.getString("stafftime.discord_hook.change.title"));
                staffChangeWebhook.setColorRGB(configManager.getString("stafftime.discord_hook.change.color_rgb"));
            }
        }
        if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.leave.enabled")) {
            String staffLeaveWebhookUrl = configManager.getString("stafftime.discord_hook.leave.url");
            if (staffLeaveWebhookUrl != null && staffLeaveWebhookUrl.startsWith("http")) {
                this.staffLeaveWebhook = new DiscordWebhook(staffLeaveWebhookUrl, configManager);
                staffLeaveWebhook.setAvatarUrl(configManager.getString("stafftime.discord_hook.leave.avatar"));
                staffLeaveWebhook.setUsername(configManager.getString("stafftime.discord_hook.leave.username"));
                staffLeaveWebhook.setTitle(configManager.getString("stafftime.discord_hook.leave.title"));
                staffLeaveWebhook.setColorRGB(configManager.getString("stafftime.discord_hook.leave.color_rgb"));
            }
        }

        server.getEventManager().register(this, new ChatListener(this, configManager, server, staffchatWebhook, adminchatWebhook));
        server.getEventManager().register(this, new PluginMessageListenerStaffChat(this, server, configManager, staffchatWebhook));
        server.getEventManager().register(this, new PluginMessageListenerAdminChat(this, server, configManager, adminchatWebhook));
        server.getEventManager().register(this, new StaffConnectionListener(this, staffSessions, configManager, staffJoinWebhook, staffChangeWebhook, staffLeaveWebhook));

        server.getCommandManager().register("alert", new AlertCommand(configManager,server));
        server.getCommandManager().register("velocityutils", new VelocityUtilsCommand(configManager, server));
        server.getCommandManager().register("vu", new VelocityUtilsCommand(configManager, server));
        server.getCommandManager().register("maintenance", new MaintenanceCommand(configManager, server));
        server.getCommandManager().register("report", new ReportCommand(configManager, server, reportWebhook));
        server.getCommandManager().register("goto", new GotoCommand(configManager, server));
        server.getCommandManager().register("find", new FindCommand(configManager, server));
        if (this.luckPerms != null) {
            server.getCommandManager().register("stafflist", new StaffListCommand(configManager, server, luckPerms));
        } else {
            server.getCommandManager().register("stafflist", new StaffListCommand(configManager, server, null));
        }
        server.getCommandManager().register("staffchat", new StaffChatCommand(this, configManager, server, staffchatWebhook));
        server.getCommandManager().register("sc", new StaffChatCommand(this, configManager, server, staffchatWebhook));
        server.getCommandManager().register("adminchat", new AdminChatCommand(this, configManager, server, adminchatWebhook));
        server.getCommandManager().register("ac", new AdminChatCommand(this, configManager, server, adminchatWebhook));
        server.getCommandManager().register("stafftime", new StaffTimeCommand(configManager, server, this));
        server.getCommandManager().register("vlist", new VListCommand(configManager, server));

        System.out.println(Component.text("The plugin has been activated").color(NamedTextColor.GREEN));
        System.out.println(Component.text("Thank you for using Rexi666 plugins").color(NamedTextColor.BLUE));
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        try {
            Component motd;
            if (configManager.isMaintenanceMode()) {
                motd = configManager.getMaintenanceMotd();  // Obtén el MotD de mantenimiento
            } else {
                motd = configManager.getMotd();  // Obtén el MotD normal
            }

            ServerPing ping = event.getPing();
            ServerPing updatePing = ping.asBuilder().description(motd).build();
            event.setPing(updatePing);
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

    public Set<UUID> getStaffChatToggled() {
        return staffChatToggled;
    }

    public Set<UUID> getAdminChatToggled() {
        return adminChatToggled;
    }

    private void createTables() {
        String staffTimeTable = """
        CREATE TABLE IF NOT EXISTS staff_time_daily (
            uuid TEXT NOT NULL,
            date TEXT NOT NULL, -- Guardamos fecha en formato ISO yyyy-MM-dd
            duration_seconds INTEGER NOT NULL,
            PRIMARY KEY (uuid, date)
        );
        """;

        String playerInfoTable = """
    CREATE TABLE IF NOT EXISTS player_info (
        uuid TEXT PRIMARY KEY,
        name TEXT NOT NULL
    );
    """;
        try (var conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(staffTimeTable);
            stmt.execute(playerInfoTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        // Cambia esta ruta si quieres que la base esté en otro sitio
        return DriverManager.getConnection("jdbc:sqlite:plugins/VelocityUtils/stafftime.db");
    }

    public Map<UUID, StaffSession> getStaffSessions() {
        return staffSessions;
    }

}
