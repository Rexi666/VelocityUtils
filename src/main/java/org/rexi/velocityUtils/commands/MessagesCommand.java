package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.managers.PluginMessageManager;

import java.util.List;

public class MessagesCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final String commandName;
    private final PluginMessageManager pluginMessageManager;

    public MessagesCommand(ConfigManager configManager, ProxyServer server, String commandName, PluginMessageManager pluginMessageManager) {
        this.configManager = configManager;
        this.server = server;
        this.commandName = commandName;
        this.pluginMessageManager = pluginMessageManager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(configManager.getMessage("no_console"));
            return;
        }

        String permission = "velocityutils.messagescommand." + commandName.toLowerCase();

        if (!player.hasPermission(permission)) {
            player.sendMessage(configManager.getMessage("no_permission"));
            return;
        }

        // Leer datos de la config
        List<String> message = configManager.getStringList("messagescommands." + commandName + ".message");

        if (message.isEmpty()) {
            server.getConsoleCommandSource().sendMessage(configManager.getMessage("messagescommands_no_message_console",
                    "{command}", commandName));
            player.sendMessage(configManager.getMessage("messagescommands_error_player"));
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
                server.getConsoleCommandSource().sendMessage(configManager.getMessage("messagescommands_no_action_or_hover_console"));
                player.sendMessage(configManager.getMessage("messagescommands_error_player"));
                return;
            }
        }

        for (String line : message) {
            if (line.startsWith("{center}")) {
                line = line.replaceFirst("^\\{center\\}\\s*", "");
                line = configManager.getCenteredMessage(line);
            }

            Component messageLine = configManager.legacy(line);

            if (click_action.equalsIgnoreCase("OPEN_URL")) {
                messageLine = messageLine
                        .clickEvent(ClickEvent.openUrl(action))
                        .hoverEvent(HoverEvent.showText(configManager.legacy(hover)));
            } else if (click_action.equalsIgnoreCase("RUN_COMMAND")) {
                messageLine = messageLine
                        .clickEvent(ClickEvent.runCommand(action))
                        .hoverEvent(HoverEvent.showText(configManager.legacy(hover)));
            }

            player.sendMessage(messageLine);

            if (soundName != null && !soundName.isEmpty()) {
                pluginMessageManager.sendSoundToPlayer(player, soundName);
            }
        }

    }
}
