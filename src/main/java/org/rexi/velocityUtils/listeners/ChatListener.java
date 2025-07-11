package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

public class ChatListener {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook staffchatWebhook;
    private final DiscordWebhook adminchatWebhook;
    private final VelocityUtils plugin;

    public ChatListener(VelocityUtils plugin, ConfigManager configManager, ProxyServer server,
                        DiscordWebhook staffchatWebhook, DiscordWebhook adminchatWebhook) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = new ConfigManager();
        this.staffchatWebhook = staffchatWebhook;
        this.adminchatWebhook = adminchatWebhook;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        // Eliminar de ambos chats por si acaso
        plugin.staffChatToggled.remove(player.getUniqueId());
        //AdminChatCommand.removeFromAdminChat(player.getUniqueId());
    }
}
