package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.rexi.velocityUtils.managers.ConfigManager;

public class MotdListener {

    private final ConfigManager configManager;

    private Component normalMotd;
    private Component maintenanceMotd;
    private boolean motdEnabled;
    private boolean maintenanceEnabled;

    public MotdListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void reload() {
        motdEnabled = configManager.getBoolean("motd.enabled");
        maintenanceEnabled = configManager.getBoolean("maintenance.active");

        normalMotd = buildMotd(
                configManager.getString("motd.line1"),
                configManager.getString("motd.line2")
        );

        maintenanceMotd = buildMotd(
                configManager.getString("maintenance.motd.line1"),
                configManager.getString("maintenance.motd.line2")
        );
    }

    private Component buildMotd(String line1, String line2) {
        // Convertir
        Component component1 = line1.contains("<") && line1.contains(">") ? MINI_MESSAGE.deserialize(line1) : configManager.legacy(line1);
        Component component2 = line2.contains("<") && line2.contains(">") ? MINI_MESSAGE.deserialize(line2) : configManager.legacy(line2);

        return Component.text()
                .append(component1)
                .append(Component.newline())
                .append(component2)
                .build();
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        if (!motdEnabled && !maintenanceEnabled) {
            return;
        }

        Component motd = maintenanceEnabled
                ? maintenanceMotd
                : normalMotd;

        event.setPing(event.getPing().asBuilder().description(motd).build());
    }

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    public void changeMaintenanceActive(boolean enabled) {
        maintenanceEnabled = enabled;
    }

    public boolean getMaintenanceStatus() {
        return maintenanceEnabled;
    }
}
