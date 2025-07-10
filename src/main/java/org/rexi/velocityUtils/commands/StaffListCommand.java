package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
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
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        List<Player> staffOnline = server.getAllPlayers().stream()
                .filter(p -> p.hasPermission("velocityutils.stafflist.staff"))
                .collect(Collectors.toList());

        if (staffOnline.isEmpty()) {
            String stafflist_no_staff = configManager.getMessage("stafflist_no_staff");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(stafflist_no_staff));
            return;
        }

        String stafflist_header = configManager.getMessage("stafflist_header");
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(stafflist_header));

        for (Player player : staffOnline) {
            String prefix = "";
            if (luckPerms != null) {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());

                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData();
                    prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
                }
            }

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));


            String stafflist_staff = configManager.getMessage("stafflist_staff");
            stafflist_staff = stafflist_staff.replace("{prefix}", prefix).replace("{player}", player.getUsername())
                        .replace("{server}", serverName);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(stafflist_staff));
        }
    }
}