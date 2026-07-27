package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.time.Duration;
import java.util.List;

public class ServerWhitelistListener {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final VelocityUtils plugin;

    public ServerWhitelistListener(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!configManager.getBoolean("serverwhitelist.enabled")) {
            return;
        }

        Player player = event.getPlayer();

        if (player.hasPermission("velocityutils.serverwhitelist.bypass")) {
            return;
        }

        String serverName = event.getOriginalServer().getServerInfo().getName();

        List<String> blockedServers = configManager.getStringList("serverwhitelist.active_servers");

        if (!blockedServers.contains(serverName)) {
            return;
        }

        event.setResult(ServerPreConnectEvent.ServerResult.denied());

        List<String> message = configManager.getStringList("serverwhitelist.message");
        String soundName = configManager.getString("serverwhitelist.sound");

        boolean titleEnabled = configManager.getBoolean("serverwhitelist.title.enabled");
        boolean actionBarEnabled = configManager.getBoolean("serverwhitelist.actionbar.enabled");
        boolean bossBarEnabled = configManager.getBoolean("serverwhitelist.bossbar.enabled");


        // Mensajes
        for (String line : message) {
            if (line.startsWith("{center}")) {
                line = line.replaceFirst("^\\{center\\}\\s*", "");
                line = plugin.getCenteredMessage(line);
            }

            player.sendMessage(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(line)
            );
        }

        // Title
        if (titleEnabled) {
            String titleText = configManager.getString("serverwhitelist.title.title");
            String subtitleText = configManager.getString("serverwhitelist.title.subtitle");
            int fade_in = configManager.getInt("serverwhitelist.title.durations.fade_in");
            int stay = configManager.getInt("serverwhitelist.title.durations.stay");
            int fade_out = configManager.getInt("serverwhitelist.title.durations.fade_out");

            Title title = Title.title(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(titleText),
                    LegacyComponentSerializer.legacyAmpersand().deserialize(subtitleText),
                    Title.Times.times(
                            Duration.ofMillis(fade_in * 50L),
                            Duration.ofMillis(stay * 50L),
                            Duration.ofMillis(fade_out * 50L)
                    )
            );

            player.showTitle(title);
        }

        // ActionBar
        if (actionBarEnabled) {
            String actionBarText = configManager.getString("serverwhitelist.actionbar.message");

            Component actionBar = LegacyComponentSerializer.legacyAmpersand().deserialize(actionBarText);

            player.sendActionBar(actionBar);
        }

        // BossBar
        if (bossBarEnabled) {
            String bossBarText = configManager.getString("serverwhitelist.bossbar.message");
            String bossBarColor = configManager.getString("serverwhitelist.bossbar.color");
            String bossBarOverlay = configManager.getString("serverwhitelist.bossbar.overlay");
            int bossBarDuration = configManager.getInt("serverwhitelist.bossbar.duration");

            final BossBar.Color color = parseColor(bossBarColor);
            final BossBar.Overlay overlay = parseOverlay(bossBarOverlay);

            BossBar bossBar = BossBar.bossBar(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(bossBarText),
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

        // Sonido
        if (soundName != null && !soundName.isEmpty()) {
            plugin.sendSoundToPlayer(player, soundName);
        }

        // Consola
        server.getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                configManager.getMessage("serverwhitelist_tried").replace("{player}", player.getUsername()).replace("{server}", serverName)
        ));
    }

    private BossBar.Color parseColor(String color) {
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
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
