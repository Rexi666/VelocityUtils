package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
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
    private final VelocityUtils plugin;

    public BanIpCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vbanip <player> [reason]
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vbanip"))) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("no_permission")));
            return;
        }

        if (args.length < 1) {
            String usage = configManager.getMessage("usage_banip");
            source.sendMessage(deserializeLegacy(usage));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = plugin.loadBan(targetName, null);

        if (checkban != null) {
            String already_banned = configManager.getMessage("already_banned");
            already_banned = already_banned.replace("{player}", targetName);
            source.sendMessage(deserializeLegacy(already_banned));
            return;
        }

        Optional<Player> target = server.getPlayer(targetName);

        String playerIp;
        if (target.isPresent()) {
            playerIp = target.get().getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
        } else {
            try (Connection conn = plugin.getConnection();
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
                source.sendMessage(deserializeLegacy("&cError trying to reach database."));
            }
        }

        if (playerIp == null) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("not_ip_registered")
                    .replace("{player}", targetName)));
            return;
        }

        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : configManager.getString("ban_system.default_ban_reason");

        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        BanData banData = new BanData(targetName, playerIp, true, fromName, java.time.Instant.now(), reason);

        plugin.saveBan(banData);

        target.ifPresent(targetPlayer -> {
            String ip = targetPlayer.getRemoteAddress().getAddress().getHostAddress();

            server.getAllPlayers().forEach(player -> {
                String ipPlayer = player.getRemoteAddress().getAddress().getHostAddress();

                if (ipPlayer.equals(ip)) {
                    player.disconnect(plugin.banDenyMessage(banData, player.getUsername()));
                }
            });
        });

        source.sendMessage(deserializeLegacy(configManager.getMessage("banip_success")
                .replace("{player}", targetName).replace("{reason}", reason)));

        String message = configManager.getMessage("banip_notify");
        message = message.replace("{player}", targetName)
                .replace("{reason}", reason)
                .replace("{banned_by}", fromName);
        Component finalMessage = deserializeLegacy(message);
        server.getConsoleCommandSource().sendMessage(finalMessage);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("velocityutils.bansystem.notify")) {
                player.sendMessage(finalMessage);
            }
        }
    }

    private Component deserializeLegacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
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
