package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.Set;
import java.util.UUID;


public class AdminChatCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final DiscordWebhook webhook;
    private final VelocityUtils plugin;
    private final LuckPerms luckPerms;

    public AdminChatCommand(VelocityUtils plugin, ConfigManager configManager, ProxyServer server, DiscordWebhook webhook, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.webhook = webhook;
        this.luckPerms = luckPerms;
    }

    @Override
    public void execute(Invocation invocation) {
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
        UUID uuid = player.getUniqueId();
        Set<UUID> toggled = plugin.getAdminChatToggled();

        // Si se pasa un mensaje como argumento, se envía directamente
        if (args.length > 0) {
            String message = String.join(" ", args);

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            String prefixRaw = obtenerRango(player);

            // Obtener el prefix como Component
            Component prefixComponent = deserializePrefix(prefixRaw);

            String rawFormat = configManager.getMessage("adminchat_format")
                    .replace("{player}", player.getUsername())
                    .replace("{message}", message)
                    .replace("{server}", serverName);

            Component adminMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(rawFormat)
                    .replaceText(TextReplacementConfig.builder()
                            .matchLiteral("{prefix}")
                            .replacement(prefixComponent)
                            .build());

            server.getAllPlayers().forEach(target -> {
                if (target.hasPermission("velocityutils.adminchat")) {
                    target.sendMessage(adminMessage);
                }
            });

            server.getConsoleCommandSource().sendMessage(adminMessage);

            if (configManager.getBoolean("adminchat.discord_hook.enabled")) {
                String raw = configManager.getString("adminchat.discord_hook.message");
                String msg = raw
                        .replace("{player}", player.getUsername())
                        .replace("{message}", message)
                        .replace("{server}", serverName);
                sendAdminChatWebhook(player.getUsername(), msg);
            }

            return;
        }

        // Alternar el modo toggle adminchat
        if (toggled.contains(uuid)) {
            toggled.remove(uuid);
            String adminchat_disabled = configManager.getMessage("adminchat_disabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(adminchat_disabled));
        } else {
            toggled.add(uuid);
            String adminchat_enabled = configManager.getMessage("adminchat_enabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(adminchat_enabled));
        }
    }

    private Component deserializePrefix(String input) {
        // Si contiene <...> asumimos que es MiniMessage
        if (input.contains("<") && input.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                // En caso de error, usa como texto plano
                return Component.text(input);
            }
        }

        // Si no, asumimos que es con códigos &
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }

    private String obtenerRango(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        // Primero intentamos obtener el prefix del propio usuario (o el que LuckPerms determine como prioritario)
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix;
        }

        // Si no tiene prefix propio, usamos el del grupo principal
        String primaryGroupName = user.getPrimaryGroup();
        var group = luckPerms.getGroupManager().getGroup(primaryGroupName);
        if (group != null) {
            String groupPrefix = group.getCachedData().getMetaData().getPrefix();
            if (groupPrefix != null && !groupPrefix.isEmpty()) {
                return groupPrefix;
            }
            return primaryGroupName;
        }

        return primaryGroupName;
    }

    public void sendAdminChatWebhook(String playerName, String message) {
        String webhookUrl = configManager.getString("adminchat.discord_hook.url");
        String avatarUrl = configManager.getString("adminchat.discord_hook.avatar");
        String username = configManager.getString("adminchat.discord_hook.username");
        String title = configManager.getString("adminchat.discord_hook.title");

        String color = configManager.getString("adminchat.discord_hook.color_rgb");
        String thumbnailUrl = webhook.getPlayerAvatar(playerName);
        webhook.send(message, webhookUrl, avatarUrl, username, color, thumbnailUrl, title);
    }
}