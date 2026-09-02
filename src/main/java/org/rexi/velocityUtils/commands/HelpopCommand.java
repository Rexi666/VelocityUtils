package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.DiscordWebhook;

import java.util.*;

public class HelpopCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final DiscordWebhook webhook;
    private final VelocityUtils plugin;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 30 * 1000;

    public HelpopCommand (ConfigManager configManager, ProxyServer server, DiscordWebhook webhook, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.webhook = webhook;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        /* ──────────── 1. Permisos ──────────── */
        if (!source.hasPermission("velocityutils.helpop.use")) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (!(source instanceof Player player)) {
            source.sendMessage(configManager.getMessage("no_console"));
            return;
        }

        /* ──────────── 2. Sintaxis ──────────── */
        if (args.length < 1) {
            source.sendMessage(configManager.getMessage("helpop_usage"));
            return;
        }

        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 0, args.length));

        /* ──────────── 3. Jugador reportador ──────────── */
        String reportername = player.getUsername();
        String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessageString("server_unknown"));

        /* ──────────── 4. Cooldown ──────────── */

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid)) {
            long lastUsed = cooldowns.get(uuid);
            if (now - lastUsed < COOLDOWN_MILLIS) {
                long secondsLeft = (COOLDOWN_MILLIS - (now - lastUsed)) / 1000;
                source.sendMessage(configManager.getMessage("helpop_cooldown",
                        "{time}", String.valueOf(secondsLeft)));
                return;
            }
        }

        cooldowns.put(uuid, now);

        /* ──────────── 5. Preparar líneas del mensaje ──────────── */
        List<String> rawLines = configManager.getStringList("helpop.message");

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
            if (plugin.isPlayerInDisabledServer(online)) continue;

            for (String raw : rawLines) {
                String parsed = raw
                        .replace("{player}", reportername)
                        .replace("{reason}", reason)
                        .replace("{server}", serverName);

                if (parsed.startsWith("{center}")) {
                    parsed = parsed.replaceFirst("^\\{center\\}\\s*", "");
                    parsed = configManager.getCenteredMessage(parsed);
                }

                if (configManager.getBoolean("helpop.teleport_on_click")) {
                    Component tpLine = configManager.legacy(parsed)
                            .clickEvent(ClickEvent.runCommand("/goto " + reportername))
                            .hoverEvent(HoverEvent.showText(
                                    configManager.getMessage("helpop_hover")));
                    online.sendMessage(tpLine);
                } else {
                    online.sendMessage(configManager.legacy(parsed));
                }
            }
        }

        // Console
        for (String raw : rawLines) {
            String parsed = raw
                    .replace("{player}", reportername)
                    .replace("{reason}", reason)
                    .replace("{server}", serverName);

            parsed = parsed.replaceFirst("^\\{center\\}\\s*", "");

            server.getConsoleCommandSource().sendMessage(configManager.legacy(parsed));
        }

        /* ──────────── 7. Confirmación al reportador ──────────── */
        source.sendMessage(configManager.getMessage("helpop_sent"));
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
