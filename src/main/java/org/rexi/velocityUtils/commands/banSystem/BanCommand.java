package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.managers.BanManager;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.BanData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BanCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final BanManager banManager;

    public BanCommand(ConfigManager configManager, ProxyServer server, BanManager banManager) {
        this.configManager = configManager;
        this.server = server;
        this.banManager = banManager;
    }

    @Override
    public void execute(Invocation invocation) { // vban <player> [reason]
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vban"))) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("usage_ban"));
            return;
        }

        String targetName = args[0].toLowerCase();

        BanData checkban = banManager.loadBan(targetName, null);

        if (checkban != null) {
            source.sendMessage(configManager.getMessage("already_banned",
                    "{player}", targetName));
            return;
        }

        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : configManager.getString("ban_system.default_ban_reason");

        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        BanData banData = new BanData(targetName, null, false, fromName, java.time.Instant.now(), reason);

        banManager.saveBan(banData);

        server.getPlayer(targetName).ifPresent(player ->
                player.disconnect(banManager.banDenyMessage(banData, player.getUsername()))
        );

        source.sendMessage(configManager.getMessage("ban_success",
                        "{player}", targetName,
                        "{reason}", reason));

        Component finalMessage = configManager.getMessage("ban_notify",
                "{player}", targetName,
                "{reason}", reason,
                "{banned_by}", fromName);
        server.getConsoleCommandSource().sendMessage(finalMessage);
        for (Player player : server.getAllPlayers()) {
            if (player.hasPermission("velocityutils.bansystem.notify")) {
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
