package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.rexi.velocityUtils.ConfigManager;

import java.util.List;
import java.util.stream.Collectors;

public class StaffListCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final LuckPerms luckPerms;

    public StaffListCommand(ConfigManager configManager, ProxyServer server, LuckPerms luckPerms) {
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Comprobar permiso de uso
        if (!(source.hasPermission("velocityutils.stafflist.use"))) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("no_permission")));
            return;
        }

        List<Player> staffOnline = server.getAllPlayers().stream()
                .filter(p -> p.hasPermission("velocityutils.stafflist.staff"))
                .collect(Collectors.toList());

        if (staffOnline.isEmpty()) {
            source.sendMessage(deserializeLegacy(configManager.getMessage("stafflist_no_staff")));
            return;
        }

        source.sendMessage(deserializeLegacy(configManager.getMessage("stafflist_header")));

        for (Player player : staffOnline) {
            String prefixRaw = "";
            if (luckPerms != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());

                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData();
                    prefixRaw = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                }
            }

            Component prefix = deserializePrefix(prefixRaw);

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            String rawMessage = configManager.getMessage("stafflist_staff")
                    .replace("{player}", player.getUsername())
                    .replace("{server}", serverName);

            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);

            message = message.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("{prefix}")
                    .replacement(prefix)
                    .build());

            source.sendMessage(message);
        }
    }

    private Component deserializeLegacy(String input) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
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
}