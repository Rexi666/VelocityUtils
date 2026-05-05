package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.rexi.velocityUtils.ConfigManager;

import java.nio.charset.StandardCharsets;

public class BrandListener {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public BrandListener(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Subscribe
    public void onJoin(ServerPostConnectEvent event) {
        if (!configManager.getBoolean("brand.enabled")) return;
        Player player = event.getPlayer();

        String text = configManager.getString("brand.text");

        Component brandText = LegacyComponentSerializer.legacyAmpersand().deserialize(text);

        sendBrand(player, brandText);
    }

    public static void sendBrand(Player player, Component brandText) {
        if (!(player instanceof ConnectedPlayer cp)) return;
        if (cp.getProtocolState() != ProtocolState.PLAY) return;

        String legacy = LegacyComponentSerializer.legacySection().serialize(brandText);

        String brand = legacy + "§r";

        ProtocolVersion version = cp.getProtocolVersion();

        ChannelIdentifier channel = MinecraftChannelIdentifier.from("minecraft:brand");

        ByteBuf buf = Unpooled.buffer();

        if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
            ProtocolUtils.writeString(buf, brand);
        } else {
            buf.writeCharSequence(brand, StandardCharsets.UTF_8);
        }

        cp.getConnection().write(new PluginMessagePacket(channel.getId(), buf));
    }

    public void sendBrandToAll() {
        if (!configManager.getBoolean("brand.enabled")) return;
        Component brandText = LegacyComponentSerializer.legacyAmpersand().deserialize(configManager.getString("brand.text"));
        for (Player player : server.getAllPlayers()) {
            sendBrand(player, brandText);
        }
    }
}