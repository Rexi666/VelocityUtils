package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.Optional;

public class FindCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;

    public FindCommand(ConfigManager configManager, ProxyServer server) {
        this.configManager = configManager;
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Verificar permiso opcional
        if (!source.hasPermission("velocityutils.find")) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        if (args.length != 1) {
            String find_usage = configManager.getMessage("find_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(find_usage));
            return;
        }

        String targetName = args[0];
        Optional<Player> target = server.getPlayer(targetName);

        if (target.isEmpty()) {
            String find_player_not_found = configManager.getMessage("find_player_not_found");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(find_player_not_found));
            return;
        }

        Player player = target.get();
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse(configManager.getMessage("server_unknown"));

        String find_where = configManager.getMessage("find_where");
        find_where = find_where.replace("{player}", player.getUsername());
        find_where = find_where.replace("{server}", serverName);
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(find_where));
    }
}
