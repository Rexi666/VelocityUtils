package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;

import java.util.*;
import java.util.stream.Collectors;

import static org.rexi.velocityUtils.DiscordWebhook.getUuidFromName;

public class ReportCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DiscordWebhook reportWebhook;

    // Mapa para cooldowns: UUID -> timestamp del último uso
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 30 * 1000;

    public ReportCommand(ConfigManager configManager, ProxyServer server, DiscordWebhook reportWebhook) {
        this.configManager = configManager;
        this.server = server;
        this.reportWebhook = reportWebhook;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        /* ──────────── 1. Permisos ──────────── */
        if (!source.hasPermission("velocityutils.report.use")) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        if (!(source instanceof Player)) {
            String no_console = configManager.getMessage("no_console");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_console));
            return;
        }

        /* ──────────── 2. Sintaxis ──────────── */
        if (args.length < 2) {
            String report_usage = configManager.getMessage("report_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(report_usage));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        /* ──────────── 3. Jugador reportado ──────────── */
        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            String report_player_not_found = configManager.getMessage("report_player_not_found");
            report_player_not_found = report_player_not_found.replace("{player}", targetName);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(report_player_not_found));
            return;
        }

        Player target = targetOpt.get();
        String reporterName = (source instanceof Player p) ? p.getUsername() : "Console";
        String serverName = target.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown");

        /* ──────────── 4. Cooldown ──────────── */

        if (source instanceof Player player) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            if (cooldowns.containsKey(uuid)) {
                long lastUsed = cooldowns.get(uuid);
                if (now - lastUsed < COOLDOWN_MILLIS) {
                    long secondsLeft = (COOLDOWN_MILLIS - (now - lastUsed)) / 1000;
                    String report_cooldown = configManager.getMessage("report_cooldown");
                    report_cooldown = report_cooldown.replace("{time}", String.valueOf(secondsLeft));
                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(report_cooldown));
                    return;
                }
            }

            cooldowns.put(uuid, now);
        }

        /* ──────────── 5. Preparar líneas del mensaje ──────────── */
        List<String> rawLines = configManager.getStringList("report.message");
        if (rawLines == null || rawLines.isEmpty()) {
            // Fallback por si el usuario borra la sección
            rawLines = List.of(
                    "&f-----------------------------",
                    "&eNew Report from {player}!",
                    "&fReported: &c{reported}",
                    "&fReason: &b{reason}",
                    "&eClick to teleport",
                    "&f-----------------------------"
            );
        }

        String uuid = getUuidFromName(target.getUsername());
        String avatar = (uuid != null)
                ? "https://minotar.net/helm/" + uuid + "/64.png"
                : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";

        if (reportWebhook != null && configManager.getBoolean("report.discord_hook.enabled")) {
            String raw = configManager.getString("report.discord_hook.message");
            String msg = raw
                    .replace("{reported}", target.getUsername())
                    .replace("{reporter}", reporterName)
                    .replace("{reason}", reason)
                    .replace("{server}", serverName);
            reportWebhook.send(msg, avatar);
        }

        /* ──────────── 6. Enviar a moderadores ──────────── */
        for (Player online : server.getAllPlayers()) {
            if (!online.hasPermission("velocityutils.report.see")) continue;

            for (String raw : rawLines) {
                String parsed = raw
                        .replace("{player}", reporterName)
                        .replace("{reported}", target.getUsername())
                        .replace("{reason}", reason);

                if (configManager.getBoolean("report.teleport_on_click")) {
                    String report_hover = configManager.getMessage("report_hover");
                    Component tpLine = legacy(parsed)
                            .clickEvent(ClickEvent.runCommand("/goto " + targetName))
                            .hoverEvent(HoverEvent.showText(
                                    LegacyComponentSerializer.legacyAmpersand().deserialize(report_hover)));;
                    online.sendMessage(tpLine);
                } else {
                    online.sendMessage(legacy(parsed));
                }
            }
        }

        /* ──────────── 7. Confirmación al reportador ──────────── */
        String report_sent = configManager.getMessage("report_sent");
        report_sent = report_sent.replace("{target}", targetName);
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(report_sent));
    }

    /* Utilidad para traducir códigos & */
    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
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
