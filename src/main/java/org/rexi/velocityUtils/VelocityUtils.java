package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.rexi.velocityUtils.commands.AlertCommand;
import org.rexi.velocityUtils.commands.VelocityUtilsCommand;
import org.slf4j.Logger;

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
            Component motd = configManager.getMotd();
            ServerPing ping = event.getPing();
            ServerPing updatePing = ping.asBuilder().description(motd).build();
            event.setPing(updatePing);
        } catch (Exception e) {
            logger.error("Error al actualizar el MOTD en ProxyPingEvent", e);
        }
    }
}
