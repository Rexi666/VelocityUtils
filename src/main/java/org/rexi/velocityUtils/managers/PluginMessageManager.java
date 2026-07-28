package org.rexi.velocityUtils.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

public class PluginMessageManager {

    private final ProxyServer server;
    private final Logger logger;

    public PluginMessageManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    private static final MinecraftChannelIdentifier STAFFCHAT_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "staffchat");

    private static final MinecraftChannelIdentifier ADMINCHAT_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "adminchat");

    private static final MinecraftChannelIdentifier PLACEHOLDER_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "placeholders");

    private static final MinecraftChannelIdentifier ALERT_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "alerts");

    private static final MinecraftChannelIdentifier SERVER_EXECUTE_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "serverexecute");

    private static final MinecraftChannelIdentifier SOUNDS_CHANNEL =
            MinecraftChannelIdentifier.create("velocityutils", "sounds");

    private static final MinecraftChannelIdentifier MINECRAFT_BRAND_CHANNEL =
            MinecraftChannelIdentifier.from("minecraft:brand"); // Como es externa, usa from

    public void registerChannels() {
        server.getChannelRegistrar().register(STAFFCHAT_CHANNEL);
        server.getChannelRegistrar().register(ADMINCHAT_CHANNEL);
        server.getChannelRegistrar().register(PLACEHOLDER_CHANNEL);
        server.getChannelRegistrar().register(ALERT_CHANNEL);
        server.getChannelRegistrar().register(SERVER_EXECUTE_CHANNEL);
        server.getChannelRegistrar().register(SOUNDS_CHANNEL);
        server.getChannelRegistrar().register(MINECRAFT_BRAND_CHANNEL);
    }

    public void sendCommandToServer(RegisteredServer toserver, String command) {
        toserver.sendPluginMessage(
                SERVER_EXECUTE_CHANNEL,
                createMessage("execute", command)
        );
    }

    public void sendSoundToPlayer(Player target, String sound) {
        Optional<ServerConnection> serverConnection = target.getCurrentServer();

        if (serverConnection.isPresent()) {
            RegisteredServer server = serverConnection.get().getServer();

            byte[] message = createMessage(
                    "sound",
                    target.getUsername(),
                    sound
            );

            server.sendPluginMessage(SOUNDS_CHANNEL, message);
        } else {
            logger.error("[VelocityUtils] Cannot send sound to player {}", target.getUsername());
        }
    }

    public void sendSoundToAll(String sound) {
        byte[] message = createMessage(
                "sound",
                "*",
                sound
        );

        for (RegisteredServer registeredServer  : server.getAllServers()) {
            registeredServer.sendPluginMessage(SOUNDS_CHANNEL, message);
        }
    }

    private byte[] createMessage(String... values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (DataOutputStream data = new DataOutputStream(out)) {
            for (String value : values) {
                data.writeUTF(value);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        return out.toByteArray();
    }

}
