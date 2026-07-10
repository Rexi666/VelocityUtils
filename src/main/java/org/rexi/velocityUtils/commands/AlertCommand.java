package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.VelocityUtils;

public class AlertCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final VelocityUtils plugin;

    public AlertCommand(ConfigManager configManager, ProxyServer server, VelocityUtils plugin) {
        this.server = server;
        this.configManager = configManager;
        this.plugin = plugin;
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
        sendAlert(message);
    }

    public void sendAlert(String message) {
        String alertPrefix = configManager.getString("alert.prefix");
        String soundName = configManager.getString("alert.sound");

        // Convierte el mensaje a un formato de Adventure Text
        Component alertMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(alertPrefix + " " + message);

        // Envía el mensaje a todos los jugadores conectados
        server.getAllPlayers().forEach(player -> {
            player.sendMessage(alertMessage);
        });

        // Enviar sonido
        if (soundName != null && !soundName.isEmpty()) {
            plugin.sendSoundToAll(soundName);
        }

        // También imprime el mensaje en la consola
        server.getConsoleCommandSource().sendMessage(alertMessage);
    }
}