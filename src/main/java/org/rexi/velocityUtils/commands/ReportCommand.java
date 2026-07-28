package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.DiscordWebhook;

import java.util.*;
import java.util.stream.Collectors;

public class ReportCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DiscordWebhook webhook;

    // Mapa para cooldowns: UUID -> timestamp del último uso
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 30 * 1000;

    public ReportCommand(ConfigManager configManager, ProxyServer server, DiscordWebhook webhook) {
        this.configManager = configManager;
        this.server = server;
        this.webhook = webhook;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        /* ──────────── 1. Permisos ──────────── */
        if (!source.hasPermission("velocityutils.report.use")) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (!(source instanceof Player player)) {
            source.sendMessage(configManager.getMessage("no_console"));
            return;
        }

        /* ──────────── 2. Sintaxis ──────────── */
        if (args.length < 2) {
            source.sendMessage(configManager.getMessage("report_usage"));
            return;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        /* ──────────── 3. Jugador reportado ──────────── */
        Optional<Player> targetOpt = server.getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            source.sendMessage(configManager.getMessage("report_player_not_found",
                    "{player}", targetName));
            return;
        }

        Player target = targetOpt.get();
        String reporterName = player.getUsername();

        if (targetName.equals(reporterName)) {
            source.sendMessage(configManager.getMessage("report_not_own"));
            return;
        }

        String serverName = target.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessageString("server_unknown"));

        /* ──────────── 4. Cooldown ──────────── */

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);
            if (now - lastUsed < COOLDOWN_MILLIS) {
                long secondsLeft = (COOLDOWN_MILLIS - (now - lastUsed)) / 1000;
                source.sendMessage(configManager.getMessage("report_cooldown",
                        "{time}", String.valueOf(secondsLeft)));
                return;
            }
        }

        cooldowns.put(uuid, now);

        /* ──────────── 5. Preparar líneas del mensaje ──────────── */
        List<String> rawLines = configManager.getStringList("report.message");

        if (configManager.getBoolean("report.discord_hook.enabled")) {
            String raw = configManager.getString("report.discord_hook.message");
            String msg = raw
                    .replace("{reported}", target.getUsername())
                    .replace("{reporter}", reporterName)
                    .replace("{reason}", reason)
                    .replace("{server}", serverName);
            sendReportWebhook(target.getUsername(), msg);
        }

        /* ──────────── 6. Enviar a moderadores ──────────── */
        for (Player online : server.getAllPlayers()) {
            if (!online.hasPermission("velocityutils.report.see")) continue;

            for (String raw : rawLines) {
                String parsed = raw
                        .replace("{player}", reporterName)
                        .replace("{reported}", target.getUsername())
                        .replace("{reason}", reason)
                        .replace("{server}", serverName);

                if (parsed.startsWith("{center}")) {
                    parsed = parsed.replaceFirst("^\\{center\\}\\s*", "");
                    parsed = configManager.getCenteredMessage(parsed);
                }

                if (configManager.getBoolean("report.teleport_on_click")) {
                    Component tpLine = configManager.legacy(parsed)
                            .clickEvent(ClickEvent.runCommand("/goto " + targetName))
                            .hoverEvent(HoverEvent.showText(configManager.getMessage("report_hover")));
                    online.sendMessage(tpLine);
                } else {
                    online.sendMessage(configManager.legacy(parsed));
                }
            }
        }

        // Console
        for (String raw : rawLines) {
            String parsed = raw
                    .replace("{player}", reporterName)
                    .replace("{reported}", target.getUsername())
                    .replace("{reason}", reason)
                    .replace("{server}", serverName);

            parsed = parsed.replaceFirst("^\\{center\\}\\s*", "");

            server.getConsoleCommandSource().sendMessage(configManager.legacy(parsed));
        }

        /* ──────────── 7. Confirmación al reportador ──────────── */
        source.sendMessage(configManager.getMessage("report_sent",
                "{target}", targetName));
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

    private void sendReportWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("report.discord_hook.url");
        String avatarUrl = configManager.getString("report.discord_hook.avatar");
        String username = configManager.getString("report.discord_hook.username");
        String title = configManager.getString("report.discord_hook.title");

        String color = configManager.getString("report.discord_hook.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
}
