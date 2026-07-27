package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.*;
import org.rexi.velocityUtils.utils.DateUtils;
import org.rexi.velocityUtils.utils.DiscordWebhook;
import org.rexi.velocityUtils.utils.StaffSession;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class StaffConnectionListener {

    private final ConfigManager configManager;
    private final VelocityUtils plugin;
    private final Map<UUID, StaffSession> sessions;
    private final ProxyServer server;
    private final LuckPerms luckPerms;
    private final DiscordWebhook webhook;
    private final DateUtils dateUtils;

    public StaffConnectionListener(VelocityUtils plugin, Map<UUID, StaffSession> sessions, ConfigManager configManager, ProxyServer server, LuckPerms luckPerms, DiscordWebhook webhook, DateUtils dateUtils) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
        this.webhook = webhook;
        this.dateUtils = dateUtils;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String newServer = event.getServer().getServerInfo().getName();  // Servidor al que acaba de conectar
        String previousServer = event.getPreviousServer()
                .map(srv -> srv.getServerInfo().getName())
                .orElse("N/A");

        staffJoinMessage(player, newServer, previousServer);
        savePlayerInfo(player);

        if (isAdmin(player)) {
            return; // No action for admins
        }
        if (isStaff(player)) {
            StaffSession session = sessions.get(player.getUniqueId());

            if (session == null) {
                // Primera vez que detectamos al jugador, creamos sesión con el servidor actual
                sessions.put(player.getUniqueId(), new StaffSession(Instant.now(), newServer));

                if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.join.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.join.message");
                    String msg = raw.replace("{player}", player.getUsername());
                    sendJoinWebhook(player.getUsername(), msg);
                }
            } else {
                // Sesión ya existente, hacemos switch de servidor
                session.switchServer(newServer);

                if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.change.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.change.message");
                    String msg = raw
                            .replace("{player}", player.getUsername())
                            .replace("{from}", previousServer)
                            .replace("{to}", newServer);
                    sendChangeWebhook(player.getUsername(), msg);
                }
            }
        }
    }



    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        savePlayerInfo(player);

        staffLeaveMessage(player);
        if (isAdmin(player)) {
            return; // No action for admins
        }
        if (isStaff(player)) {
            StaffSession session = sessions.remove(player.getUniqueId());
            if (session != null) {
                session.finalizeSession();

                LocalDate today = LocalDate.now();
                Duration sessionDuration = session.getTotalTime();
                saveSessionDurationDaily(player.getUniqueId(), today, sessionDuration);

                Duration daily = getDurationForRange(player.getUniqueId(), today, today);
                Duration weekly = getDurationForRange(player.getUniqueId(), dateUtils.getStartOfWeek(), dateUtils.getEndOfWeek());
                Duration monthly = getDurationForRange(player.getUniqueId(), dateUtils.getStartOfMonth(), dateUtils.getEndOfMonth());

                if (configManager.getBoolean("stafftime.discord_hook.enabled") && configManager.getBoolean("stafftime.discord_hook.leave.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.leave.message");
                    String serverTimeFormat = configManager.getString("stafftime.discord_hook.leave.serverstime");

                    StringBuilder serverTimes = new StringBuilder();
                    session.getTimePerServer().forEach((server, duration) -> {
                        String formatted = serverTimeFormat
                                .replace("{server}", server)
                                .replace("{time}", formatDuration(duration));
                        serverTimes.append(formatted).append("\n");
                    });

                    String msg = raw
                            .replace("{player}", player.getUsername())
                            .replace("{time}", formatDuration(session.getTotalTime()))
                            .replace("{time_daily}", formatDuration(daily))
                            .replace("{time_weekly}", formatDuration(weekly))
                            .replace("{time_monthly}", formatDuration(monthly))
                            .replace("{serverstime}", serverTimes.toString().trim());
                    sendLeaveWebhook(player.getUsername(), msg);
                }
            }
        }
    }

    private boolean isStaff(Player player) {
        return player.hasPermission("velocityutils.stafftime.staff");
    }

    private boolean isAdmin(Player player) {
        return player.hasPermission("velocityutils.stafftime.exclude");
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String hour_simbol = configManager.getMessageString("hour_simbol");
        String minute_simbol = configManager.getMessageString("minute_simbol");
        String second_simbol = configManager.getMessageString("second_simbol");


        return String.format("%02d"+ hour_simbol + " %02d" + minute_simbol + " %02d" + second_simbol, hours, minutes, seconds);
    }

    public void saveSessionDurationDaily(UUID uuid, LocalDate date, Duration duration) {
        String sql;
        if (plugin.isUsingMySQL()) {
            sql = """
            INSERT INTO staff_time_daily (uuid, date, duration_seconds)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
            duration_seconds = duration_seconds + VALUES(duration_seconds);
            """;
        } else {
            sql = """
            INSERT INTO staff_time_daily (uuid, date, duration_seconds)
            VALUES (?, ?, ?)
            ON CONFLICT(uuid, date) DO UPDATE SET
            duration_seconds = duration_seconds + excluded.duration_seconds;
            """;
        }
        try (var conn = plugin.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, date.toString()); // yyyy-MM-dd
            pstmt.setLong(3, duration.getSeconds());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Duration getDurationForRange(UUID uuid, LocalDate startDate, LocalDate endDate) {
        String sql = """
        SELECT SUM(duration_seconds) FROM staff_time_daily
        WHERE uuid = ? AND date BETWEEN ? AND ?
        """;
        try (var conn = plugin.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, startDate.toString());
            pstmt.setString(3, endDate.toString());
            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long seconds = rs.getLong(1);
                    return Duration.ofSeconds(seconds);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Duration.ZERO;
    }

    private void savePlayerInfo(Player player) {
        String ip = player.getRemoteAddress()
                .getAddress()
                .getHostAddress();

        String sql;
        if (plugin.isUsingMySQL()) {
            sql = """
        INSERT INTO player_info (uuid, name, last_join, player_ip)
        VALUES (?, ?, NOW(), ?)
        ON DUPLICATE KEY UPDATE 
            name = VALUES(name),
            last_join = VALUES(last_join),
            player_ip = VALUES(player_ip);
        """;
        } else {
            sql = """
        INSERT INTO player_info (uuid, name, last_join, player_ip)
        VALUES (?, ?, datetime('now'), ?)
        ON CONFLICT(uuid) DO UPDATE SET 
                name = excluded.name,
                last_join = excluded.last_join,
                player_ip = excluded.player_ip;
        """;
        }

        try (var conn = plugin.getConnection();
             var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, player.getUsername());
            pstmt.setString(3, ip);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void staffJoinMessage(Player player, String newServer, String previousServer) {
        if (configManager.getBoolean("staffjoin.enabled")) {
            if (player.hasPermission("velocityutils.staffjoin.staff")) {
                String prefixRaw = obtenerRango(player);

                Component prefix = deserializePrefix(prefixRaw);

                if (previousServer.equalsIgnoreCase("N/A")) {
                    String format = configManager.getString("staffjoin.join_message")
                            .replace("{player}", player.getUsername());

                    Component joinMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("{rank}")
                                    .replacement(prefix)
                                    .build());

                    server.getAllPlayers().forEach(target -> {
                        if (target.hasPermission("velocityutils.staffjoin.notify")) {
                            target.sendMessage(joinMessage);
                        }
                    });
                } else {
                    String format = configManager.getString("staffjoin.change_message")
                            .replace("{player}", player.getUsername())
                            .replace("{server}", newServer)
                            .replace("{from}", previousServer);

                    Component changeMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                            .replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("{rank}")
                                    .replacement(prefix)
                                    .build());

                    server.getAllPlayers().forEach(target -> {
                        if (target.hasPermission("velocityutils.staffjoin.notify")) {
                            target.sendMessage(changeMessage);
                        }
                    });

                }
            }
        }
    }

    public void staffLeaveMessage(Player player) {
        if (configManager.getBoolean("staffjoin.enabled")) {
            if (player.hasPermission("velocityutils.staffjoin.staff")) {
                String prefixRaw = obtenerRango(player);

                Component prefix = deserializePrefix(prefixRaw);

                String format = configManager.getString("staffjoin.leave_message")
                        .replace("{player}", player.getUsername());

                Component leaveMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("{rank}")
                                .replacement(prefix)
                                .build());


                server.getAllPlayers().forEach(target -> {
                    if (target.hasPermission("velocityutils.staffjoin.notify")) {
                        target.sendMessage(leaveMessage);
                    }
                });
            }
        }
    }

    private Component deserializePrefix(String input) {
        // Si contiene <...> asumimos que es MiniMessage
        if (input.contains("<") && input.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                // En caso de error, usa como texto plano
                return Component.text(input);
            }
        }

        // Si no, asumimos que es con códigos &
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }

    private String obtenerRango(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        // Primero intentamos obtener el prefix del propio usuario (o el que LuckPerms determine como prioritario)
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix;
        }

        // Si no tiene prefix propio, usamos el del grupo principal
        String primaryGroupName = user.getPrimaryGroup();
        var group = luckPerms.getGroupManager().getGroup(primaryGroupName);
        if (group != null) {
            String groupPrefix = group.getCachedData().getMetaData().getPrefix();
            if (groupPrefix != null && !groupPrefix.isEmpty()) {
                return groupPrefix;
            }
            return primaryGroupName;
        }

        return primaryGroupName;
    }

    private void sendJoinWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("stafftime.discord_hook.join.url");
        String avatarUrl = configManager.getString("stafftime.discord_hook.join.avatar");
        String username = configManager.getString("stafftime.discord_hook.join.username");
        String title = configManager.getString("stafftime.discord_hook.join.title");

        String color = configManager.getString("stafftime.discord_hook.join.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
    private void sendChangeWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("stafftime.discord_hook.change.url");
        String avatarUrl = configManager.getString("stafftime.discord_hook.change.avatar");
        String username = configManager.getString("stafftime.discord_hook.change.username");
        String title = configManager.getString("stafftime.discord_hook.change.title");

        String color = configManager.getString("stafftime.discord_hook.change.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
    private void sendLeaveWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("stafftime.discord_hook.leave.url");
        String avatarUrl = configManager.getString("stafftime.discord_hook.leave.avatar");
        String username = configManager.getString("stafftime.discord_hook.leave.username");
        String title = configManager.getString("stafftime.discord_hook.leave.title");

        String color = configManager.getString("stafftime.discord_hook.leave.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
}
