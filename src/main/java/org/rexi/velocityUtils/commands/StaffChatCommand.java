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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.rexi.velocityUtils.DiscordWebhook.getUuidFromName;

public class StaffChatCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook staffchatWebhook;
    private static final Set<UUID> staffChatToggled = new HashSet<>();

    public StaffChatCommand(ConfigManager configManager, ProxyServer server, DiscordWebhook staffchatWebhook) {
        this.server = server;
        this.configManager = new ConfigManager();
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

        // Si se pasa un mensaje como argumento, se envía directamente al staffchat
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

            String uuid = getUuidFromName(player.getUsername());
            String avatar = (uuid != null)
                    ? "https://minotar.net/helm/" + uuid + "/64.png"
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

        // Si no hay argumentos, alternar el modo staffchat
        UUID uuid = player.getUniqueId();
        if (staffChatToggled.contains(uuid)) {
            staffChatToggled.remove(uuid);
            String staffchat_disabled = configManager.getMessage("staffchat_disabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(staffchat_disabled));
        } else {
            staffChatToggled.add(uuid);
            String staffchat_enabled = configManager.getMessage("staffchat_enabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(staffchat_enabled));
        }
    }

    public static boolean isInStaffChat(UUID uuid) {
        return staffChatToggled.contains(uuid);
    }

    public static void removeFromStaffChat(UUID uuid) {
        staffChatToggled.remove(uuid);
    }
}
