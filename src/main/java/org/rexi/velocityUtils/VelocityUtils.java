package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.commands.AlertCommand;
import org.rexi.velocityUtils.commands.VelocityUtilsCommand;
import org.slf4j.Logger;

import java.util.List;

@Plugin(id = "velocityutils", name = "VelocityUtils", version = BuildConstants.VERSION, authors = {"Rexi666"})
public class VelocityUtils {

    private final ProxyServer server;
    private final ConfigManager configManager;

    @Inject
    public VelocityUtils(ProxyServer server) {
        this.server = server;
        this.configManager = new ConfigManager();
    }

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager.loadConfig();

        server.getCommandManager().register("alert", new AlertCommand(server));
        server.getCommandManager().register("velocityutils", new VelocityUtilsCommand(configManager, server));
        server.getCommandManager().register("vu", new VelocityUtilsCommand(configManager, server));

        System.out.println(Component.text("The plugin has been activated").color(NamedTextColor.GREEN));
        System.out.println(Component.text("Thank you for using Rexi666 plugins").color(NamedTextColor.BLUE));
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        try {
            Component motd;
            if (configManager.isMaintenanceMode()) {
                motd = configManager.getMaintenanceMotd();  // Obtén el MotD de mantenimiento
            } else {
                motd = configManager.getMotd();  // Obtén el MotD normal
            }

            ServerPing ping = event.getPing();
            ServerPing updatePing = ping.asBuilder().description(motd).build();
            event.setPing(updatePing);
        } catch (Exception e) {
            logger.error("Error al actualizar el MOTD en ProxyPingEvent", e);
        }
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        if (configManager.isMaintenanceMode()) {
            List<String> allowedPlayers = configManager.getAllowedPlayers();
            String username = event.getPlayer().getUsername();
            if (!allowedPlayers.contains(username)) {
                String under_maintenance = configManager.getMessage("maintenance_not_on_list");
                event.setResult(LoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacyAmpersand().deserialize(under_maintenance)));
            }
        }
    }
}
