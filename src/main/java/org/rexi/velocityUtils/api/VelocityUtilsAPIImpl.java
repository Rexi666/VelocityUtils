package org.rexi.velocityUtils.api;

import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.commands.AlertCommand;

public class VelocityUtilsAPIImpl implements VelocityUtilsAPI {

    private final VelocityUtils plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;

    public VelocityUtilsAPIImpl(VelocityUtils plugin, ProxyServer server, ConfigManager configManager) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
    }

    @Override
    public void sendAlert(String message) {
        new AlertCommand(configManager, server).sendAlert(message);
    }
}
