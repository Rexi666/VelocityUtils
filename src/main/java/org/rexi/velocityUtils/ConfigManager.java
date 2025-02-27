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

                if (node.node("messages", "no_permission").empty()) {
                    node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
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

            node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
            node.node("messages", "alert_usage").set("&cUsage: /alert <message>");
            node.node("messages", "configuration_reloaded").set("&aConfiguration reloaded successfully!");
            node.node("messages", "velocityutils_usage").set("&cUsage: /velocityutils reload");

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
}