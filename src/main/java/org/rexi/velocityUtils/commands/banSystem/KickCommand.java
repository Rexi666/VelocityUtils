package org.rexi.velocityUtils.commands.banSystem;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class KickCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public KickCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) { // vkick <player> [reason]
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (!(source.hasPermission("velocityutils.bansystem.vkick"))) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("usage_kick"));
            return;
        }

        String targetName = args[0].toLowerCase();
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : configManager.getString("ban_system.default_ban_reason");
        String fromName = source instanceof Player ? ((Player) source).getUsername() : configManager.getString("ban_system.console");

        if (!server.getPlayer(targetName).isPresent()) {
            source.sendMessage(configManager.getMessage("not_connected",
                    "{player}", targetName));
            return;
        }

        server.getPlayer(targetName).ifPresent(player ->
                player.disconnect(plugin.kickDenyMessage(player.getUsername(), fromName, reason))
        );

        source.sendMessage(configManager.getMessage("kick_success",
                "{player}", targetName,
                "{reason}", reason));

        Component finalMessage = configManager.getMessage("kick_notify",
                "{player}", targetName,
                "{reason}", reason,
                "{kicked_by}", fromName);
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
