package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.managers.PluginMessageManager;

import java.util.List;

public class ServerExecuteListener {
    private final VelocityUtils plugin;
    private final ProxyServer server;
    private final PluginMessageManager pluginMessageManager;

    public ServerExecuteListener(VelocityUtils plugin, ProxyServer server, PluginMessageManager pluginMessageManager) {
        this.plugin = plugin;
        this.server = server;
        this.pluginMessageManager = pluginMessageManager;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            String serverName = serverConnection.getServerInfo().getName();

            server.getServer(serverName).ifPresent(registeredServer -> {
                if (!plugin.pendingCommands.containsKey(serverName)) return;

                List<String> commands = plugin.pendingCommands.get(serverName);
                for (String cmd : commands) {
                    pluginMessageManager.sendCommandToServer(registeredServer, cmd);
                }

                plugin.pendingCommands.remove(serverName);
            });
        });
    }
}
