package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class FindCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public FindCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Verificar permiso opcional
        if (!source.hasPermission("velocityutils.find")) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        if (args.length != 1) {
            String find_usage = configManager.getMessage("find_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(find_usage));
            return;
        }

        String targetName = args[0];
        Optional<Player> target = server.getPlayer(targetName);

        // 🔹 Si está online → mostrar el servidor actual
        if (target.isPresent()) {
            Player player = target.get();
            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            String find_where = configManager.getMessage("find_where");
            find_where = find_where.replace("{player}", player.getUsername());
            find_where = find_where.replace("{server}", serverName);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(find_where));
            return;
        }

        // 🔹 Si no está online → buscar en la DB
        try (Connection conn = plugin.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_join FROM player_info WHERE name = ?")) {
            stmt.setString(1, targetName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String lastJoinStr = rs.getString("last_join");
                if (lastJoinStr != null) {
                    Instant lastJoinInstant;
                    try {
                        // MySQL
                        lastJoinInstant = rs.getTimestamp("last_join", Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant();
                    } catch (SQLException e) {
                        // SQLite (texto plano)
                        lastJoinStr = rs.getString("last_join");
                        LocalDateTime lastJoin = LocalDateTime.parse(lastJoinStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        lastJoinInstant = lastJoin.toInstant(ZoneOffset.UTC);
                    }
                    Duration diff = Duration.between(lastJoinInstant, Instant.now());

                    String formatted = formatDuration(diff);

                    String message = configManager.getMessage("find_last_seen");
                    message = message.replace("{player}", targetName);
                    message = message.replace("{time}", formatted);

                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
                } else {
                    String not_found = configManager.getMessage("find_player_not_found")
                            .replace("{player}", targetName);
                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(not_found));
                }
            } else {
                String not_found = configManager.getMessage("find_player_not_found")
                        .replace("{player}", targetName);
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(not_found));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cError al consultar la base de datos."));
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        String day_simbol = configManager.getMessage("day_simbol");
        String hour_simbol = configManager.getMessage("hour_simbol");
        String minute_simbol = configManager.getMessage("minute_simbol");

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(day_simbol).append(" ");
        if (hours > 0) sb.append(hours).append(hour_simbol).append(" ");
        if (minutes > 0) sb.append(minutes).append(minute_simbol).append(" ");

        if (sb.length() == 0) {
            return configManager.getMessage("find_less_minute");
        }
        return sb.toString().trim();
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            // No se ha escrito nada aún, sugerimos todos los jugadores
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .collect(Collectors.toList());
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
