package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.List;

public class ServerExecuteListener {
    private final VelocityUtils plugin;
    private final ProxyServer server;

    public ServerExecuteListener(VelocityUtils plugin, ProxyServer server) {
        this.plugin = plugin;
        this.server = server;

    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            String serverName = serverConnection.getServerInfo().getName();

            server.getServer(serverName).ifPresent(registeredServer -> {
                if (!plugin.pendingCommands.containsKey(serverName)) return;

                List<String> commands = plugin.pendingCommands.get(serverName);
                for (String cmd : commands) {
                    plugin.sendCommandToServer(registeredServer, cmd);
                }

                plugin.pendingCommands.remove(serverName);
            });
        });
    }
}
