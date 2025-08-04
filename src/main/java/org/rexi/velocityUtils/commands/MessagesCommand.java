package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

import java.util.List;

public class MessagesCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final String commandName;

    public MessagesCommand(ConfigManager configManager, ProxyServer server, String commandName) {
        this.configManager = configManager;
        this.server = server;
        this.commandName = commandName;
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

        String link = configManager.getString("messagescommands." + commandName + ".link");
        String hover = configManager.getString("messagescommands." + commandName + ".hover");
        boolean openLink = configManager.getBoolean("messagescommands." + commandName + ".open_link");

        if (openLink) {
            if (link == null || link.isEmpty() || hover == null || hover.isEmpty()) {
                server.getConsoleCommandSource().sendMessage(legacy(configManager.getMessage("messagescommands_no_link_or_hover_console")));
                player.sendMessage(legacy(configManager.getMessage("messagescommands_error_player")));
                return;
            }
        }

        for (String line : message) {
            if (openLink) {
                Component messageLine = legacy(line)
                        .clickEvent(ClickEvent.openUrl(link))
                        .hoverEvent(HoverEvent.showText(legacy(hover)));
                player.sendMessage(messageLine);
            } else {
                player.sendMessage(legacy(line));
            }
        }

    }

    private net.kyori.adventure.text.Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
