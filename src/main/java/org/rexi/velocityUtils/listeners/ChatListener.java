package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

public class ChatListener {

    private final VelocityUtils plugin;

    public ChatListener(VelocityUtils plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        // Eliminar de ambos chats por si acaso
        plugin.staffChatToggled.remove(player.getUniqueId());
        plugin.adminChatToggled.remove(player.getUniqueId());
    }
}
