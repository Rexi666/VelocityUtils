package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import net.kyori.adventure.text.Component;
import org.rexi.velocityUtils.managers.ConfigManager;

public class MaintenanceListener {

    private final ConfigManager configManager;
    private final MotdListener motdListener;

    public MaintenanceListener(ConfigManager configManager, MotdListener motdListener) {
        this.configManager = configManager;
        this.motdListener = motdListener;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (motdListener.getMaintenanceStatus()) {
            if (!event.getPlayer().hasPermission("velocityutils.maintenance.bypass")) {
                Component message = configManager.getMessage("maintenance_not_on_list");
                event.setResult(LoginEvent.ComponentResult.denied(message));
            }
        }
    }
}
