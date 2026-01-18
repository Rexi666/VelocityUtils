package org.rexi.velocityUtils.commands;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.UUID;

public class ToggleScCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final VelocityUtils plugin;

    public ToggleScCommand(VelocityUtils plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        configManager.loadConfig();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("velocityutils.togglesc")) {
            String no_permission = configManager.getMessage("no_permission");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        UUID uuid = player.getUniqueId();

        if (plugin.disabledSC.contains(uuid)) {
            plugin.disabledSC.remove(uuid);
            String sc_enabled = configManager.getMessage("togglesc_enabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sc_enabled));
        } else {
            plugin.disabledSC.add(uuid);
            String sc_disabled = configManager.getMessage("togglesc_disabled");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sc_disabled));
        }
    }
}
