package org.rexi.velocityUtils.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.UUID;

public class PluginMessageListenerPlaceholders {

    private final ProxyServer server;
    private final MinecraftChannelIdentifier PLACEHOLDER_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "placeholders");

    public PluginMessageListenerPlaceholders(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(PLACEHOLDER_CHANNEL)) return;

        if (!(event.getSource() instanceof ServerConnection serverConn)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String playerUUIDStr = in.readUTF();
        String placeholder = in.readUTF();

        String response = switch (placeholder) {
            case "globalplayers" ->  {
                String val = String.valueOf(server.getAllPlayers().size());
                yield val;
            }
            default -> {
                if (placeholder.startsWith("players_")) {
                    String serverName = placeholder.substring("players_".length());
                    var serverOpt = server.getServer(serverName);
                    if (serverOpt.isPresent()) {
                        yield String.valueOf(serverOpt.get().getPlayersConnected().size());
                    } else {
                        yield "0";
                    }
                } else yield "null";
            }
        };

        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(playerUUIDStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        // Ya no necesitas player.sendPluginMessage
        // Envía la respuesta con la conexión del servidor
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(placeholder);
        out.writeUTF(response);

        serverConn.sendPluginMessage(PLACEHOLDER_CHANNEL, out.toByteArray());
    }
}