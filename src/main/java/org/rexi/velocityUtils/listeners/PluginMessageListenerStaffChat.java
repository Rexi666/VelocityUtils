package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

import java.io.*;
import java.util.UUID;

public class PluginMessageListenerStaffChat {

    private final VelocityUtils plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook staffchatWebhook;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create("velocityutils", "staffchat");

    public PluginMessageListenerStaffChat(VelocityUtils plugin, ProxyServer server, ConfigManager configManager, DiscordWebhook staffchatWebhook) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.staffchatWebhook = staffchatWebhook;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) return;

        if (!(event.getSource() instanceof ServerConnection serverConn)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String subChannel = in.readUTF();

            if (subChannel.equals("toggle_request")) {
                UUID uuid = UUID.fromString(in.readUTF());
                String msg = in.readUTF();

                boolean isToggled = plugin.getStaffChatToggled().contains(uuid);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream data = new DataOutputStream(out);

                data.writeUTF("toggle_response");
                data.writeUTF(uuid.toString());
                data.writeBoolean(isToggled);
                data.writeUTF(msg);

                serverConn.sendPluginMessage(channel, out.toByteArray());
            }

            else if (subChannel.equals("staffchat")) {
                UUID uuid = UUID.fromString(in.readUTF());
                String username = in.readUTF();
                String message = in.readUTF();

                String serverName = serverConn.getServerInfo().getName();

                String format = configManager.getMessage("staffchat_format")
                        .replace("{player}", username)
                        .replace("{message}", message)
                        .replace("{server}", serverName);

                Component staffMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

                server.getAllPlayers().forEach(target -> {
                    if (target.hasPermission("velocityutils.staffchat")) {
                        target.sendMessage(staffMessage);
                    }
                });

                if (staffchatWebhook != null && configManager.getBoolean("staffchat.discord_hook.enabled")) {
                    String raw = configManager.getString("staffchat.discord_hook.message");
                    String msgToSend = raw
                            .replace("{player}", username)
                            .replace("{message}", message)
                            .replace("{server}", serverName);

                    String uuidStr = uuid.toString().replace("-", "");
                    String avatar = "https://minotar.net/helm/" + uuidStr + "/64.png";
                    staffchatWebhook.send(msgToSend, avatar);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
