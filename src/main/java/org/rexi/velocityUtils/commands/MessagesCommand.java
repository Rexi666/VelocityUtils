package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

import java.util.List;

public class MessagesCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final String commandName;
    private final VelocityUtils plugin;

    public MessagesCommand(ConfigManager configManager, ProxyServer server, String commandName, VelocityUtils plugin) {
        this.configManager = configManager;
        this.server = server;
        this.commandName = commandName;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(legacy(configManager.getMessage("no_console")));
            return;
        }

        String permission = "velocityutils.messagescommand." + commandName.toLowerCase();

        if (!player.hasPermission(permission)) {
            player.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        // Leer datos de la config
        List<String> message = configManager.getStringList("messagescommands." + commandName + ".message");

        if (message.isEmpty()) {
            String messagescommands_no_message = configManager.getMessage("messagescommands_no_message_console")
                    .replace("{command}", commandName);
            server.getConsoleCommandSource().sendMessage(legacy(messagescommands_no_message));
            player.sendMessage(legacy(configManager.getMessage("messagescommands_error_player")));
            return;
        }

        String action = configManager.getString("messagescommands." + commandName + ".action");
        String hover = configManager.getString("messagescommands." + commandName + ".hover");
        String click_action = configManager.getString("messagescommands." + commandName + ".click_action");
        String soundName = configManager.getString("messagescommands." + commandName + ".sound");

        if (click_action == null ||
                (!click_action.equalsIgnoreCase("OPEN_URL") && !click_action.equalsIgnoreCase("RUN_COMMAND"))) {
            click_action = "NONE";
        }

        if (click_action.equalsIgnoreCase("OPEN_URL") || click_action.equalsIgnoreCase("RUN_COMMAND")) {
            if (action == null || action.isEmpty() || hover == null || hover.isEmpty()) {
                server.getConsoleCommandSource().sendMessage(legacy(configManager.getMessage("messagescommands_no_action_or_hover_console")));
                player.sendMessage(legacy(configManager.getMessage("messagescommands_error_player")));
                return;
            }
        }

        for (String line : message) {
            Component messageLine = legacy(line);

            if (click_action.equalsIgnoreCase("OPEN_URL")) {
                messageLine = messageLine
                        .clickEvent(ClickEvent.openUrl(action))
                        .hoverEvent(HoverEvent.showText(legacy(hover)));
            } else if (click_action.equalsIgnoreCase("RUN_COMMAND")) {
                messageLine = messageLine
                        .clickEvent(ClickEvent.runCommand(action))
                        .hoverEvent(HoverEvent.showText(legacy(hover)));
            }

            player.sendMessage(messageLine);

            if (soundName != null && !soundName.isEmpty()) {
                plugin.sendSoundToPlayer(player, soundName);
            }
        }

    }

    private net.kyori.adventure.text.Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
