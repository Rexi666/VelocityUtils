package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.Set;
import java.util.UUID;

import static org.rexi.velocityUtils.DiscordWebhook.getUuidFromName;

public class StaffChatCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook staffchatWebhook;
    private final VelocityUtils plugin;

    public StaffChatCommand(VelocityUtils plugin, ConfigManager configManager, ProxyServer server, DiscordWebhook staffchatWebhook) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.staffchatWebhook = staffchatWebhook;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        configManager.loadConfig();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("velocityutils.staffchat")) {
            String no_permission = configManager.getMessage("no_permission");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        String[] args = invocation.arguments();
        UUID uuid = player.getUniqueId();
        Set<UUID> toggled = plugin.getStaffChatToggled();

        // Si se pasa un mensaje como argumento, se envía directamente
        if (args.length > 0) {
            String message = String.join(" ", args);

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            String format = configManager.getMessage("staffchat_format")
                    .replace("{player}", player.getUsername())
                    .replace("{message}", message)
                    .replace("{server}", serverName);

            Component staffMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

            server.getAllPlayers().forEach(target -> {
                if (target.hasPermission("velocityutils.staffchat")) {
                    target.sendMessage(staffMessage);
                }
            });

            String uuidStr = getUuidFromName(player.getUsername());
            String avatar = (uuidStr != null)
                    ? "https://minotar.net/helm/" + uuidStr + "/64.png"
                    : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";

            if (staffchatWebhook != null && configManager.getBoolean("staffchat.discord_hook.enabled")) {
                String raw = configManager.getString("staffchat.discord_hook.message");
                String msg = raw
                        .replace("{player}", player.getUsername())
                        .replace("{message}", message)
                        .replace("{server}", serverName);
                staffchatWebhook.send(msg, avatar);
            }

            return;
        }

        // Alternar el modo toggle staffchat
        if (toggled.contains(uuid)) {
            toggled.remove(uuid);
            String staffchat_disabled = configManager.getMessage("staffchat_disabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(staffchat_disabled));
        } else {
            toggled.add(uuid);
            String staffchat_enabled = configManager.getMessage("staffchat_enabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(staffchat_enabled));
        }
    }
}