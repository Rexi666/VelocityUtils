package org.rexi.velocityUtils.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

public class PluginMessageListenerAlerts {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final MinecraftChannelIdentifier ALERT_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "alerts");

    public PluginMessageListenerAlerts(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(ALERT_CHANNEL)) return;

        if (!(event.getSource() instanceof ServerConnection serverConn)) return;

        String alertPrefix = configManager.getString("alert.prefix");

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String message = in.readUTF();

        Component alertMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(alertPrefix + " " + message);

        server.getAllPlayers().forEach(player -> player.sendMessage(alertMessage));
        server.getConsoleCommandSource().sendMessage(alertMessage);
    }
}
