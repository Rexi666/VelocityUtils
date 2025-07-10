package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.listeners.StaffChatListener;
import org.slf4j.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.util.List;

@Plugin(
        id = "velocityutils",
        name = "VelocityUtils",
        version = BuildConstants.VERSION,
        authors = {"Rexi666"},
        dependencies = {@Dependency(id = "luckperms", optional = true)})
public class VelocityUtils {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private DiscordWebhook reportWebhook;
    private DiscordWebhook staffchatWebhook;
    private LuckPerms luckPerms = null;

    @Inject
    public VelocityUtils(ProxyServer server) {
        this.server = server;
        this.configManager = new ConfigManager();
    }

    @Inject private Logger logger;


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager.loadConfig();

        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("[VelocityUtils] LuckPerms detected.");
        } catch (IllegalStateException e) {
            this.luckPerms = null;
            logger.warn("[VelocityUtils] LuckPerms not detected.");
        }

        if (configManager.getBoolean("report.discord_hook.enabled")) {
            String reportWebhookUrl = configManager.getString("report.discord_hook.url");
            if (reportWebhookUrl != null && reportWebhookUrl.startsWith("http")) {
                this.reportWebhook = new DiscordWebhook(reportWebhookUrl, configManager);
                reportWebhook.setAvatarUrl(configManager.getString("report.discord_hook.avatar"));
                reportWebhook.setUsername(configManager.getString("report.discord_hook.username"));
                reportWebhook.setTitle(configManager.getString("report.discord_hook.title"));
                reportWebhook.setColorRGB(configManager.getString("report.discord_hook.color_rgb"));
            }
        }
        if (configManager.getBoolean("staffchat.discord_hook.enabled")) {
            String staffchatWebhookUrl = configManager.getString("staffchat.discord_hook.url");
            if (staffchatWebhookUrl != null && staffchatWebhookUrl.startsWith("http")) {
                this.staffchatWebhook = new DiscordWebhook(staffchatWebhookUrl, configManager);
                staffchatWebhook.setAvatarUrl(configManager.getString("staffchat.discord_hook.avatar"));
                staffchatWebhook.setUsername(configManager.getString("staffchat.discord_hook.username"));
                staffchatWebhook.setTitle(configManager.getString("staffchat.discord_hook.title"));
                staffchatWebhook.setColorRGB(configManager.getString("staffchat.discord_hook.color_rgb"));
            }
        }

        server.getEventManager().register(this, new StaffChatListener(configManager, server, staffchatWebhook));

        server.getCommandManager().register("alert", new AlertCommand(configManager,server));
        server.getCommandManager().register("velocityutils", new VelocityUtilsCommand(configManager, server));
        server.getCommandManager().register("vu", new VelocityUtilsCommand(configManager, server));
        server.getCommandManager().register("maintenance", new MaintenanceCommand(configManager, server));
        server.getCommandManager().register("report", new ReportCommand(configManager, server, reportWebhook));
        server.getCommandManager().register("goto", new GotoCommand(configManager, server));
        server.getCommandManager().register("find", new FindCommand(configManager, server));
        if (this.luckPerms != null) {
            server.getCommandManager().register("stafflist", new StaffListCommand(configManager, server, luckPerms));
        } else {
            server.getCommandManager().register("stafflist", new StaffListCommand(configManager, server, null));
        }
        server.getCommandManager().register("staffchat", new StaffChatCommand(configManager, server, staffchatWebhook));
        server.getCommandManager().register("sc", new StaffChatCommand(configManager, server, staffchatWebhook));

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
