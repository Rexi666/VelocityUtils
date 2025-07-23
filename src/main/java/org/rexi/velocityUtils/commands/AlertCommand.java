package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;

public class AlertCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;

    public AlertCommand(ConfigManager configManager, ProxyServer server) {
        this.server = server;
        this.configManager = new ConfigManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        configManager.loadConfig();

        // Verifica si el usuario tiene permiso
        if (!source.hasPermission("velocityutils.alert") && !(source instanceof ConsoleCommandSource)) {
            String no_permission = configManager.getMessage("no_permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(no_permission));
            return;
        }

        // Verifica si hay argumentos (evita enviar solo el prefix)
        if (invocation.arguments().length == 0) {
            String alert_usage = configManager.getMessage("alert_usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(alert_usage));
            return;
        }

        // Obtiene el mensaje y el prefijo
        String message = String.join(" ", invocation.arguments());
        String alertPrefix = configManager.getString("alert.prefix");

        // Convierte el mensaje a un formato de Adventure Text
        Component alertMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(alertPrefix + " " + message);

        // Envía el mensaje a todos los jugadores conectados
        server.getAllPlayers().forEach(player -> player.sendMessage(alertMessage));

        // También imprime el mensaje en la consola
        server.getConsoleCommandSource().sendMessage(alertMessage);
    }
}