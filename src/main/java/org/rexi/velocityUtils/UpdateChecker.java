package org.rexi.velocityUtils;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {

    private final ProxyServer server;
    private final PluginContainer plugin;
    private final ConfigManager configManager;
    private final String currentVersion;
    private final String updateUrl;

    public UpdateChecker(ProxyServer server, PluginContainer plugin, ConfigManager configManager , String currentVersion, String updateUrl) {
        this.server = server;
        this.plugin = plugin;
        this.configManager = configManager;
        this.currentVersion = currentVersion;
        this.updateUrl = updateUrl;
    }

    public void checkForUpdates() {
        server.getScheduler().buildTask(plugin, () -> {
            try {
                URL url = new URL(updateUrl);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    String message = configManager.getMessage("new_version_available").replace("{version}", latestVersion).replace("{url}", "https://www.spigotmc.org/resources/velocityutils.123517/");
                    server.getConsoleCommandSource().sendMessage(legacy(message));
                }
            } catch (IOException e) {
                server.getConsoleCommandSource().sendMessage(
                        Component.text("§6[VelocityUtils] §cError validating updates."));
            }
        }).delay(3, TimeUnit.SECONDS).schedule();
    }

    public void checkForUpdatesPlayer(Player player) {
        server.getScheduler().buildTask(plugin, () -> {
            try {
                URL url = new URL(updateUrl);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    String message = configManager.getMessage("new_version_available").replace("{version}", latestVersion).replace("{url}", "https://www.spigotmc.org/resources/velocityutils.123517/");
                    Component tpLine = legacy(message)
                            .clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/velocityutils.123517/"));
                    player.sendMessage(tpLine);
                }
            } catch (IOException e) {
                player.sendMessage(
                        Component.text("§6[VelocityUtils] §cError validating updates."));
            }
        }).delay(3, TimeUnit.SECONDS).schedule();
    }

    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }
}

