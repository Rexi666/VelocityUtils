package org.rexi.velocityUtils.commands;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.rexi.velocityUtils.managers.ConfigManager;
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

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be used by players.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("velocityutils.togglesc")) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        if (plugin.isPlayerInDisabledServer(player)) {
            source.sendMessage(configManager.getMessage("disabled_features_servers"));
            return;
        }

        UUID uuid = player.getUniqueId();

        if (plugin.disabledSC.contains(uuid)) {
            plugin.disabledSC.remove(uuid);
            player.sendMessage(configManager.getMessage("togglesc_enabled"));
        } else {
            plugin.disabledSC.add(uuid);
            player.sendMessage(configManager.getMessage("togglesc_disabled"));
        }
    }
}
