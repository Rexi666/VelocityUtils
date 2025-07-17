package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.StaffSession;
import org.rexi.velocityUtils.VelocityUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StaffTimeCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public StaffTimeCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocityutils.stafftime.use")) {
            source.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(legacy(configManager.getMessage("stafftime_usage")));
            return;
        }

        String targetName = args[0];
        String period = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";

        UUID uuid;
        try (Connection conn = plugin.getConnection()) {
            Optional<Player> targetOpt = server.getPlayer(targetName);

            if (targetOpt.isPresent()) {
                Player target = targetOpt.get();
                uuid = target.getUniqueId();
            } else {
                uuid = getUUIDFromDatabase(conn, targetName);
                if (uuid == null) {
                    String notFound = configManager.getMessage("stafftime_not_found").replace("{player}", targetName);
                    source.sendMessage(legacy(notFound));
                    return;
                }
            }

            // Obtener el tiempo ya registrado en base de datos
            long daySeconds = getSecondsForDay(conn, uuid, LocalDate.now());
            long weekSeconds = getSecondsForWeek(conn, uuid, LocalDate.now());
            long monthSeconds = getSecondsForMonth(conn, uuid, LocalDate.now());

            // Si el jugador está online, sumamos el tiempo que lleva conectado en esta sesión
            if (targetOpt.isPresent()) {
                Player target = targetOpt.get();
                StaffSession session = plugin.getStaffSessions().get(uuid);  // Asumo que tienes acceso a las sesiones aquí
                if (session != null) {
                    Duration connectedDuration = Duration.between(session.getStartTime(), Instant.now());
                    long connectedSeconds = connectedDuration.getSeconds();

                    // Sumamos ese tiempo solo para mostrar, no lo guardamos en BD aquí
                    daySeconds += connectedSeconds;
                    weekSeconds += connectedSeconds;
                    monthSeconds += connectedSeconds;
                }
            }

            if (period.isEmpty()) {
                List<String> lines = configManager.getStringList("stafftime.command.no_type");
                for (String line : lines) {
                    String parsed = line
                            .replace("{player}", targetName)
                            .replace("{day}", formatSeconds(daySeconds))
                            .replace("{week}", formatSeconds(weekSeconds))
                            .replace("{month}", formatSeconds(monthSeconds));
                    source.sendMessage(legacy(parsed));
                }
            } else {
                long seconds;
                String typeLabel;

                switch (period) {
                    case "day":
                        seconds = daySeconds;
                        typeLabel = configManager.getString("stafftime.command.day");
                        break;
                    case "week":
                        seconds = weekSeconds;
                        typeLabel = configManager.getString("stafftime.command.week");
                        break;
                    case "month":
                        seconds = monthSeconds;
                        typeLabel = configManager.getString("stafftime.command.month");
                        break;
                    default:
                        source.sendMessage(legacy(configManager.getMessage("stafftime_invalid_type")));
                        return;
                }

                List<String> lines = configManager.getStringList("stafftime.command.type");
                for (String line : lines) {
                    String parsed = line
                            .replace("{player}", targetName)
                            .replace("{type}", typeLabel)
                            .replace("{time}", formatSeconds(seconds));
                    source.sendMessage(legacy(parsed));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(legacy("&cError trying to reach database."));
        }
    }


    private long getSecondsForDay(Connection conn, UUID uuid, LocalDate day) throws SQLException {
        String sql = "SELECT duration_seconds FROM staff_time_daily WHERE uuid = ? AND date = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, day.format(DateTimeFormatter.ISO_DATE));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("duration_seconds") : 0;
            }
        }
    }

    private long getSecondsForWeek(Connection conn, UUID uuid, LocalDate date) throws SQLException {
        LocalDate startOfWeek = date.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        String sql = "SELECT SUM(duration_seconds) AS total FROM staff_time_daily WHERE uuid = ? AND date BETWEEN ? AND ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, startOfWeek.format(DateTimeFormatter.ISO_DATE));
            ps.setString(3, endOfWeek.format(DateTimeFormatter.ISO_DATE));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("total") : 0;
            }
        }
    }

    private long getSecondsForMonth(Connection conn, UUID uuid, LocalDate date) throws SQLException {
        LocalDate firstDay = date.withDayOfMonth(1);
        LocalDate lastDay = date.withDayOfMonth(date.lengthOfMonth());

        String sql = "SELECT SUM(duration_seconds) AS total FROM staff_time_daily WHERE uuid = ? AND date BETWEEN ? AND ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, firstDay.format(DateTimeFormatter.ISO_DATE));
            ps.setString(3, lastDay.format(DateTimeFormatter.ISO_DATE));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("total") : 0;
            }
        }
    }

    private String formatSeconds(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02dh %02dm %02ds", h, m, s);
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 2) {
            return List.of("day", "week", "month");
        }
        return List.of();
    }

    private UUID getUUIDFromDatabase(Connection conn, String playerName) throws SQLException {
        String sql = "SELECT uuid FROM player_info WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        }
        return null;
    }
}