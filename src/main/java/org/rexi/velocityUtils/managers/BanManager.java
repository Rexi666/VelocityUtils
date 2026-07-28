package org.rexi.velocityUtils.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.utils.BanData;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BanManager {

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final ProxyServer server;

    private final Map<String, BanData> banCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> subIpBanCache = new ConcurrentHashMap<>();

    public BanManager(ConfigManager configManager, DatabaseManager databaseManager, ProxyServer server) {
        this.configManager = configManager;
        this.databaseManager = databaseManager;
        this.server = server;
    }

    public BanData loadBan(String name, String ip) {
        try (Connection conn = databaseManager.getConnection()) {
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

        return configManager.legacy(joined);
    }
    public Component kickDenyMessage(String player, String kickedBy, String reason) {
        List<String> original = configManager.getStringList("ban_system.screen_messages.kick");

        String joined = String.join("\n", original)
                .replace("{player}", player)
                .replace("{kicked_by}", kickedBy)
                .replace("{reason}", reason);

        return configManager.legacy(joined);
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

        if (databaseManager.isUsingMySQL()) {
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

        try (Connection conn = databaseManager.getConnection();
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

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String name = banData.getName().toLowerCase();

            stmt.setString(1, name);
            stmt.executeUpdate();

            banCache.remove(name);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, BanData> getBanCache() {
        return banCache;
    }

    public Map<String, List<String>> getSubIpBanCache() {
        return subIpBanCache;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        // Comprobar ban
        if (configManager.getBoolean("ban_system.enabled")) {
            String playerName = event.getUsername().toLowerCase();
            BanData cached = getBanCache().get(playerName);
            if (cached != null) {
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(banDenyMessage(cached, event.getUsername())));
                Component message = configManager.getMessage("try_join_ban",
                        "{player}", event.getUsername(),
                        "{reason}", cached.getReason());
                server.getConsoleCommandSource().sendMessage(message);

                for (Player player : server.getAllPlayers()) {
                    if (player.hasPermission("velocityutils.bansystem.notify")) {
                        player.sendMessage(message);
                    }
                }
                return;
            }

            String ip = ((InetSocketAddress) event.getConnection()
                    .getRemoteAddress()).getAddress().getHostAddress();

            BanData ban = loadBan(playerName, ip);

            if (ban != null) {
                getBanCache().put(playerName, ban);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(banDenyMessage(ban, event.getUsername())));

                boolean ipBanFromOtherAccount = ban.getIpBan() && !ban.getName().equals(playerName);

                if (ipBanFromOtherAccount) {
                    String bannedName = ban.getName();
                    List<String> subIpBans = getSubIpBanCache().getOrDefault(bannedName, new ArrayList<>());
                    if (!subIpBans.contains(playerName)) {
                        subIpBans.add(playerName);
                    }
                    getSubIpBanCache().put(bannedName, subIpBans);

                    Component message = configManager.getMessage("try_join_banip",
                            "{player}", event.getUsername(),
                            "{ip_playername}", bannedName,
                            "{reason}", ban.getReason());
                    server.getConsoleCommandSource().sendMessage(message);

                    for (Player player : server.getAllPlayers()) {
                        if (player.hasPermission("velocityutils.bansystem.notify")) {
                            player.sendMessage(message);
                        }
                    }
                } else {
                    Component message = configManager.getMessage("try_join_ban",
                            "{player}", event.getUsername(),
                            "{reason}", ban.getReason());
                    server.getConsoleCommandSource().sendMessage(message);

                    for (Player player : server.getAllPlayers()) {
                        if (player.hasPermission("velocityutils.bansystem.notify")) {
                            player.sendMessage(message);
                        }
                    }
                }
            }
        }
    }
}
