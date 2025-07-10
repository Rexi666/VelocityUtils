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
import static org.rexi.velocityUtils.commands.StaffChatCommand.staffChatToggled;

public class AdminChatCommand implements SimpleCommand{
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook adminchatWebhook;
    public static final Set<UUID> adminChatToggled = new HashSet<>();

    public AdminChatCommand(ConfigManager configManager, ProxyServer server, DiscordWebhook adminchatWebhook) {
        this.server = server;
        this.configManager = new ConfigManager();
        this.adminchatWebhook = adminchatWebhook;
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source = invocation.source();
        configManager.loadConfig();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("velocityutils.adminchat")) {
            String no_permission = configManager.getMessage("no_permission");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        String[] args = invocation.arguments();

        // Si se pasa un mensaje como argumento, se envía directamente al adminchat
        if (args.length > 0) {
            String message = String.join(" ", args);

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            String format = configManager.getMessage("adminchat_format")
                    .replace("{player}", player.getUsername())
                    .replace("{message}", message)
                    .replace("{server}", serverName);

            Component adminMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

            server.getAllPlayers().forEach(target -> {
                if (target.hasPermission("velocityutils.adminchat")) {
                    target.sendMessage(adminMessage);
                }
            });

            String uuid = getUuidFromName(player.getUsername());
            String avatar = (uuid != null)
                    ? "https://minotar.net/helm/" + uuid + "/64.png"
                    : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";

            if (adminchatWebhook != null && configManager.getBoolean("adminchat.discord_hook.enabled")) {
                String raw = configManager.getString("adminchat.discord_hook.message");
                String msg = raw
                        .replace("{player}", player.getUsername())
                        .replace("{message}", message)
                        .replace("{server}", serverName);
                adminchatWebhook.send(msg, avatar);
            }

            return;
        }

        // Si no hay argumentos, alternar el modo adminchat
        UUID uuid = player.getUniqueId();
        if (adminChatToggled.contains(uuid)) {
            adminChatToggled.remove(uuid);
            String adminchat_disabled = configManager.getMessage("adminchat_disabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(adminchat_disabled));
        } else {
            adminChatToggled.add(uuid);
            staffChatToggled.remove(uuid); // Asegurarse de que el jugador no esté en staffchat si se activa adminchat
            String adminchat_enabled = configManager.getMessage("adminchat_enabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(adminchat_enabled));
        }
    }

    public static boolean isInAdminChat(UUID uuid) {
        return adminChatToggled.contains(uuid);
    }

    public static void removeFromAdminChat(UUID uuid) {
        adminChatToggled.remove(uuid);
    }
}
