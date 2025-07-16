package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import org.rexi.velocityUtils.*;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.rexi.velocityUtils.DiscordWebhook.getUuidFromName;

public class StaffConnectionListener {

    private final ConfigManager configManager;
    private final VelocityUtils plugin;
    private final Map<UUID, StaffSession> sessions;
    private final DiscordWebhook staffJoinWebhook;
    private final DiscordWebhook staffChangeWebhook;
    private final DiscordWebhook staffLeaveWebhook;

    public StaffConnectionListener(VelocityUtils plugin, Map<UUID, StaffSession> sessions, ConfigManager configManager, DiscordWebhook staffJoinWebhook,
                                   DiscordWebhook staffChangeWebhook, DiscordWebhook staffLeaveWebhook) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.configManager = new ConfigManager();
        this.staffJoinWebhook = staffJoinWebhook;
        this.staffChangeWebhook = staffChangeWebhook;
        this.staffLeaveWebhook = staffLeaveWebhook;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        if (isAdmin(player)) {
            return; // No action for admins
        }
        if (isStaff(player)) {
            StaffSession session = sessions.get(player.getUniqueId());

            String newServer = event.getServer().getServerInfo().getName();  // Servidor al que acaba de conectar

            if (session == null) {
                // Primera vez que detectamos al jugador, creamos sesión con el servidor actual
                sessions.put(player.getUniqueId(), new StaffSession(Instant.now(), newServer));

                String uuid = getUuidFromName(player.getUsername());
                String avatar = (uuid != null)
                        ? "https://minotar.net/helm/" + uuid + "/64.png"
                        : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";
                if (staffJoinWebhook != null && configManager.getBoolean("stafftime.enabled") && configManager.getBoolean("stafftime.discord_hook.join.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.join.message");
                    String msg = raw.replace("{player}", player.getUsername());
                    staffJoinWebhook.send(msg, avatar);
                }
            } else {
                // Sesión ya existente, hacemos switch de servidor
                session.switchServer(newServer);

                String previousServer = event.getPreviousServer()
                        .map(srv -> srv.getServerInfo().getName())
                        .orElse("N/A");

                String uuid = getUuidFromName(player.getUsername());
                String avatar = (uuid != null)
                        ? "https://minotar.net/helm/" + uuid + "/64.png"
                        : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";
                if (staffChangeWebhook != null && configManager.getBoolean("stafftime.enabled") && configManager.getBoolean("stafftime.discord_hook.change.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.change.message");
                    String msg = raw
                            .replace("{player}", player.getUsername())
                            .replace("{from}", previousServer)
                            .replace("{to}", newServer);
                    staffChangeWebhook.send(msg, avatar);
                }
            }
        }
    }



    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
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
                Duration weekly = getDurationForRange(player.getUniqueId(), DateUtils.getStartOfWeek(), DateUtils.getEndOfWeek());
                Duration monthly = getDurationForRange(player.getUniqueId(), DateUtils.getStartOfMonth(), DateUtils.getEndOfMonth());

                if (staffLeaveWebhook != null && configManager.getBoolean("stafftime.enabled") && configManager.getBoolean("stafftime.discord_hook.leave.enabled")) {
                    String raw = configManager.getString("stafftime.discord_hook.leave.message");
                    String serverTimeFormat = configManager.getString("stafftime.discord_hook.leave.serverstime");

                    StringBuilder serverTimes = new StringBuilder();
                    session.getTimePerServer().forEach((server, duration) -> {
                        String formatted = serverTimeFormat
                                .replace("{server}", server)
                                .replace("{time}", formatDuration(duration));
                        serverTimes.append(formatted).append("\n");
                    });

                    String uuid = getUuidFromName(player.getUsername());
                    String avatar = (uuid != null)
                            ? "https://minotar.net/helm/" + uuid + "/64.png"
                            : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";

                    String msg = raw
                            .replace("{player}", player.getUsername())
                            .replace("{time}", formatDuration(session.getTotalTime()))
                            .replace("{time_daily}", formatDuration(daily))
                            .replace("{time_weekly}", formatDuration(weekly))
                            .replace("{time_monthly}", formatDuration(monthly))
                            .replace("{serverstime}", serverTimes.toString().trim());
                    staffLeaveWebhook.send(msg, avatar);
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

    private String getCurrentServerName(Player player) {
        return player.getCurrentServer()
                .map(srv -> srv.getServerInfo().getName())
                .orElse(configManager.getMessage("server_unknown"));
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public void saveSessionDurationDaily(UUID uuid, LocalDate date, Duration duration) {
        String sql = """
        INSERT INTO staff_time_daily (uuid, date, duration_seconds)
        VALUES (?, ?, ?)
        ON CONFLICT(uuid, date) DO UPDATE SET
        duration_seconds = duration_seconds + excluded.duration_seconds;
        """;
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


}
