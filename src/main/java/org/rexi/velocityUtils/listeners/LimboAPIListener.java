package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;
import org.rexi.velocityUtils.VelocityUtils;

public class LimboAPIListener {
    private final VelocityUtils plugin;

    public LimboAPIListener(VelocityUtils plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLimboRegister(LoginLimboRegisterEvent event) {
        // Se dispara cuando un jugador entra a un Limbo
        Player player = event.getPlayer();
        plugin.playersInSpecialServers.put(player.getUniqueId(), "Limbo");
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        plugin.playersInSpecialServers.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // El jugador llegó a un servidor real, ya no está en Limbo
        plugin.playersInSpecialServers.remove(event.getPlayer().getUniqueId());
    }
}
