package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.commands.StaffChatCommand;

import static org.rexi.velocityUtils.DiscordWebhook.getUuidFromName;

public class StaffChatListener {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook staffchatWebhook;

    public StaffChatListener(ConfigManager configManager, ProxyServer server, DiscordWebhook staffchatWebhook) {
        this.server = server;
        this.configManager = new ConfigManager();
        this.staffchatWebhook = staffchatWebhook;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!StaffChatCommand.isInStaffChat(player.getUniqueId())) return;

        event.setResult(PlayerChatEvent.ChatResult.denied()); // Cancela el mensaje del chat normal

        configManager.loadConfig();

        String plainMessage = event.getMessage();

        String serverName = player.getCurrentServer()
                .map(registered -> registered.getServerInfo().getName())
                .orElse(configManager.getMessage("server_unknown"));

        String format = configManager.getMessage("staffchat_format")
                .replace("{player}", player.getUsername())
                .replace("{message}", plainMessage)
                .replace("{server}", serverName);

        Component staffMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

        for (Player target : server.getAllPlayers()) {
            if (target.hasPermission("velocityutils.staffchat")) {
                target.sendMessage(staffMessage);
            }
        }

        String uuid = getUuidFromName(player.getUsername());
        String avatar = (uuid != null)
                ? "https://minotar.net/helm/" + uuid + "/64.png"
                : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";

        if (staffchatWebhook != null && configManager.getBoolean("staffchat.discord_hook.enabled")) {
            String raw = configManager.getString("staffchat.discord_hook.message");
            String msg = raw
                    .replace("{player}", player.getUsername())
                    .replace("{message}", plainMessage)
                    .replace("{server}", serverName);
            staffchatWebhook.send(msg, avatar);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        // Elimina al jugador del modo staffchat si estaba activado
        StaffChatCommand.removeFromStaffChat(player.getUniqueId());
    }
}
