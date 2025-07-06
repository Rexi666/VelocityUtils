package org.rexi.velocityUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigManager {

    private final Path configPath;
    private final YamlConfigurationLoader loader;
    private Config config;

    public ConfigManager() {
        // Define la carpeta del plugin dentro de "plugins/"
        Path pluginFolder = Paths.get("plugins", "VelocityUtils");

        // Asegura que la carpeta del plugin existe
        if (!Files.exists(pluginFolder)) {
            try {
                Files.createDirectories(pluginFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Ruta correcta dentro de "plugins/VelocityUtils/"
        this.configPath = pluginFolder.resolve("config.yml");

        // 💡 Configura el YAML para evitar inline objects
        this.loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK) // 🔥 Evita la serialización en una sola línea
                .build();
    }

    public void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                // Si el archivo no existe, crea uno con valores por defecto
                config = new Config();
                saveConfig();
            } else {
                ConfigurationNode node = loader.load();
                config = new Config();

                // Asegurar que la estructura del YAML sea correcta
                config.setAlertPrefix(node.node("alert", "prefix").getString("&7[&b&lSERVER&7]"));

                // Cargar los mensajes si no existen
                if (node.node("motd", "line1").empty()) {
                    node.node("motd", "line1").set("&aWelcome to this Velocity Server!");
                }
                if (node.node("motd", "line2").empty()) {
                    node.node("motd", "line2").set("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");
                }

                if (node.node("maintenance", "active").empty()) {
                    node.node("maintenance", "active").set(false);
                }
                if (node.node("maintenance", "motd", "line1").empty()) {
                    node.node("maintenance", "motd", "line1").set("&cServer under maintenance!");
                }
                if (node.node("maintenance", "motd", "line2").empty()) {
                    node.node("maintenance", "motd", "line2").set("<bold><gradient:red:yellow>Try again later</gradient></bold>");
                }
                if (node.node("maintenance", "allowed").empty()) {
                    node.node("maintenance", "allowed").setList(String.class, List.of("Rexigamer666"));
                }

                if (node.node("report", "teleport_on_click").empty()) {
                    node.node("report", "teleport_on_click").set(true);
                }
                if (node.node("report", "message").empty()) {
                    node.node("report", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&eNew Report from {player}!",
                            "&fReported: &c{reported}",
                            "&fReason: &b{reason}",
                            "&eClick to teleport",
                            "&f-----------------------------"));
                }
                if (node.node("report", "discord_hook", "enabled").empty()) {
                    node.node("report", "discord_hook", "enabled").set(false);
                }
                if (node.node("report", "discord_hook", "url").empty()) {
                    node.node("report", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("report", "discord_hook", "avatar").empty()) {
                    node.node("report", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("report", "discord_hook", "username").empty()) {
                    node.node("report", "discord_hook", "username").set("VelocityUtils");
                }
                if (node.node("report", "discord_hook", "color_rgb").empty()) {
                    node.node("report", "discord_hook", "color_rgb").set("240,43,20");
                }
                if (node.node("report", "discord_hook", "title").empty()) {
                    node.node("report", "discord_hook", "title").set("\uD83D\uDCE2User Report\uD83D\uDCE2");
                }
                if (node.node("report", "discord_hook", "message").empty()) {
                    node.node("report", "discord_hook", "message").set(String.class, """
      📢 **New Report from {reporter}**
      👤 **Reported:** {reported}
      📄 **Reason:** {reason}
      🌍 **Server:** {server}
      """);
                }

                if (node.node("messages", "no_permission").empty()) {
                    node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
                }
                if (node.node("messages", "no_console").empty()) {
                    node.node("messages", "no_console").set("&cOnly players can use this command");
                }
                if (node.node("messages", "alert_usage").empty()) {
                    node.node("messages", "alert_usage").set("&cUsage: /alert <message>");
                }
                if (node.node("messages", "configuration_reloaded").empty()) {
                    node.node("messages", "configuration_reloaded").set("&aConfiguration reloaded successfully!");
                }
                if (node.node("messages", "velocityutils_usage").empty()) {
                    node.node("messages", "velocityutils_usage").set("&cUsage: /velocityutils reload");
                }
                if (node.node("messages", "maintenance_not_on_list").empty()) {
                    node.node("messages", "maintenance_not_on_list").set("&cThe server is under maintenance!");
                }
                if (node.node("messages", "maintenance_usage").empty()) {
                    node.node("messages", "maintenance_usage").set("&cUsage: /maintenance <on|off> | /maintenance <add|remove> <nick>");
                }
                if (node.node("messages", "maintenance_activated").empty()) {
                    node.node("messages", "maintenance_activated").set("&aMaintenance mode activated.");
                }
                if (node.node("messages", "maintenance_deactivated").empty()) {
                    node.node("messages", "maintenance_deactivated").set("&cMaintenance mode deactivated.");
                }
                if (node.node("messages", "maintenance_already_on_list").empty()) {
                    node.node("messages", "maintenance_already_on_list").set("&cThe player is already in the maintenance list.");
                }
                if (node.node("messages", "maintenance_player_added").empty()) {
                    node.node("messages", "maintenance_player_added").set("&aPlayer {player} added to the maintenance list.");
                }
                if (node.node("messages", "maintenance_player_not_on_list").empty()) {
                    node.node("messages", "maintenance_player_not_on_list").set("&cThe player is not in the maintenance list.");
                }
                if (node.node("messages", "maintenance_player_removed").empty()) {
                    node.node("messages", "maintenance_player_removed").set("&cPlayer {player} removed from the maintenance list.");
                }
                if (node.node("messages", "report_usage").empty()) {
                    node.node("messages", "report_usage").set("&cUsage: /report <nick> <reason>");
                }
                if (node.node("messages", "report_player_not_found").empty()) {
                    node.node("messages", "report_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("messages", "report_sent").empty()) {
                    node.node("messages", "report_sent").set("&aYour report for the player {target} was sent");
                }
                if (node.node("messages", "report_hover").empty()) {
                    node.node("messages", "report_hover").set("&bClick to teleport");
                }
                if (node.node("messages", "report_cooldown").empty()) {
                    node.node("messages", "report_cooldown").set("&cYou have {time}s before using /report again");
                }
                if (node.node("messages", "report_webhook_error").empty()) {
                    node.node("messages", "report_webhook_error").set("&cError trying to send discord report webhook");
                }
                if (node.node("messages", "goto_usage").empty()) {
                    node.node("messages", "goto_usage").set("&cUsage: /goto <player>");
                }
                if (node.node("messages", "goto_player_not_found").empty()) {
                    node.node("messages", "goto_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("messages", "goto_server_not_found").empty()) {
                    node.node("messages", "goto_server_not_found").set("&cServer could not be found");
                }
                if (node.node("messages", "goto_same_server").empty()) {
                    node.node("messages", "goto_same_server").set("&cYou are currently on the same server as {player}");
                }
                if (node.node("messages", "goto_connecting").empty()) {
                    node.node("messages", "goto_connecting").set("&aConnecting with {player} server");
                }
                if (node.node("messages", "find_usage").empty()) {
                    node.node("messages", "find_usage").set("&cUsage: /find <player>");
                }
                if (node.node("messages", "find_player_not_found").empty()) {
                    node.node("messages", "find_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("messages", "find_unknown").empty()) {
                    node.node("messages", "find_unknown").set("Unknown");
                }
                if (node.node("messages", "find_where").empty()) {
                    node.node("messages", "find_where").set("&b{player} &eis on &b{server}");
                }

                // Guardar en caso de que se hayan agregado valores predeterminados
                loader.save(node);
            }
        } catch (SerializationException e) {
            System.err.println("Error al serializar/deserializar la configuración.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error al leer/escribir el archivo de configuración.");
            e.printStackTrace();
        }
    }


    public void saveConfig() {
        try {
            ConfigurationNode node = loader.createNode();

            // 💡 Crear la estructura correctamente sin inline mapping
            node.node("alert", "prefix").set(config.getAlertPrefix());

            // Agregar mensajes predeterminados
            node.node("motd", "line1").set("&aWelcome to this Velocity Server!");
            node.node("motd", "line2").set("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");

            node.node("maintenance", "active").set(false);
            node.node("maintenance", "motd", "line1").set("&cServer under maintenance!");
            node.node("maintenance", "motd", "line2").set("<bold><gradient:red:yellow>Try again later</gradient></bold>");
            node.node("maintenance", "allowed").setList(String.class, List.of("Rexigamer666"));

            node.node("report", "teleport_on_click").set(true);
            node.node("report", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eNew Report from {player}!",
                    "&fReported: &c{reported}",
                    "&fReason: &b{reason}",
                    "&eClick to teleport",
                    "&f-----------------------------"));
            node.node("report", "discord_hook", "enabled").set(false);
            node.node("report", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("report", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("report", "discord_hook", "username").set("VelocityUtils");
            node.node("report", "discord_hook", "color_rgb").set("240,43,20");
            node.node("report", "discord_hook", "title").set("\uD83D\uDCE2User Report\uD83D\uDCE2");
            node.node("report", "discord_hook", "message").set(String.class, """
      📢 **New Report from {reporter}**
      👤 **Reported:** {reported}
      📄 **Reason:** {reason}
      🌍 **Server:** {server}
      """);

            node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
            node.node("messages", "no_console").set("&cOnly players can use this command");
            node.node("messages", "alert_usage").set("&cUsage: /alert <message>");
            node.node("messages", "configuration_reloaded").set("&aConfiguration reloaded successfully!");
            node.node("messages", "velocityutils_usage").set("&cUsage: /velocityutils reload");
            node.node("messages", "maintenance_not_on_list").set("&cThe server is under maintenance!");
            node.node("messages", "maintenance_usage").set("&cUsage: /maintenance <on|off> | /maintenance <add|remove> <nick>");
            node.node("messages", "maintenance_activated").set("&aMaintenance mode activated.");
            node.node("messages", "maintenance_deactivated").set("&cMaintenance mode deactivated.");
            node.node("messages", "maintenance_already_on_list").set("&cThe player is already in the maintenance list.");
            node.node("messages", "maintenance_player_added").set("&aPlayer {player} added to the maintenance list.");
            node.node("messages", "maintenance_player_not_on_list").set("&cThe player is not in the maintenance list.");
            node.node("messages", "maintenance_player_removed").set("&cPlayer {player} removed from the maintenance list.");
            node.node("messages", "report_usage").set("&cUsage: /report <nick> <reason>");
            node.node("messages", "report_player_not_found").set("&cPlayer {player} not found");
            node.node("messages", "report_sent").set("&aYour report for the player {target} was sent");
            node.node("messages", "report_hover").set("&bClick to teleport");
            node.node("messages", "report_cooldown").set("&cYou have {time}s before using /report again");
            node.node("messages", "report_webhook_error").set("&cError trying to send discord report webhook");
            node.node("messages", "goto_usage").set("&cUsage: /goto <player>");
            node.node("messages", "goto_player_not_found").set("&cPlayer {player} not found");
            node.node("messages", "goto_server_not_found").set("&cServer could not be found");
            node.node("messages", "goto_same_server").set("&cYou are currently on the same server as {player}");
            node.node("messages", "goto_connecting").set("&aConnecting with {player} server");
            node.node("messages", "find_usage").set("&cUsage: /find <player>");
            node.node("messages", "find_player_not_found").set("&cPlayer {player} not found");
            node.node("messages", "find_unknown").set("Unknown");
            node.node("messages", "find_where").set("&b{player} &eis on &b{server}");

            loader.save(node);
        } catch (SerializationException e) {
            System.err.println("Error al serializar la configuración.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo de configuración.");
            e.printStackTrace();
        }
    }

    public String getAlertPrefix() {
        return config != null ? config.getAlertPrefix() : "&7[&b&lSERVER&7]";
    }

    public void setAlertPrefix(String prefix) {
        if (config != null) {
            config.setAlertPrefix(prefix);
            saveConfig();
        }
    }

    public String getMessage(String key) {
        try {
            ConfigurationNode node = loader.load();
            return node.node("messages", key).getString("&cMessage not found: " + key);
        } catch (IOException e) {
            e.printStackTrace();
            return "&cError loading message: " + key;
        }
    }

    public String getString(String key) {
        try {
            ConfigurationNode node = loader.load();
            String[] parts = key.split("\\.");
            for (String part : parts) {
                node = node.node(part);
            }
            String result = node.getString();
            return (result != null && !result.isBlank()) ? result : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean getBoolean(String key) {
        try {
            ConfigurationNode node = loader.load();
            for (String part : key.split("\\.")) {
                node = node.node(part);
            }
            return node.getBoolean(false);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getStringList(String key) {
        try {
            ConfigurationNode node = loader.load();
            for (String part : key.split("\\.")) {
                node = node.node(part);
            }
            return node.getList(String.class, List.of());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public Component getMotd() {
        try {
            ConfigurationNode node = loader.load();
            String line1 = node.node("motd", "line1").getString("&aWelcome to this Velocity Server!");
            String line2 = node.node("motd", "line2").getString("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");

            // Serializadores
            LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
            MiniMessage miniMessage = MiniMessage.miniMessage();

            // Convertir
            Component component1 = line1.contains("<") ? miniMessage.deserialize(line1) : legacySerializer.deserialize(line1);
            Component component2 = line2.contains("<") ? miniMessage.deserialize(line2) : legacySerializer.deserialize(line2);

            return Component.text()
                    .append(component1)
                    .append(Component.newline())
                    .append(component2)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();

            String line1 = "&aWelcome to this Velocity Server!";
            String line2 = "<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>";

            // Serializadores
            LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
            MiniMessage miniMessage = MiniMessage.miniMessage();

            // Convertir
            Component component1 = line1.contains("<") ? miniMessage.deserialize(line1) : legacySerializer.deserialize(line1);
            Component component2 = line2.contains("<") ? miniMessage.deserialize(line2) : legacySerializer.deserialize(line2);

            return Component.text()
                    .append(component1)
                    .append(Component.newline())
                    .append(component2)
                    .build();
        }
    }

    public boolean isMaintenanceMode() {
        try {
            ConfigurationNode node = loader.load();
            return node.node("maintenance", "active").getBoolean(false);
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Si hay un error, devolver `false` por defecto.
        }
    }

    public List<String> getAllowedPlayers() {
        try {
            ConfigurationNode node = loader.load();
            return node.node("maintenance", "allowed").getList(String.class, List.of());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of(); // Si hay un error, devolver una lista vacía.
        }
    }

    public Component getMaintenanceMotd() {
        try {
            ConfigurationNode node = loader.load();
            String line1 = node.node("maintenance", "motd", "line1").getString("&cServer under maintenance!");
            String line2 = node.node("maintenance", "motd", "line2").getString("<bold><gradient:red:yellow>Try again later</gradient></bold>");

            // Serializadores
            LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
            MiniMessage miniMessage = MiniMessage.miniMessage();

            // Convertir
            Component component1 = line1.contains("<") ? miniMessage.deserialize(line1) : legacySerializer.deserialize(line1);
            Component component2 = line2.contains("<") ? miniMessage.deserialize(line2) : legacySerializer.deserialize(line2);

            return Component.text()
                    .append(component1)
                    .append(Component.newline())
                    .append(component2)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();

            String line1 = "&cServer under maintenance!";
            String line2 = "<bold><gradient:red:yellow>Try again later</gradient></bold>";

            // Serializadores
            LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();
            MiniMessage miniMessage = MiniMessage.miniMessage();

            // Convertir
            Component component1 = line1.contains("<") ? miniMessage.deserialize(line1) : legacySerializer.deserialize(line1);
            Component component2 = line2.contains("<") ? miniMessage.deserialize(line2) : legacySerializer.deserialize(line2);

            return Component.text()
                    .append(component1)
                    .append(Component.newline())
                    .append(component2)
                    .build();
        }
    }

    public void setMaintenanceMode(boolean active) {
        try {
            ConfigurationNode node = loader.load();
            node.node("maintenance", "active").set(active);
            loader.save(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAllowedPlayers(List<String> players) {
        try {
            ConfigurationNode node = loader.load();
            node.node("maintenance", "allowed").setList(String.class, players);
            loader.save(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}