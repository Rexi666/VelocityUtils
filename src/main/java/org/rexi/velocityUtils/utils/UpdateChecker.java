package org.rexi.velocityUtils.utils;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.rexi.velocityUtils.managers.ConfigManager;

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

    public UpdateChecker(ProxyServer server, PluginContainer plugin, ConfigManager configManager , String currentVersion) {
        this.server = server;
        this.plugin = plugin;
        this.configManager = configManager;
        this.currentVersion = currentVersion;
    }

    private final String lastVersion = "https://raw.githubusercontent.com/Rexi666/VelocityUtils/main/latest-version.txt";
    private final String updateUrl = "https://modrinth.com/plugin/velocityutils-rexi/";

    public void checkForUpdatesConsole() {
        server.getScheduler().buildTask(plugin, () -> {
            try {
                URL url = new URL(lastVersion);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    server.getConsoleCommandSource().sendMessage(configManager.getMessage("new_version_available",
                            "{version}", latestVersion,
                            "{url}", updateUrl));
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
                URL url = new URL(lastVersion);
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                if (!latestVersion.equalsIgnoreCase(currentVersion)) {
                    Component tpLine = configManager.getMessage("new_version_available",
                                    "{version}", latestVersion,
                                    "{url}", updateUrl)
                            .clickEvent(ClickEvent.openUrl(updateUrl));
                    player.sendMessage(tpLine);
                }
            } catch (IOException e) {
                player.sendMessage(
                        Component.text("§6[VelocityUtils] §cError validating updates."));
            }
        }).delay(3, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void PostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("velocityutils.admin")) {
            checkForUpdatesPlayer(player);
        }
    }
}

