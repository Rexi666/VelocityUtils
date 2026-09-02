package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.BanManager;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.managers.DatabaseManager;
import org.rexi.velocityUtils.utils.BanData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class BanIpCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DatabaseManager databaseManager;
    private final BanManager banManager;
    private final VelocityUtils plugin;

    public BanIpCommand(ConfigManager configManager, ProxyServer server, DatabaseManager databaseManager, BanManager banManager, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.databaseManager = databaseManager;
        this.banManager = banManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vbanip <player> [reason]
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vbanip"))) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (source instanceof Player p && plugin.isPlayerInDisabledServer(p)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("usage_banip"));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = banManager.loadBan(targetName, null);

        if (checkban != null) {
            source.sendMessage(configManager.getMessage("already_banned",
                    "{player}", targetName));
            return;
        }

        Optional<Player> target = server.getPlayer(targetName);

        String playerIp;
        if (target.isPresent()) {
            playerIp = target.get().getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
        } else {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT player_ip FROM player_info WHERE LOWER(name) = LOWER(?)")) {
                stmt.setString(1, targetName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    playerIp = rs.getString("player_ip");
                } else {
                    playerIp = null;
                }
            } catch (SQLException e) {
                playerIp = null;
                e.printStackTrace();
                source.sendMessage(configManager.legacy("&cError trying to reach database."));
            }
        }

        if (playerIp == null) {
            source.sendMessage(configManager.getMessage("not_ip_registered",
                    "{player}", targetName));
            return;
        }

        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : configManager.getString("ban_system.default_ban_reason");

        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        BanData banData = new BanData(targetName, playerIp, true, fromName, java.time.Instant.now(), reason);

        banManager.saveBan(banData);

        target.ifPresent(targetPlayer -> {
            String ip = targetPlayer.getRemoteAddress().getAddress().getHostAddress();

            server.getAllPlayers().forEach(player -> {
                String ipPlayer = player.getRemoteAddress().getAddress().getHostAddress();

                if (ipPlayer.equals(ip)) {
                    player.disconnect(banManager.banDenyMessage(banData, player.getUsername()));
                }
            });
        });

        source.sendMessage(configManager.getMessage("banip_success",
                "{player}", targetName,
                "{reason}", reason));

        Component finalMessage = configManager.getMessage("banip_notify",
                "{player}", targetName,
                "{reason}", reason,
                "{banned_by}", fromName);
        server.getConsoleCommandSource().sendMessage(finalMessage);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("velocityutils.bansystem.notify") && !plugin.isPlayerInDisabledServer(player)) {
                player.sendMessage(finalMessage);
            }
        }
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
