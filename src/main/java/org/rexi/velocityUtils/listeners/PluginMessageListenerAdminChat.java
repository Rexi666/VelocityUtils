package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

import java.io.*;
import java.util.UUID;

public class PluginMessageListenerAdminChat {

    private final VelocityUtils plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook adminchatWebhook;
    private final LuckPerms luckPerms;
    private final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.create("velocityutils", "adminchat");

    public PluginMessageListenerAdminChat(VelocityUtils plugin, ProxyServer server, ConfigManager configManager, DiscordWebhook adminchatWebhook, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.adminchatWebhook = adminchatWebhook;
        this.luckPerms = luckPerms;
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

                boolean isToggled = plugin.getAdminChatToggled().contains(uuid);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream data = new DataOutputStream(out);

                data.writeUTF("toggle_response");
                data.writeUTF(uuid.toString());
                data.writeBoolean(isToggled);
                data.writeUTF(msg);

                serverConn.sendPluginMessage(channel, out.toByteArray());
            }

            else if (subChannel.equals("adminchat")) {
                UUID uuid = UUID.fromString(in.readUTF());
                String username = in.readUTF();
                String message = in.readUTF();

                String serverName = serverConn.getServerInfo().getName();

                String prefixRaw = "";
                if (luckPerms != null) {
                    User user = luckPerms.getUserManager().getUser(uuid);

                    if (user != null) {
                        CachedMetaData metaData = user.getCachedData().getMetaData();
                        prefixRaw = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                    }
                }

                Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixRaw);

                String format = configManager.getMessage("adminchat_format")
                        .replace("{player}", username)
                        .replace("{message}", message)
                        .replace("{server}", serverName);

                Component adminMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format)
                        .replaceText(TextReplacementConfig.builder()
                                .matchLiteral("{prefix}")
                                .replacement(prefixComponent)
                                .build());

                server.getAllPlayers().forEach(target -> {
                    if (target.hasPermission("velocityutils.adminchat")) {
                        target.sendMessage(adminMessage);
                    }
                });

                if (adminchatWebhook != null && configManager.getBoolean("adminchat.discord_hook.enabled")) {
                    String raw = configManager.getString("adminchat.discord_hook.message");
                    String msgToSend = raw
                            .replace("{player}", username)
                            .replace("{message}", message)
                            .replace("{server}", serverName);

                    String uuidStr = uuid.toString().replace("-", "");
                    String avatar = "https://minotar.net/helm/" + uuidStr + "/64.png";
                    adminchatWebhook.send(msgToSend, avatar);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Component deserializePrefix(String input) {
        // Si contiene <...> asumimos que es MiniMessage
        if (input.contains("<") && input.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                // En caso de error, usa como texto plano
                return Component.text(input);
            }
        }

        // Si no, asumimos que es con códigos &
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }
}
