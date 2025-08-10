package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;

import java.util.*;

public class HelpopCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DiscordWebhook webhook;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 30 * 1000;

    public HelpopCommand (ConfigManager configManager, ProxyServer server, DiscordWebhook webhook) {
        this.configManager = configManager;
        this.server = server;
        this.webhook = webhook;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        /* ──────────── 1. Permisos ──────────── */
        if (!source.hasPermission("velocityutils.helpop.use")) {
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
        if (args.length < 1) {
            String report_usage = configManager.getMessage("helpop_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(report_usage));
            return;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 0, args.length));

        /* ──────────── 3. Jugador reportador ──────────── */
        Player player = (Player) source;
        String reportername = player.getUsername();
        String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessage("server_unknown"));

        /* ──────────── 4. Cooldown ──────────── */

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);
            if (now - lastUsed < COOLDOWN_MILLIS) {
                long secondsLeft = (COOLDOWN_MILLIS - (now - lastUsed)) / 1000;
                String helpop_cooldown = configManager.getMessage("helpop_cooldown");
                helpop_cooldown = helpop_cooldown.replace("{time}", String.valueOf(secondsLeft));
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(helpop_cooldown));
                return;
            }
        }

        cooldowns.put(uuid, now);

        /* ──────────── 5. Preparar líneas del mensaje ──────────── */
        List<String> rawLines = configManager.getStringList("helpop.message");
        if (rawLines == null || rawLines.isEmpty()) {
            // Fallback por si el usuario borra la sección
            rawLines = List.of(
                    "&f-----------------------------",
                    "&eNew Help Request from {player}!",
                    "&fReason: &b{reason}",
                    "&fServer: &b{server}",
                    "&eClick to teleport",
                    "&f-----------------------------"
            );
        }

        if (configManager.getBoolean("helpop.discord_hook.enabled")) {
            String raw = configManager.getString("helpop.discord_hook.message");
            String msg = raw
                    .replace("{player}", reportername)
                    .replace("{reason}", reason)
                    .replace("{server}", serverName);
            sendHelpopWebhook(reportername, msg);
        }

        /* ──────────── 6. Enviar a moderadores ──────────── */
        for (Player online : server.getAllPlayers()) {
            if (!online.hasPermission("velocityutils.helpop.see")) continue;

            for (String raw : rawLines) {
                String parsed = raw
                        .replace("{player}", reportername)
                        .replace("{reason}", reason)
                        .replace("{server}", serverName);

                if (configManager.getBoolean("helpop.teleport_on_click")) {
                    String helpop_hover = configManager.getMessage("helpop_hover");
                    Component tpLine = legacy(parsed)
                            .clickEvent(ClickEvent.runCommand("/goto " + reportername))
                            .hoverEvent(HoverEvent.showText(
                                    LegacyComponentSerializer.legacyAmpersand().deserialize(helpop_hover)));
                    online.sendMessage(tpLine);
                } else {
                    online.sendMessage(legacy(parsed));
                }
            }
        }

        /* ──────────── 7. Confirmación al reportador ──────────── */
        String helpop_sent = configManager.getMessage("helpop_sent");
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(helpop_sent));
    }

    /* Utilidad para traducir códigos & */
    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    private void sendHelpopWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("helpop.discord_hook.url");
        String avatarUrl = configManager.getString("helpop.discord_hook.avatar");
        String username = configManager.getString("helpop.discord_hook.username");
        String title = configManager.getString("helpop.discord_hook.title");

        String color = configManager.getString("helpop.discord_hook.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
}
