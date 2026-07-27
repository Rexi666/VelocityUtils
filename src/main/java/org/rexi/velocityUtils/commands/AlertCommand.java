package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.time.Duration;
import java.util.List;

public class AlertCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final VelocityUtils plugin;

    public AlertCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.server = server;
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Verifica si el usuario tiene permiso
        if (!source.hasPermission("velocityutils.alert") && !(source instanceof ConsoleCommandSource)) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        // Verifica si hay argumentos (evita enviar solo el prefix)
        if (invocation.arguments().length == 0) {
            source.sendMessage(configManager.getMessage("alert_usage"));
            return;
        }

        // Obtiene el mensaje y el prefijo
        String message = String.join(" ", invocation.arguments());
        sendAlert(message);
    }

    public void sendAlert(String message) {
        List<String> alertLines = configManager.getStringList("alert.message");
        String soundName = configManager.getString("alert.sound");

        boolean titleEnabled = configManager.getBoolean("alert.title.enabled");
        boolean actionBarEnabled = configManager.getBoolean("alert.actionbar.enabled");
        boolean bossBarEnabled = configManager.getBoolean("alert.bossbar.enabled");

        String titleText = configManager.getString("alert.title.title");
        String subtitleText = configManager.getString("alert.title.subtitle");
        int fade_in = configManager.getInt("alert.title.durations.fade_in");
        int stay = configManager.getInt("alert.title.durations.stay");
        int fade_out = configManager.getInt("alert.title.durations.fade_out");

        String actionBarText = configManager.getString("alert.actionbar.message");

        String bossBarText = configManager.getString("alert.bossbar.message");
        String bossBarColor = configManager.getString("alert.bossbar.color");
        String bossBarOverlay = configManager.getString("alert.bossbar.overlay");
        int bossBarDuration = configManager.getInt("alert.bossbar.duration");

        // Title

        Title title = Title.title(
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        titleText.replace("{message}", message)),
                LegacyComponentSerializer.legacyAmpersand().deserialize(
                        subtitleText.replace("{message}", message)),
                Title.Times.times(
                        Duration.ofMillis(fade_in * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fade_out * 50L)
                )
        );

        // ActionBar

        Component actionBar = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(actionBarText.replace("{message}", message));

        // BossBar
        final BossBar.Color color = parseColor(bossBarColor);
                /*
                PINK
                BLUE
                RED
                GREEN
                YELLOW
                PURPLE
                WHITE
                 */
        final BossBar.Overlay overlay = parseOverlay(bossBarOverlay);
                /*
                PROGRESS
                NOTCHED_6
                NOTCHED_10
                NOTCHED_12
                NOTCHED_20
                 */

        server.getAllPlayers().forEach(player -> {

            // Chat
            for (String line : alertLines) {
                line = line.replace("{message}", message);

                if (line.startsWith("{center}")) {
                    line = line.replaceFirst("^\\{center\\}\\s*", "");
                    line = plugin.getCenteredMessage(line);
                }

                player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(line));
            }

            // Title
            if (titleEnabled) {
                player.showTitle(title);
            }

            // ActionBar
            if (actionBarEnabled) {
                player.sendActionBar(actionBar);
            }

            // BossBar
            if (bossBarEnabled) {
                BossBar bossBar = BossBar.bossBar(
                        LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(bossBarText.replace("{message}", message)),
                        1.0f,
                        color,
                        overlay
                );

                player.showBossBar(bossBar);

                server.getScheduler()
                        .buildTask(plugin, () -> player.hideBossBar(bossBar))
                        .delay(Duration.ofSeconds(bossBarDuration))
                        .schedule();
            }
        });

        // Sonido
        if (soundName != null && !soundName.isEmpty()) {
            plugin.sendSoundToAll(soundName);
        }

        // Consola
        for (String line : alertLines) {
            line = line.replace("{message}", message);
            line = line.replaceFirst("^\\{center\\}\\s*", "");

            server.getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(line));
        }
    }

    private BossBar.Color parseColor(String color) {
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay parseOverlay(String overlay) {
        try {
            return BossBar.Overlay.valueOf(overlay.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}