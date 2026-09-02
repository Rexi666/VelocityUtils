package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.List;
import java.util.stream.Collectors;

public class StaffListCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final LuckPerms luckPerms;
    private final VelocityUtils plugin;

    public StaffListCommand(ConfigManager configManager, ProxyServer server, LuckPerms luckPerms, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Comprobar permiso de uso
        if (!(source.hasPermission("velocityutils.stafflist.use"))) {
            source.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (source instanceof Player p && plugin.isPlayerInDisabledServer(p)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        List<Player> staffOnline = getStaffOnline();

        if (staffOnline.isEmpty()) {
            source.sendMessage(configManager.getMessage("stafflist_no_staff"));
            return;
        }

        source.sendMessage(configManager.getMessage("stafflist_header"));

        for (Player player : staffOnline) {
            String prefixRaw = obtenerRango(player);

            Component prefix = deserializePrefix(prefixRaw);

            String serverName = plugin.getServerName(player);

            Component message = configManager.getMessage("stafflist_staff",
                    "{player}", player.getUsername(),
                    "{server}", serverName);

            message = message.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("{prefix}")
                    .replacement(prefix)
                    .build());

            source.sendMessage(message);
        }
    }

    public Component deserializePrefix(String input) {
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
        return configManager.legacy(input);
    }

    public List<Player> getStaffOnline() {
        return server.getAllPlayers().stream()
                .filter(p -> p.hasPermission("velocityutils.stafflist.staff"))
                .sorted((p1, p2) -> Integer.compare(
                        getGroupWeight(p2), getGroupWeight(p1)
                ))
                .collect(Collectors.toList());
    }

    public int getGroupWeight(Player player) {
        if (luckPerms == null) return 0;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            var group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group != null && group.getWeight().isPresent()) {
                return group.getWeight().getAsInt();
            }
        }
        return 0;
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

    public String obtenerRangoPrincipal(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        return user.getPrimaryGroup();
    }

}