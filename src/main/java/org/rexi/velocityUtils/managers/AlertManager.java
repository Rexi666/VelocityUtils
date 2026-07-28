package org.rexi.velocityUtils.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.rexi.velocityUtils.VelocityUtils;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AlertManager {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;
    private final PluginMessageManager pluginMessageManager;
    private final VelocityUtils plugin;

    private ScheduledTask alertsTask;
    private int currentAlertIndex = 0;
    private final List<String> alertList = new ArrayList<>();

    public AlertManager(ProxyServer server,
                        ConfigManager configManager,
                        Logger logger,
                        PluginMessageManager pluginMessageManager,
                        VelocityUtils plugin) {
        this.server = server;
        this.configManager = configManager;
        this.logger = logger;
        this.pluginMessageManager = pluginMessageManager;
        this.plugin = plugin;
    }

    public void startRegularAlerts() {
        if (alertsTask != null) {
            alertsTask.cancel();
            alertsTask = null;
        }

        if (!configManager.getBoolean("regular_alerts.enabled")) {
            return;
        }

        int delay = configManager.getInt("regular_alerts.delay_seconds");

        loadAlerts();

        alertsTask = server.getScheduler()
                .buildTask(plugin, () -> sendNextAlert())
                .delay(delay, TimeUnit.SECONDS)
                .repeat(delay, TimeUnit.SECONDS)
                .schedule();
    }

    private void loadAlerts() {
        alertList.clear();
        currentAlertIndex = 0;

        ConfigurationNode alertsNode = configManager.getRootNode().node("regular_alerts", "alerts");

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : alertsNode.childrenMap().entrySet()) {
            alertList.add(entry.getKey().toString());
        }
    }

    private void sendNextAlert() {
        if (alertList.isEmpty()) {
            return;
        }

        String soundName = configManager.getString("regular_alerts.sound");

        String alert = alertList.get(currentAlertIndex);
        List<String> messages = configManager.getStringList("regular_alerts.alerts." + alert + ".message");

        String action = configManager.getString("regular_alerts.alerts." + alert + ".action");
        String hover = configManager.getString("regular_alerts.alerts." + alert + ".hover");
        String click_action = configManager.getString("regular_alerts.alerts." + alert + ".click_action");

        if (click_action == null ||
                (!click_action.equalsIgnoreCase("OPEN_URL") && !click_action.equalsIgnoreCase("RUN_COMMAND"))) {
            click_action = "NONE";
        }

        if (messages == null || messages.isEmpty()) {
            logger.warn("Error trying to send regular alert '{}': message is empty: ", alert);
            return;
        }

        if (click_action.equalsIgnoreCase("OPEN_URL") || click_action.equalsIgnoreCase("RUN_COMMAND")) {
            if (action == null || action.isEmpty() || hover == null || hover.isEmpty()) {
                logger.warn("Error trying to send regular alert '{}': action or hover message is missing or empty: ", alert);
                return;
            }
        }

        for (Player player : server.getAllPlayers()) {
            for (String line : messages) {
                if (line.startsWith("{center}")) {
                    line = line.replaceFirst("^\\{center\\}\\s*", "");
                    line = configManager.getCenteredMessage(line);
                }

                Component messageLine = configManager.legacy(line);

                if (click_action.equalsIgnoreCase("OPEN_URL")) {
                    messageLine = messageLine
                            .clickEvent(ClickEvent.openUrl(action))
                            .hoverEvent(HoverEvent.showText(configManager.legacy(hover)));
                } else if (click_action.equalsIgnoreCase("RUN_COMMAND")) {
                    messageLine = messageLine
                            .clickEvent(ClickEvent.runCommand(action))
                            .hoverEvent(HoverEvent.showText(configManager.legacy(hover)));
                }
                player.sendMessage(messageLine);
            }
        }

        pluginMessageManager.sendSoundToAll(soundName);

        currentAlertIndex++;

        if (currentAlertIndex >= alertList.size()) {
            currentAlertIndex = 0;
        }
    }
}
