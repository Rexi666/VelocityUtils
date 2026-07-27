package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.*;

public class StreamCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final LuckPerms luckPerms;
    private final VelocityUtils plugin;

    public StreamCommand(ConfigManager configManager, ProxyServer server, LuckPerms luckPerms, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
        this.plugin = plugin;
    }

    private final Map<UUID, Long> streamCooldowns = new HashMap<>();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(configManager.getMessage("no_console"));
            return;
        }

        if (!player.hasPermission("velocityutils.stream")) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = configManager.getInt("stream.cooldown_seconds") * 1000L;
        Long lastUse = streamCooldowns.get(player.getUniqueId());

        if (lastUse != null && now - lastUse < cooldown) {
            long remaining = (cooldown - (now - lastUse)) / 1000; // en segundos
            long minutes = remaining / 60;
            long seconds = remaining % 60;

            String minute_simbol = configManager.getMessageString("minute_simbol");
            String second_simbol = configManager.getMessageString("second_simbol");

            String cooldownFormatted;
            if (minutes > 0) {
                cooldownFormatted = minutes + minute_simbol + " " + seconds + second_simbol;
            } else {
                cooldownFormatted = seconds + second_simbol;
            }

            player.sendMessage(configManager.getMessage("stream_cooldown",
                    "{cooldown}", cooldownFormatted));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(configManager.getMessage("stream_usage"));
            return;
        }

        String url = args[0];
        if (configManager.getBoolean("stream.whitelist")) {
            List<String> whitelistLinks = configManager.getStringList("stream.whitelist_links");
            boolean whitelisted = false;
            for (String link : whitelistLinks) {
                if (url.startsWith(link)) {
                    whitelisted = true;
                    break;
                }
            }
            if (whitelisted) {
                streamCooldowns.put(player.getUniqueId(), now);
                sendMessage(player, url);
            } else {
                player.sendMessage(configManager.getMessage("stream_invalid_url"));
            }
        } else {
            sendMessage(player, url);
            streamCooldowns.put(player.getUniqueId(), now);
        }
    }

    private static final LegacyComponentSerializer LEGACY_HEX_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors() // Habilita el soporte de hex
            .useUnusualXRepeatedCharacterHexFormat() // Soporta &x&r&r&g&g&b&b
            .build();
    private Component legacy(String s) {
        return LEGACY_HEX_SERIALIZER.deserialize(s);
    }

    private void sendMessage(Player player, String url) {
        String rangoRaw = obtenerRango(player);

        Component rankComponent = deserializePrefix(rangoRaw);

        String rankPlain = LegacyComponentSerializer.legacySection().serialize(rankComponent);
        rankPlain = MiniMessage.miniMessage().stripTags(rankPlain);

        List<String> messageLines = configManager.getStringList("stream.message");
        boolean hoverEnabled = configManager.getBoolean("stream.hover_enabled");

        Component hoverComponent = hoverEnabled
                ? legacy(configManager.getString("stream.hover"))
                : null;

        Collection<Player> players = server.getAllPlayers();

        for (String line : messageLines) {
            line = line.replace("{player}", player.getUsername())
                    .replace("{url}", url)
                    .replace("{rank}", rankPlain);

            if (line.startsWith("{center}")) {
                line = line.replaceFirst("^\\{center\\}\\s*", "");
                line = plugin.getCenteredMessage(line);
            }

            Component base = legacy(line);

            base = base.replaceText(TextReplacementConfig.builder()
                    .matchLiteral(rankPlain)
                    .replacement(rankComponent)
                    .build());

            Component finalMessage;

            if (hoverEnabled) {
                finalMessage = base
                        .clickEvent(ClickEvent.openUrl(url))
                        .hoverEvent(HoverEvent.showText(hoverComponent));
            } else {
                finalMessage = base.clickEvent(ClickEvent.openUrl(url));
            }

            for (Player onlinePlayer : players) {
                onlinePlayer.sendMessage(finalMessage);
            }
        }

        // Console
        for (String line : messageLines) {
            line = line.replace("{player}", player.getUsername())
                    .replace("{url}", url)
                    .replace("{rank}", rankPlain);
            line = line.replaceFirst("^\\{center\\}\\s*", "");
            server.getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(line));
        }
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
        return LEGACY_HEX_SERIALIZER.deserialize(input);
    }
}
