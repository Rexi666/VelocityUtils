package org.rexi.velocityUtils.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.rexi.velocityUtils.utils.DefaultFontInfo;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConfigManager {

    private final Path configPath;
    private final Path messagePath;

    private final YamlConfigurationLoader configLoader;
    private final YamlConfigurationLoader messagesLoader;

    public ConfigManager() {
        // Define la carpeta del plugin dentro de "plugins/"
        Path pluginFolder = Paths.get("plugins", "VelocityUtils");

        // Asegura que la carpeta del plugin existe
        if (!Files.exists(pluginFolder)) {
            try {
                Files.createDirectories(pluginFolder);

                File file = new File("plugins/VelocityUtils", "server-icon.png");

                if (!file.exists()) {
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream("server-icon.png")) {
                        if (in == null) {
                            throw new FileNotFoundException("server-icon.png");
                        }

                        Files.copy(in, file.toPath());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Ruta correcta dentro de "plugins/VelocityUtils/"
        this.configPath = pluginFolder.resolve("config.yml");
        this.messagePath = pluginFolder.resolve("messages.yml");

        // 💡 Configura el YAML para evitar inline objects
        this.configLoader = YamlConfigurationLoader.builder()
                .path(configPath)
                .nodeStyle(NodeStyle.BLOCK) // 🔥 Evita la serialización en una sola línea
                .build();

        this.messagesLoader = YamlConfigurationLoader.builder()
                .path(messagePath)
                .nodeStyle(NodeStyle.BLOCK) // 🔥 Evita la serialización en una sola línea
                .build();
    }

    ////////////
    // CONFIG //
    ////////////

    public void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                // Si el archivo no existe, crea uno con valores por defecto
                saveConfig();
            } else {
                ConfigurationNode node = configLoader.load();

                // Cargar las configs si no existen

                if (node.node("database", "type").empty()) {
                    node.node("database", "type").set("sqlite");
                }
                if (node.node("database", "mysql", "host").empty()) {
                    node.node("database", "mysql", "host").set("localhost");
                }
                if (node.node("database", "mysql", "port").empty()) {
                    node.node("database", "mysql", "port").set(3306);
                }
                if (node.node("database", "mysql", "database").empty()) {
                    node.node("database", "mysql", "database").set("velocityutils");
                }
                if (node.node("database", "mysql", "username").empty()) {
                    node.node("database", "mysql", "username").set("root");
                }
                if (node.node("database", "mysql", "password").empty()) {
                    node.node("database", "mysql", "password").set("");
                }

                if (node.node("alert", "enabled").empty()) {
                    node.node("alert", "enabled").set(true);
                }
                if (node.node("alert", "prefix").empty()) {
                    node.node("alert", "prefix").set("&7[&b&lSERVER&7]");
                }
                if (node.node("alert", "sound").empty()) {
                    node.node("alert", "sound").set("BLOCK_NOTE_BLOCK_PLING");
                }

                if (node.node("alert", "message").empty()) {
                    if (!node.node("alert","prefix").empty()) {
                        node.node("alert", "message").setList(String.class, List.of(
                                "&f-----------------------------",
                                node.node("alert", "prefix").getString()+ " &r{message}",
                                "&f-----------------------------"));
                    } else {
                        node.node("alert", "message").setList(String.class, List.of(
                                "{center} &f-----------------------------",
                                "{center} &7[&b&lSERVER&7] &r{message}",
                                "{center} &f-----------------------------"));
                    }
                }

                if (node.node("alert", "title", "enabled").empty()) {
                    node.node("alert", "title", "enabled").set(true);
                }
                if (node.node("alert", "title", "title").empty()) {
                    node.node("alert", "title", "title").set("&7[&b&lSERVER&7]");
                }
                if (node.node("alert", "title", "subtitle").empty()) {
                    node.node("alert", "title", "subtitle").set("{message}");
                }
                if (node.node("alert", "title", "durations", "fade_in").empty()) {
                    node.node("alert", "title", "durations", "fade_in").set(20);
                }
                if (node.node("alert", "title", "durations", "stay").empty()) {
                    node.node("alert", "title", "durations", "stay").set(60);
                }
                if (node.node("alert", "title", "durations", "fade_out").empty()) {
                    node.node("alert", "title", "durations", "fade_out").set(20);
                }
                if (node.node("alert", "actionbar", "enabled").empty()) {
                    node.node("alert", "actionbar", "enabled").set(true);
                }
                if (node.node("alert", "actionbar", "message").empty()) {
                    node.node("alert", "actionbar", "message").set("&7[&b&lSERVER&7] &r{message}");
                }
                if (node.node("alert", "bossbar", "enabled").empty()) {
                    node.node("alert", "bossbar", "enabled").set(true);
                }
                if (node.node("alert", "bossbar", "message").empty()) {
                    node.node("alert", "bossbar", "message").set("&7[&b&lSERVER&7] &r{message}");
                }
                if (node.node("alert", "bossbar", "color").empty()) {
                    node.node("alert", "bossbar", "color").set("BLUE");
                }
                if (node.node("alert", "bossbar", "overlay").empty()) {
                    node.node("alert", "bossbar", "overlay").set("PROGRESS");
                }
                if (node.node("alert", "bossbar", "duration").empty()) {
                    node.node("alert", "bossbar", "duration").set(5);
                }

                if (node.node("motd", "enabled").empty()) {
                    node.node("motd", "enabled").set(true);
                }
                if (node.node("motd", "line1").empty()) {
                    node.node("motd", "line1").set("&aWelcome to this Velocity Server!");
                }
                if (node.node("motd", "line2").empty()) {
                    node.node("motd", "line2").set("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");
                }

                if (node.node("maintenance", "enabled").empty()) {
                    node.node("maintenance", "enabled").set(true);
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
                if (!node.node("maintenance", "allowed").empty()) {
                    node.node("maintenance", "allowed").set(null); // Eliminar la lista, ahora se usa permiso
                }

                if (node.node("report", "enabled").empty()) {
                    node.node("report", "enabled").set(true);
                }
                if (node.node("report", "teleport_on_click").empty()) {
                    node.node("report", "teleport_on_click").set(true);
                }
                if (node.node("report", "message").empty()) {
                    node.node("report", "message").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eNew Report from {player}!",
                            "{center} &fReported: &c{reported}",
                            "{center} &fReason: &b{reason}",
                            "{center} &fServer: &b{server}",
                            "{center} &eClick to teleport",
                            "{center} &f-----------------------------"));
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

                if (node.node("helpop", "enabled").empty()) {
                    node.node("helpop", "enabled").set(true);
                }
                if (node.node("helpop", "teleport_on_click").empty()) {
                    node.node("helpop", "teleport_on_click").set(true);
                }
                if (node.node("helpop", "message").empty()) {
                    node.node("helpop", "message").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eNew Help Request from {player}!",
                            "{center} &fReason: &b{reason}",
                            "{center} &fServer: &b{server}",
                            "{center} &eClick to teleport",
                            "{center} &f-----------------------------"));
                }
                if (node.node("helpop", "discord_hook", "enabled").empty()) {
                    node.node("helpop", "discord_hook", "enabled").set(false);
                }
                if (node.node("helpop", "discord_hook", "url").empty()) {
                    node.node("helpop", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("helpop", "discord_hook", "avatar").empty()) {
                    node.node("helpop", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("helpop", "discord_hook", "username").empty()) {
                    node.node("helpop", "discord_hook", "username").set("VelocityUtils");
                }
                if (node.node("helpop", "discord_hook", "color_rgb").empty()) {
                    node.node("helpop", "discord_hook", "color_rgb").set("227,230,14");
                }
                if (node.node("helpop", "discord_hook", "title").empty()) {
                    node.node("helpop", "discord_hook", "title").set("\uD83D\uDCE2Help Request\uD83D\uDCE2");
                }
                if (node.node("helpop", "discord_hook", "message").empty()) {
                    node.node("helpop", "discord_hook", "message").set(String.class, """
      📢 **New Help Request from {player}**
      📄 **Reason:** {reason}
      🌍 **Server:** {server}
      """);
                }

                if (node.node("staffchat", "enabled").empty()) {
                    node.node("staffchat", "enabled").set(true);
                }
                if (node.node("staffchat", "discord_hook", "enabled").empty()) {
                    node.node("staffchat", "discord_hook", "enabled").set(false);
                }
                if (node.node("staffchat", "discord_hook", "url").empty()) {
                    node.node("staffchat", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("staffchat", "discord_hook", "avatar").empty()) {
                    node.node("staffchat", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("staffchat", "discord_hook", "username").empty()) {
                    node.node("staffchat", "discord_hook", "username").set("VelocityUtils");
                }
                if (node.node("staffchat", "discord_hook", "color_rgb").empty()) {
                    node.node("staffchat", "discord_hook", "color_rgb").set("20,200,240");
                }
                if (node.node("staffchat", "discord_hook", "title").empty()) {
                    node.node("staffchat", "discord_hook", "title").set("\uD83D\uDDE8\uFE0FStaff Chat\uD83D\uDDE8\uFE0F");
                }
                if (node.node("staffchat", "discord_hook", "message").empty()) {
                    node.node("staffchat", "discord_hook", "message").set(String.class, """
      🗨️ **Staff Chat from {player}**
      🌍 **Server:** {server}
      💬 **Message:** {message}
      """);
                }

                if (node.node("adminchat", "enabled").empty()) {
                    node.node("adminchat", "enabled").set(true);
                }
                if (node.node("adminchat", "discord_hook", "enabled").empty()) {
                    node.node("adminchat", "discord_hook", "enabled").set(false);
                }
                if (node.node("adminchat", "discord_hook", "url").empty()) {
                    node.node("adminchat", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("adminchat", "discord_hook", "avatar").empty()) {
                    node.node("adminchat", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("adminchat", "discord_hook", "username").empty()) {
                    node.node("adminchat", "discord_hook", "username").set("VelocityUtils");
                }
                if (node.node("adminchat", "discord_hook", "color_rgb").empty()) {
                    node.node("adminchat", "discord_hook", "color_rgb").set("196,3,184");
                }
                if (node.node("adminchat", "discord_hook", "title").empty()) {
                    node.node("adminchat", "discord_hook", "title").set("\uD83D\uDDE8\uFE0FAdmin Chat\uD83D\uDDE8\uFE0F");
                }
                if (node.node("adminchat", "discord_hook", "message").empty()) {
                    node.node("adminchat", "discord_hook", "message").set(String.class, """
      🗨️ **Admin Chat from {player}**
      🌍 **Server:** {server}
      💬 **Message:** {message}
      """);
                }

                if (node.node("stafftime", "week_start").empty()) {
                    node.node("stafftime", "week_start").set("MONDAY");
                }
                if (node.node("stafftime", "discord_hook", "enabled").empty()) {
                    node.node("stafftime", "discord_hook", "enabled").set(false);
                }
                if (node.node("stafftime", "discord_hook", "join", "enabled").empty()) {
                    node.node("stafftime", "discord_hook", "join", "enabled").set(true);
                }
                if (node.node("stafftime", "discord_hook", "join", "url").empty()) {
                    node.node("stafftime", "discord_hook", "join", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("stafftime", "discord_hook", "join", "avatar").empty()) {
                    node.node("stafftime", "discord_hook", "join", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("stafftime", "discord_hook", "join", "username").empty()) {
                    node.node("stafftime", "discord_hook", "join", "username").set("VelocityUtils");
                }
                if (node.node("stafftime", "discord_hook", "join", "color_rgb").empty()) {
                    node.node("stafftime", "discord_hook", "join", "color_rgb").set("45,255,0");
                }
                if (node.node("stafftime", "discord_hook", "join", "title").empty()) {
                    node.node("stafftime", "discord_hook", "join", "title").set("\uD83D\uDD52Staff Join\uD83D\uDD52");
                }
                if (node.node("stafftime", "discord_hook", "join", "message").empty()) {
                    node.node("stafftime", "discord_hook", "join", "message").set(String.class, """
      🕒 **{player}** joined the server
      """);
                }
                if (node.node("stafftime", "discord_hook", "change", "enabled").empty()) {
                    node.node("stafftime", "discord_hook", "change", "enabled").set(true);
                }
                if (node.node("stafftime", "discord_hook", "change", "url").empty()) {
                    node.node("stafftime", "discord_hook", "change", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("stafftime", "discord_hook", "change", "avatar").empty()) {
                    node.node("stafftime", "discord_hook", "change", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("stafftime", "discord_hook", "change", "username").empty()) {
                    node.node("stafftime", "discord_hook", "change", "username").set("VelocityUtils");
                }
                if (node.node("stafftime", "discord_hook", "change", "color_rgb").empty()) {
                    node.node("stafftime", "discord_hook", "change", "color_rgb").set("255,255,0");
                }
                if (node.node("stafftime", "discord_hook", "change", "title").empty()) {
                    node.node("stafftime", "discord_hook", "change", "title").set("\uD83D\uDD52Staff Change\uD83D\uDD52");
                }
                if (node.node("stafftime", "discord_hook", "change", "message").empty()) {
                    node.node("stafftime", "discord_hook", "change", "message").set(String.class, """
      🕒 **{player}** changed the server
      📋 {from} > {to}
      """);
                }
                if (node.node("stafftime", "discord_hook", "leave", "enabled").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "enabled").set(true);
                }
                if (node.node("stafftime", "discord_hook", "leave", "url").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
                }
                if (node.node("stafftime", "discord_hook", "leave", "avatar").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
                }
                if (node.node("stafftime", "discord_hook", "leave", "username").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "username").set("VelocityUtils");
                }
                if (node.node("stafftime", "discord_hook", "leave", "color_rgb").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "color_rgb").set("255,0,0");
                }
                if (node.node("stafftime", "discord_hook", "leave", "title").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "title").set("\uD83D\uDD52Staff Leave\uD83D\uDD52");
                }
                if (node.node("stafftime", "discord_hook", "leave", "message").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "message").set(String.class, """
      🕒 **{player}** left the server
      📅 Time today: {time_daily}
      📅 Time this week: {time_weekly}
      📅 Time this month: {time_monthly}
      📋 {time} this session
      🖊️ Servers this session:
      {serverstime}
      """);
                }
                if (node.node("stafftime", "discord_hook", "leave", "serverstime").empty()) {
                    node.node("stafftime", "discord_hook", "leave", "serverstime").set("- {server} - {time}");
                }
                if (node.node("stafftime", "command", "enabled").empty()) {
                    node.node("stafftime", "command", "enabled").set(true);
                }
                if (node.node("stafftime", "command", "no_type").empty()) {
                    node.node("stafftime", "command", "no_type").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eStaff Time from {player}",
                            "{center} &fToday: &b{day}",
                            "{center} &fWeek: &b{week}",
                            "{center} &fMonth: &b{month}",
                            "{center} &f-----------------------------"));
                }
                if (node.node("stafftime", "command", "type").empty()) {
                    node.node("stafftime", "command", "type").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eStaff Time from {player} ({type})",
                            "{center} &f{type}: &b{time}",
                            "{center} &f-----------------------------"));
                }
                if (node.node("stafftime", "command", "day").empty()) {
                    node.node("stafftime", "command", "day").set("Day");
                }
                if (node.node("stafftime", "command", "week").empty()) {
                    node.node("stafftime", "command", "week").set("Week");
                }
                if (node.node("stafftime", "command", "month").empty()) {
                    node.node("stafftime", "command", "month").set("Month");
                }

                if (node.node("vlist", "enabled").empty()) {
                    node.node("vlist", "enabled").set(true);
                }
                if (node.node("vlist", "default_mode").empty()) {
                    node.node("vlist", "default_mode").set("server");
                }
                if (node.node("vlist", "server", "message").empty()) {
                    node.node("vlist", "server", "message").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eThere are {count} players online",
                            "{servercount}",
                            "{center} &f-----------------------------"));
                }
                if (node.node("vlist", "server", "servercount").empty()) {
                    node.node("vlist", "server", "servercount").set("{center} &7[&b{server} &7(&b{count}&7)] - &f{players}");
                }
                if (node.node("vlist", "rank", "message").empty()) {
                    node.node("vlist", "rank", "message").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &eThere are {count} players online",
                            "{rankcount}",
                            "{center} &f-----------------------------"));
                }
                if (node.node("vlist", "rank", "rankcount").empty()) {
                    node.node("vlist", "rank", "rankcount").set("{center} &7[&b{rank} &7(&b{count}&7)] - &f{players}");
                }
                if (node.node("movecommands").empty()) {
                    node.node("movecommands", "enabled").set(true);
                    node.node("movecommands", "lobby", "server").setList(String.class, List.of(
                            "lobby1",
                            "lobby2"));
                    node.node("movecommands", "lobby", "message").set("&aYou have been moved to a &blobby");
                    node.node("movecommands", "survival", "server").setList(String.class, List.of(
                            "survival"));
                    node.node("movecommands", "survival", "message").set("&aYou have been moved to the &dsurvival");

                }
                if (node.node("messagescommands").empty()) {
                    node.node("messagescommands", "enabled").set(true);
                    node.node("messagescommands", "discord", "message").setList(String.class, List.of(
                            "{center} &f-----------------------------",
                            "{center} &fJoin ur &9discord",
                            "{center} &9https://discord.com/invite/a3zkKtrjTr",
                            "{center} &f-----------------------------"));
                    node.node("messagescommands", "discord", "click_action").set("OPEN_URL");
                    node.node("messagescommands", "discord", "action").set("https://discord.com/invite/a3zkKtrjTr");
                    node.node("messagescommands", "discord", "hover").set("&9Click to join ur discord");
                    node.node("messagescommands", "discord", "sound").set("UI_BUTTON_CLICK");

                    node.node("messagescommands", "newgamemode", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&6New Game Mode released",
                            "&#c3d600&lJ&#c6c900&lO&#cabd00&lI&#cdb000&lN &#d1a400&lT&#d49700&lH&#d78a00&lE &#db7e00&lN&#de7100&lE&#e26500&lW &#e55800&lS&#e94c00&lU&#ec3f00&lR&#ef3200&lV&#f32600&lI&#f61900&lV&#fa0d00&lA&#fd0000&lL",
                            "&f-----------------------------"));
                    node.node("messagescommands", "newgamemode", "click_action").set("RUN_COMMAND");
                    node.node("messagescommands", "newgamemode", "action").set("/survival");
                    node.node("messagescommands", "newgamemode", "hover").set("&6Click to join ur new survival");
                    node.node("messagescommands", "newgamemode", "sound").set("ENTITY_PLAYER_LEVELUP");

                    node.node("messagescommands", "rules", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&6Remember to read all the server rules",
                            "&f-----------------------------"));
                    node.node("messagescommands", "rules", "click_action").set("NONE");
                    node.node("messagescommands", "rules", "sound").set("");
                }

                if (node.node("find", "enabled").empty()) {
                    node.node("find", "enabled").set(true);
                }
                if (node.node("goto", "enabled").empty()) {
                    node.node("goto", "enabled").set(true);
                }
                if (node.node("stafflist", "enabled").empty()) {
                    node.node("stafflist", "enabled").set(true);
                }

                if (node.node("staffjoin", "enabled").empty()) {
                    node.node("staffjoin", "enabled").set(true);
                }
                if (node.node("staffjoin", "join_message").empty()) {
                    node.node("staffjoin", "join_message").set("&b&lStaff - &a{rank} {player} has joined the server");
                }
                if (node.node("staffjoin", "leave_message").empty()) {
                    node.node("staffjoin", "leave_message").set("&b&lStaff - &c{rank} {player} has left the server");
                }
                if (node.node("staffjoin", "change_message").empty()) {
                    node.node("staffjoin", "change_message").set("&b&lStaff - &e{rank} {player} has changed the server to &b{server}");
                }

                if (node.node("stream", "enabled").empty()) {
                    node.node("stream", "enabled").set(true);
                }

                if (node.node("stream", "message").empty()) {
                    node.node("stream", "message").set(List.of(
                            "{center} &f-----------------------------",
                            "{center} {rank} &b{player} &fis now &6streaming",
                            "{center} &b{url}",
                            "{center} &f-----------------------------"
                    ));
                } else if (!node.node("stream", "message").isList()) {
                    node.node("stream", "message").set(List.of(
                            node.node("stream", "message").getString("")
                    ));
                }

                if (node.node("stream", "hover_enabled").empty()) {
                    node.node("stream", "hover_enabled").set(true);
                }
                if (node.node("stream", "hover").empty()) {
                    node.node("stream", "hover").set("&bClick to watch the stream");
                }
                if (node.node("stream", "cooldown_seconds").empty()) {
                    node.node("stream", "cooldown_seconds").set(300);
                }
                if (node.node("stream", "whitelist").empty()) {
                    node.node("stream", "whitelist").set(true);
                }
                if (node.node("stream", "whitelist_links").empty()) {
                    node.node("stream", "whitelist_links").setList(String.class, List.of(
                            "https://www.twitch.tv/",
                            "https://www.youtube.com/"));
                }
                if (node.node("serverexecute", "enabled").empty()) {
                    node.node("serverexecute", "enabled").set(true);
                }
                if (node.node("togglesc", "enabled").empty()) {
                    node.node("togglesc", "enabled").set(true);
                }

                if (node.node("ban_system", "enabled").empty()) {
                    node.node("ban_system", "enabled").set(true);
                }
                if (node.node("ban_system", "commands", "vban").empty()) {
                    node.node("ban_system", "commands", "vban").set(true);
                }
                if (node.node("ban_system", "commands", "vbanip").empty()) {
                    node.node("ban_system", "commands", "vbanip").set(true);
                }
                if (node.node("ban_system", "commands", "vunban").empty()) {
                    node.node("ban_system", "commands", "vunban").set(true);
                }
                if (node.node("ban_system", "commands", "vkick").empty()) {
                    node.node("ban_system", "commands", "vkick").set(true);
                }
                if (node.node("ban_system", "commands", "vcheckban").empty()) {
                    node.node("ban_system", "commands", "vcheckban").set(true);
                }
                if (node.node("ban_system", "default_ban_reason").empty()) {
                    node.node("ban_system", "default_ban_reason").set("No reason specified");
                }
                if (node.node("ban_system", "console").empty()) {
                    node.node("ban_system", "console").set("Console");
                }
                if (node.node("ban_system", "screen_messages", "ban").empty()) {
                    node.node("ban_system", "screen_messages", "ban").set(String.class, """
      &f-----------------------------
      &cYou have been banned from the network!
      &fPlayer: &b{player}
      &fBanned by: &b{banned_by}
      &fBanned at: &b{banned_at}
      &fReason: &b{reason}
      &f-----------------------------
      """);
                }
                if (node.node("ban_system", "screen_messages", "kick").empty()) {
                    node.node("ban_system", "screen_messages", "kick").set(String.class, """
      &f-----------------------------
      &cYou have been kicked from the network!
      &fPlayer: &b{player}
      &fKicked by: &b{kicked_by}
      &fReason: &b{reason}
      &f-----------------------------
      """);
                }

                if (node.node("brand", "enabled").empty()) {
                    node.node("brand", "enabled").set(true);
                }
                if (node.node("brand", "text").empty()) {
                    node.node("brand", "text").set("&3&l&nVelocityUtils&r &c| &6by Rexi666");
                }

                if (node.node("regular_alerts", "enabled").empty()) {
                    node.node("regular_alerts", "enabled").set(true);
                }
                if (node.node("regular_alerts", "delay_seconds").empty()) {
                    node.node("regular_alerts", "delay_seconds").set(300);
                }
                if (node.node("regular_alerts", "sound").empty()) {
                    node.node("regular_alerts", "sound").set("ENTITY_EXPERIENCE_ORB_PICKUP");
                }

                if (node.node("regular_alerts", "alerts").empty()) {
                    node.node("regular_alerts", "alerts", "discord", "message").set(List.of(
                            "{center} &f-----------------------------",
                            "{center} &9Join our &bDiscord&9 for news and updates!",
                            "{center} &bhttps://discord.myserver.com",
                            "{center} &f-----------------------------"
                    ));
                    node.node("regular_alerts", "alerts", "discord", "click_action").set("OPEN_URL");
                    node.node("regular_alerts", "alerts", "discord", "action").set("https://discord.myserver.com");
                    node.node("regular_alerts", "alerts", "discord", "hover").set("&bClick to join our Discord");
                    node.node("regular_alerts", "alerts", "store", "message").set(List.of(
                            "&f-----------------------------",
                            "&9Visit our &bStore&9 for ranks and perks!",
                            "{center} &bhttps://store.myserver.com",
                            "&f-----------------------------"
                    ));
                    node.node("regular_alerts", "alerts", "store", "click_action").set("OPEN_URL");
                    node.node("regular_alerts", "alerts", "store", "action").set("https://store.myserver.com");
                    node.node("regular_alerts", "alerts", "store", "hover").set("&bClick to open our store");
                }

                if (node.node("tebex_link", "enabled").empty()) {
                    node.node("tebex_link", "enabled").set(true);
                }
                if (node.node("tebex_link", "secret").empty()) {
                    node.node("tebex_link", "secret").set("YOUR_TEBEX_SECRET_KEY");
                }
                if (node.node("tebex_link", "refresh_minutes").empty()) {
                    node.node("tebex_link", "refresh_minutes").set(30);
                }

                if (node.node("serverwhitelist", "enabled").empty()) {
                    node.node("serverwhitelist", "enabled").set(true);
                }
                if (node.node("serverwhitelist", "active_servers").empty()) {
                    node.node("serverwhitelist", "active_servers").set(List.of(
                            "whitelisted_server1",
                            "whitelisted_server2"
                    ));
                }
                if (node.node("serverwhitelist", "sound").empty()) {
                    node.node("serverwhitelist", "sound").set("ENTITY_VILLAGER_NO");
                }
                if (node.node("serverwhitelist", "message").empty()) {
                    node.node("serverwhitelist", "message").set(List.of(
                            "{center} &f-----------------------------",
                            "{center} &cThis server is &lwhitelisted&c!",
                            "{center} &fPlease wait until you are allowed to join.",
                            "{center} &f-----------------------------"
                    ));
                }
                if (node.node("serverwhitelist", "title", "enabled").empty()) {
                    node.node("serverwhitelist", "title", "enabled").set(true);
                }
                if (node.node("serverwhitelist", "title", "title").empty()) {
                    node.node("serverwhitelist", "title", "title").set("&cServer Whitelisted");
                }
                if (node.node("serverwhitelist", "title", "subtitle").empty()) {
                    node.node("serverwhitelist", "title", "subtitle").set("&7Wait until you are allowed to join.");
                }
                if (node.node("serverwhitelist", "title", "durations", "fade_in").empty()) {
                    node.node("serverwhitelist", "title", "durations", "fade_in").set(20);
                }
                if (node.node("serverwhitelist", "title", "durations", "stay").empty()) {
                    node.node("serverwhitelist", "title", "durations", "stay").set(60);
                }
                if (node.node("serverwhitelist", "title", "durations", "fade_out").empty()) {
                    node.node("serverwhitelist", "title", "durations", "fade_out").set(20);
                }
                if (node.node("serverwhitelist", "actionbar", "enabled").empty()) {
                    node.node("serverwhitelist", "actionbar", "enabled").set(true);
                }
                if (node.node("serverwhitelist", "actionbar", "message").empty()) {
                    node.node("serverwhitelist", "actionbar", "message").set("&cServer Whitelisted");
                }
                if (node.node("serverwhitelist", "bossbar", "enabled").empty()) {
                    node.node("serverwhitelist", "bossbar", "enabled").set(true);
                }
                if (node.node("serverwhitelist", "bossbar", "message").empty()) {
                    node.node("serverwhitelist", "bossbar", "message").set("&cServer Whitelisted");
                }
                if (node.node("serverwhitelist", "bossbar", "color").empty()) {
                    node.node("serverwhitelist", "bossbar", "color").set("RED");
                }
                if (node.node("serverwhitelist", "bossbar", "overlay").empty()) {
                    node.node("serverwhitelist", "bossbar", "overlay").set("PROGRESS");
                }
                if (node.node("serverwhitelist", "bossbar", "duration").empty()) {
                    node.node("serverwhitelist", "bossbar", "duration").set(5);
                }

                if (node.node("server_icon", "enabled").empty()) {
                    node.node("server_icon", "enabled").set(false);
                }
                if (node.node("server_icon", "file").empty()) {
                    node.node("server_icon", "file").set("server-icon.png");
                }

                if (node.node("private_messages", "enabled").empty()) {
                    node.node("private_messages", "enabled").set(true);
                }
                if (node.node("private_messages", "vmsg").empty()) {
                    node.node("private_messages", "vmsg").set(true);
                }
                if (node.node("private_messages", "vreply").empty()) {
                    node.node("private_messages", "vreply").set(true);
                }
                if (node.node("private_messages", "vignore").empty()) {
                    node.node("private_messages", "vignore").set(true);
                }
                if (node.node("private_messages", "vspy").empty()) {
                    node.node("private_messages", "vspy").set(true);
                }

                if (node.node("disabled_features_servers").empty()) {
                    node.node("disabled_features_servers").set(List.of(
                            "auth1",
                            "auth2"
                    ));
                }

                // Guardar en caso de que se hayan agregado valores predeterminados
                configLoader.save(node);
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
            ConfigurationNode node = configLoader.createNode();

            // 💡 Crear la estructura correctamente sin inline mapping
            node.node("database", "type").set("sqlite");
            node.node("database", "mysql", "host").set("localhost");
            node.node("database", "mysql", "port").set(3306);
            node.node("database", "mysql", "database").set("velocityutils");
            node.node("database", "mysql", "username").set("root");
            node.node("database", "mysql", "password").set("");

            node.node("alert", "enabled").set(true);
            node.node("alert", "sound").set("BLOCK_NOTE_BLOCK_PLING");
            node.node("alert", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &7[&b&lSERVER&7] &r{message}",
                    "{center} &f-----------------------------"));
            node.node("alert", "title", "enabled").set(true);
            node.node("alert", "title", "title").set("&7[&b&lSERVER&7]");
            node.node("alert", "title", "subtitle").set("{message}");
            node.node("alert", "title", "durations", "fade_in").set(20);
            node.node("alert", "title", "durations", "stay").set(60);
            node.node("alert", "title", "durations", "fade_out").set(20);
            node.node("alert", "actionbar", "enabled").set(true);
            node.node("alert", "actionbar", "message").set("&7[&b&lSERVER&7] &r{message}");
            node.node("alert", "bossbar", "enabled").set(true);
            node.node("alert", "bossbar", "message").set("&7[&b&lSERVER&7] &r{message}");
            node.node("alert", "bossbar", "color").set("BLUE");
            node.node("alert", "bossbar", "overlay").set("PROGRESS");
            node.node("alert", "bossbar", "duration").set(5);


            // Agregar mensajes predeterminados
            node.node("motd", "enabled").set(true);
            node.node("motd", "line1").set("&aWelcome to this Velocity Server!");
            node.node("motd", "line2").set("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");

            node.node("maintenance", "enabled").set(true);
            node.node("maintenance", "active").set(false);
            node.node("maintenance", "motd", "line1").set("&cServer under maintenance!");
            node.node("maintenance", "motd", "line2").set("<bold><gradient:red:yellow>Try again later</gradient></bold>");

            node.node("report", "enabled").set(true);
            node.node("report", "teleport_on_click").set(true);
            node.node("report", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eNew Report from {player}!",
                    "{center} &fReported: &c{reported}",
                    "{center} &fReason: &b{reason}",
                    "{center} &fServer: &b{server}",
                    "{center} &eClick to teleport",
                    "{center} &f-----------------------------"));
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

            node.node("helpop", "enabled").set(true);
            node.node("helpop", "teleport_on_click").set(true);
            node.node("helpop", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eNew Help Request from {player}!",
                    "{center} &fReason: &b{reason}",
                    "{center} &fServer: &b{server}",
                    "{center} &eClick to teleport",
                    "{center} &f-----------------------------"));
            node.node("helpop", "discord_hook", "enabled").set(false);
            node.node("helpop", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("helpop", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("helpop", "discord_hook", "username").set("VelocityUtils");
            node.node("helpop", "discord_hook", "color_rgb").set("227,230,14");
            node.node("helpop", "discord_hook", "title").set("\uD83D\uDCE2Help Request\uD83D\uDCE2");
            node.node("helpop", "discord_hook", "message").set(String.class, """
      📢 **New Help Request from {player}**
      📄 **Reason:** {reason}
      🌍 **Server:** {server}
      """);

            node.node("staffchat", "enabled").set(true);
            node.node("staffchat", "discord_hook", "enabled").set(false);
            node.node("staffchat", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("staffchat", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("staffchat", "discord_hook", "username").set("VelocityUtils");
            node.node("staffchat", "discord_hook", "color_rgb").set("20,200,240");
            node.node("staffchat", "discord_hook", "title").set("\uD83D\uDDE8\uFE0FStaff Chat\uD83D\uDDE8\uFE0F");
            node.node("staffchat", "discord_hook", "message").set(String.class, """
      🗨️ **Staff Chat from {player}**
      🌍 **Server:** {server}
      💬 **Message:** {message}
      """);

            node.node("adminchat", "enabled").set(true);
            node.node("adminchat", "discord_hook", "enabled").set(false);
            node.node("adminchat", "discord_hook", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("adminchat", "discord_hook", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("adminchat", "discord_hook", "username").set("VelocityUtils");
            node.node("adminchat", "discord_hook", "color_rgb").set("196,3,184");
            node.node("adminchat", "discord_hook", "title").set("\uD83D\uDDE8\uFE0FAdmin Chat\uD83D\uDDE8\uFE0F");
            node.node("adminchat", "discord_hook", "message").set(String.class, """
      🗨️ **Admin Chat from {player}**
      🌍 **Server:** {server}
      💬 **Message:** {message}
      """);

            node.node("stafftime", "week_start").set("MONDAY");
            node.node("stafftime", "discord_hook", "enabled").set(false);
            node.node("stafftime", "discord_hook", "join", "enabled").set(true);
            node.node("stafftime", "discord_hook", "join", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("stafftime", "discord_hook", "join", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("stafftime", "discord_hook", "join", "username").set("VelocityUtils");
            node.node("stafftime", "discord_hook", "join", "color_rgb").set("45,255,0");
            node.node("stafftime", "discord_hook", "join", "title").set("🕒Staff Join🕒");
            node.node("stafftime", "discord_hook", "join", "message").set(String.class, """
      🕒 **{player}** joined the server
      """);
            node.node("stafftime", "discord_hook", "change", "enabled").set(true);
            node.node("stafftime", "discord_hook", "change", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("stafftime", "discord_hook", "change", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("stafftime", "discord_hook", "change", "username").set("VelocityUtils");
            node.node("stafftime", "discord_hook", "change", "color_rgb").set("255,255,0");
            node.node("stafftime", "discord_hook", "change", "title").set("🕒Staff Change🕒");
            node.node("stafftime", "discord_hook", "change", "message").set(String.class, """
      🕒 **{player}** changed the server
      📋 {from} > {to}
      """);
            node.node("stafftime", "discord_hook", "leave", "enabled").set(true);
            node.node("stafftime", "discord_hook", "leave", "url").set("https://discord.com/api/webhooks/xxxxxxxx/yyyyyyyyyyyy");
            node.node("stafftime", "discord_hook", "leave", "avatar").set("https://www.spigotmc.org/data/resource_icons/123/123517.jpg?1742847968");
            node.node("stafftime", "discord_hook", "leave", "username").set("VelocityUtils");
            node.node("stafftime", "discord_hook", "leave", "color_rgb").set("255,0,0");
            node.node("stafftime", "discord_hook", "leave", "title").set("🕒Staff Leave🕒");
            node.node("stafftime", "discord_hook", "leave", "message").set(String.class, """
      🕒 **{player}** left the server
      📅 Time today: {time_daily}
      📅 Time this week: {time_weekly}
      📅 Time this month: {time_monthly}
      📋 {time} this session
      🖊️ Servers this session:
      {serverstime}
      """);
            node.node("stafftime", "discord_hook", "leave", "serverstime").set("- {server} - {time}");
            node.node("stafftime", "command", "enabled").set(true);
            node.node("stafftime", "command", "no_type").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eStaff Time from {player}",
                    "{center} &fToday: &b{day}",
                    "{center} &fWeek: &b{week}",
                    "{center} &fMonth: &b{month}",
                    "{center} &f-----------------------------"));
            node.node("stafftime", "command", "type").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eStaff Time from {player} ({type})",
                    "{center} &f{type}: &b{time}",
                    "{center} &f-----------------------------"));
            node.node("stafftime", "command", "day").set("Day");
            node.node("stafftime", "command", "week").set("Week");
            node.node("stafftime", "command", "month").set("Month");

            node.node("vlist", "enabled").set(true);
            node.node("vlist", "default_mode").set("server");
            node.node("vlist", "server", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eThere are {count} players online",
                    "{servercount}",
                    "{center} &f-----------------------------"));
            node.node("vlist", "server", "servercount").set("{center} &7[&b{server} &7(&b{count}&7)] - &f{players}");
            node.node("vlist", "rank", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &eThere are {count} players online",
                    "{rankcount}",
                    "{center} &f-----------------------------"));
            node.node("vlist", "rank", "rankcount").set("{center} &7[&b{rank} &7(&b{count}&7)] - &f{players}");

            node.node("movecommands", "enabled").set(true);
            node.node("movecommands", "lobby", "server").setList(String.class, List.of(
                    "lobby1",
                    "lobby2"));
            node.node("movecommands", "lobby", "message").set("&aYou have been moved to a &blobby");
            node.node("movecommands", "survival", "server").setList(String.class, List.of(
                    "survival"));
            node.node("movecommands", "survival", "message").set("&aYou have been moved to the &dsurvival");

            node.node("messagescommands", "enabled").set(true);
            node.node("messagescommands", "discord", "message").setList(String.class, List.of(
                    "{center} &f-----------------------------",
                    "{center} &fJoin ur &9discord",
                    "{center} &9https://discord.com/invite/a3zkKtrjTr",
                    "{center} &f-----------------------------"));
            node.node("messagescommands", "discord", "click_action").set("OPEN_URL");
            node.node("messagescommands", "discord", "action").set("https://discord.com/invite/a3zkKtrjTr");
            node.node("messagescommands", "discord", "hover").set("&9Click to join ur discord");
            node.node("messagescommands", "discord", "sound").set("UI_BUTTON_CLICK");
            node.node("messagescommands", "newgamemode", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&6New Game Mode released",
                    "&#c3d600&lJ&#c6c900&lO&#cabd00&lI&#cdb000&lN &#d1a400&lT&#d49700&lH&#d78a00&lE &#db7e00&lN&#de7100&lE&#e26500&lW &#e55800&lS&#e94c00&lU&#ec3f00&lR&#ef3200&lV&#f32600&lI&#f61900&lV&#fa0d00&lA&#fd0000&lL",
                    "&f-----------------------------"));
            node.node("messagescommands", "newgamemode", "click_action").set("RUN_COMMAND");
            node.node("messagescommands", "newgamemode", "action").set("/survival");
            node.node("messagescommands", "newgamemode", "hover").set("&6Click to join ur new survival");
            node.node("messagescommands", "newgamemode", "sound").set("ENTITY_PLAYER_LEVELUP");
            node.node("messagescommands", "rules", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&6Remember to read all the server rules",
                    "&f-----------------------------"));
            node.node("messagescommands", "rules", "click_action").set("NONE");
            node.node("messagescommands", "rules", "sound").set("");

            node.node("find", "enabled").set(true);
            node.node("goto", "enabled").set(true);
            node.node("stafflist", "enabled").set(true);

            node.node("staffjoin", "enabled").set(true);
            node.node("staffjoin", "join_message").set("&b&lStaff - &a{rank} {player} has joined the server");
            node.node("staffjoin", "leave_message").set("&b&lStaff - &c{rank} {player} has left the server");
            node.node("staffjoin", "change_message").set("&b&lStaff - &e{rank} {player} has changed the server to &b{server}");

            node.node("stream", "enabled").set(true);
            node.node("stream", "message").set(List.of(
                    "{center} &f-----------------------------",
                    "{center} {rank} &b{player} &fis now &6streaming",
                    "{center} &b{url}",
                    "{center} &f-----------------------------"
            ));
            node.node("stream", "hover_enabled").set(true);
            node.node("stream", "hover").set("&bClick to watch the stream");
            node.node("stream", "cooldown_seconds").set(300);
            node.node("stream", "whitelist").set(true);
            node.node("stream", "whitelist_links").setList(String.class, List.of(
                    "https://www.twitch.tv/",
                    "https://www.youtube.com/"));

            node.node("serverexecute", "enabled").set(true);
            node.node("togglesc", "enabled").set(true);

            node.node("ban_system", "enabled").set(true);
            node.node("ban_system", "commands", "vban").set(true);
            node.node("ban_system", "commands", "vbanip").set(true);
            node.node("ban_system", "commands", "vunban").set(true);
            node.node("ban_system", "commands", "vkick").set(true);
            node.node("ban_system", "commands", "vcheckban").set(true);
            node.node("ban_system", "default_ban_reason").set("No reason specified");
            node.node("ban_system", "console").set("Console");
            node.node("ban_system", "screen_messages", "ban").set(String.class, """
      &f-----------------------------
      &cYou have been banned from the network!
      &fPlayer: &b{player}
      &fBanned by: &b{banned_by}
      &fBanned at: &b{banned_at}
      &fReason: &b{reason}
      &f-----------------------------
      """);
            node.node("ban_system", "screen_messages", "kick").set(String.class, """
      &f-----------------------------
      &cYou have been kicked from the network!
      &fPlayer: &b{player}
      &fKicked by: &b{kicked_by}
      &fReason: &b{reason}
      &f-----------------------------
      """);

            node.node("brand", "enabled").set(true);
            node.node("brand", "text").set("&3&l&nVelocityUtils&r &c| &6by Rexi666");

            node.node("regular_alerts", "enabled").set(true);
            node.node("regular_alerts", "delay_seconds").set(300);
            node.node("regular_alerts", "sound").set("ENTITY_EXPERIENCE_ORB_PICKUP");
            node.node("regular_alerts", "alerts", "discord", "message").set(List.of(
      "{center} &f-----------------------------",
      "{center} &9Join our &bDiscord&9 for news and updates!",
      "{center} &bhttps://discord.myserver.com",
      "{center} &f-----------------------------"
      ));
            node.node("regular_alerts", "alerts", "discord", "click_action").set("OPEN_URL");
            node.node("regular_alerts", "alerts", "discord", "action").set("https://discord.myserver.com");
            node.node("regular_alerts", "alerts", "discord", "hover").set("&bClick to join our Discord");
            node.node("regular_alerts", "alerts", "store", "message").set(List.of(
                    "&f-----------------------------",
                    "&9Visit our &bStore&9 for ranks and perks!",
                    "{center} &bhttps://store.myserver.com",
                    "&f-----------------------------"
            ));
            node.node("regular_alerts", "alerts", "store", "click_action").set("OPEN_URL");
            node.node("regular_alerts", "alerts", "store", "action").set("https://store.myserver.com");
            node.node("regular_alerts", "alerts", "store", "hover").set("&bClick to open our store");

            node.node("tebex_link", "enabled").set(true);
            node.node("tebex_link", "secret").set("YOUR_TEBEX_SECRET_KEY");
            node.node("tebex_link", "refresh_minutes").set(30);

            node.node("serverwhitelist", "enabled").set(true);
            node.node("serverwhitelist", "active_servers").set(List.of(
                    "whitelisted_server1",
                    "whitelisted_server2"
            ));
            node.node("serverwhitelist", "sound").set("ENTITY_VILLAGER_NO");
            node.node("serverwhitelist", "message").set(List.of(
                    "{center} &f-----------------------------",
                    "{center} &cThis server is &lwhitelisted&c!",
                    "{center} &fPlease wait until you are allowed to join.",
                    "{center} &f-----------------------------"
            ));
            node.node("serverwhitelist", "title", "enabled").set(true);
            node.node("serverwhitelist", "title", "title").set("&cServer Whitelisted");
            node.node("serverwhitelist", "title", "subtitle").set("&7Wait until you are allowed to join.");
            node.node("serverwhitelist", "title", "durations", "fade_in").set(20);
            node.node("serverwhitelist", "title", "durations", "stay").set(60);
            node.node("serverwhitelist", "title", "durations", "fade_out").set(20);
            node.node("serverwhitelist", "actionbar", "enabled").set(true);
            node.node("serverwhitelist", "actionbar", "message").set("&cServer Whitelisted");
            node.node("serverwhitelist", "bossbar", "enabled").set(true);
            node.node("serverwhitelist", "bossbar", "message").set("&cServer Whitelisted");
            node.node("serverwhitelist", "bossbar", "color").set("RED");
            node.node("serverwhitelist", "bossbar", "overlay").set("PROGRESS");
            node.node("serverwhitelist", "bossbar", "duration").set(5);

            node.node("server_icon", "enabled").set(true);
            node.node("server_icon", "file").set("server-icon.png");

            node.node("private_messages", "enabled").set(true);
            node.node("private_messages", "vmsg").set(true);
            node.node("private_messages", "vreply").set(true);
            node.node("private_messages", "vignore").set(true);
            node.node("private_messages", "vspy").set(true);

            node.node("disabled_features_servers").set(List.of(
                        "auth1",
                        "auth2"
            ));


            configLoader.save(node);
        } catch (SerializationException e) {
            System.err.println("Error al serializar la configuración.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo de configuración.");
            e.printStackTrace();
        }
    }

    public int getInt(String key) {
        try {
            ConfigurationNode node = configLoader.load();
            String[] parts = key.split("\\.");
            for (String part : parts) {
                node = node.node(part);
            }
            int result = node.getInt();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getString(String key) {
        try {
            ConfigurationNode node = configLoader.load();
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
            ConfigurationNode node = configLoader.load();
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
            ConfigurationNode node = configLoader.load();
            for (String part : key.split("\\.")) {
                node = node.node(part);
            }
            return node.getList(String.class, List.of());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void setBoolean(String key, boolean value) {
        try {
            ConfigurationNode root = configLoader.load();
            ConfigurationNode node = root;

            for (String part : key.split("\\.")) {
                node = node.node(part);
            }

            node.set(value);
            configLoader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setList(String key, List<String> value) {
        try {
            ConfigurationNode root = configLoader.load();
            ConfigurationNode node = root;

            for (String part : key.split("\\.")) {
                node = node.node(part);
            }

            node.set(value);
            configLoader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConfigurationNode getRootNode() {
        try {
            return configLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    //////////////
    // MENSAJES //
    //////////////

    public void loadMessages() {
        try {
            if (!Files.exists(messagePath)) {
                if (Files.exists(configPath)) {
                    ConfigurationNode config = configLoader.load();

                    if (!config.node("messages").virtual()) {
                        ConfigurationNode messages = messagesLoader.createNode();

                        messages.set(config.node("messages"));

                        messagesLoader.save(messages);

                        // Elimina la sección antigua del config.yml
                        config.node("messages").set(null);
                        configLoader.save(config);

                        // Completa con las nuevas claves por defecto
                        loadMessages();
                        return;
                    }
                }
                // Si el archivo no existe, crea uno con valores por defecto
                saveMessages();
            } else {
                ConfigurationNode node = messagesLoader.load();

                // Cargar las configs si no existen

                if (node.node("no_permission").empty()) {
                    node.node("no_permission").set("&cYou don't have permission to use this command");
                }
                if (node.node("no_console").empty()) {
                    node.node("no_console").set("&cOnly players can use this command");
                }
                if (node.node("new_version_available").empty()) {
                    node.node("new_version_available").set("&cA new version of VelocityUtils is available (&b{version}&c)! &e{url}");
                }
                if (node.node("alert_usage").empty()) {
                    node.node("alert_usage").set("&cUsage: /alert <message>");
                }
                if (node.node("configuration_reloaded").empty()) {
                    node.node("configuration_reloaded").set("&aConfiguration reloaded successfully! For some changes to take effect, you may need to restart the proxy.");
                }
                if (node.node("velocityutils_usage").empty()) {
                    node.node("velocityutils_usage").set("&cUsage: /velocityutils <reload|version>");
                }
                if (node.node("velocityutils_version").empty()) {
                    node.node("velocityutils_version").set("&eVelocityUtils version: &b{version} &eby &b{author}");
                }
                if (node.node("maintenance_not_on_list").empty()) {
                    node.node("maintenance_not_on_list").set("&cThe server is under maintenance!");
                }
                if (node.node("maintenance_usage").empty()) {
                    node.node("maintenance_usage").set("&cUsage: /maintenance <on|off>");
                }
                if (node.node("maintenance_activated").empty()) {
                    node.node("maintenance_activated").set("&aMaintenance mode activated.");
                }
                if (node.node("maintenance_deactivated").empty()) {
                    node.node("maintenance_deactivated").set("&cMaintenance mode deactivated.");
                }
                if (!node.node("maintenance_already_on_list").empty()) { // Ya no se necesita
                    node.node("maintenance_already_on_list").set(null);
                }
                if (!node.node("maintenance_player_added").empty()) { // Ya no se necesita
                    node.node("maintenance_player_added").set(null);
                }
                if (!node.node("maintenance_player_not_on_list").empty()) { // Ya no se necesita
                    node.node("maintenance_player_not_on_list").set(null);
                }
                if (!node.node("maintenance_player_removed").empty()) { // Ya no se necesita
                    node.node("maintenance_player_removed").set(null);
                }
                if (node.node("report_usage").empty()) {
                    node.node("report_usage").set("&cUsage: /report <nick> <reason>");
                }
                if (node.node("report_player_not_found").empty()) {
                    node.node("report_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("report_not_own").empty()) {
                    node.node("report_not_own").set("&cYou cannot report yourself");
                }
                if (node.node("report_sent").empty()) {
                    node.node("report_sent").set("&aYour report for the player {target} was sent");
                }
                if (node.node("report_hover").empty()) {
                    node.node("report_hover").set("&bClick to teleport");
                }
                if (node.node("report_cooldown").empty()) {
                    node.node("report_cooldown").set("&cYou have {time}s before using /report again");
                }
                if (node.node("report_webhook_error").empty()) {
                    node.node("report_webhook_error").set("&cError trying to send discord report webhook");
                }
                if (node.node("helpop_usage").empty()) {
                    node.node("helpop_usage").set("&cUsage: /helpop <reason>");
                }
                if (node.node("helpop_cooldown").empty()) {
                    node.node("helpop_cooldown").set("&cYou have {time}s before using /helpop again");
                }
                if (node.node("helpop_hover").empty()) {
                    node.node("helpop_hover").set("&bClick to teleport");
                }
                if (node.node("helpop_sent").empty()) {
                    node.node("helpop_sent").set("&aYour help request was sent");
                }
                if (node.node("goto_usage").empty()) {
                    node.node("goto_usage").set("&cUsage: /goto <player>");
                }
                if (node.node("goto_player_not_found").empty()) {
                    node.node("goto_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("goto_server_not_found").empty()) {
                    node.node("goto_server_not_found").set("&cServer could not be found");
                }
                if (node.node("goto_same_server").empty()) {
                    node.node("goto_same_server").set("&cYou are currently on the same server as {player}");
                }
                if (node.node("goto_connecting").empty()) {
                    node.node("goto_connecting").set("&aConnecting with {player} server");
                }
                if (node.node("find_usage").empty()) {
                    node.node("find_usage").set("&cUsage: /find <player>");
                }
                if (node.node("find_player_not_found").empty()) {
                    node.node("find_player_not_found").set("&cPlayer {player} not found");
                }
                if (node.node("find_where").empty()) {
                    node.node("find_where").set("&b{player} &eis on &b{server}");
                }
                if (node.node("find_last_seen").empty()) {
                    node.node("find_last_seen").set("&e{player} &cis not connected. Last seen: &e{time} ago");
                }
                if (node.node("find_less_minute").empty()) {
                    node.node("find_less_minute").set("Less than 1 minute");
                }
                if (node.node("server_unknown").empty()) {
                    node.node("server_unknown").set("Unknown");
                }
                if (node.node("stafflist_no_staff").empty()) {
                    node.node("stafflist_no_staff").set("&cThere are no staff online");
                }
                if (node.node("stafflist_header").empty()) {
                    node.node("stafflist_header").set("&b&lStaff List");
                }
                if (node.node("stafflist_staff").empty()) {
                    node.node("stafflist_staff").set("{prefix} &f{player} &7- &b{server}");
                }
                if (node.node("staffchat_disabled").empty()) {
                    node.node("staffchat_disabled").set("&eStaff chat &cdisabled");
                }
                if (node.node("staffchat_enabled").empty()) {
                    node.node("staffchat_enabled").set("&eStaff chat &aenabled");
                }
                if (node.node("staffchat_format").empty()) {
                    node.node("staffchat_format").set("&8[&bStaffChat&8] &7{server} - {prefix} &b{player}&7: &f{message}");
                }

                if (node.node("adminchat_disabled").empty()) {
                    node.node("adminchat_disabled").set("&eAdmin chat &cdisabled");
                }
                if (node.node("adminchat_enabled").empty()) {
                    node.node("adminchat_enabled").set("&eAdmin chat &aenabled");
                }
                if (node.node("adminchat_format").empty()) {
                    node.node("adminchat_format").set("&8[&dAdminChat&8] &7{server} - {prefix} &d{player}&7: &f{message}");
                }
                if (node.node("stafftime_usage").empty()) {
                    node.node("stafftime_usage").set("&cUsage: /stafftime <player> [day|week|month]");
                }
                if (node.node("stafftime_not_found").empty()) {
                    node.node("stafftime_not_found").set("&cPlayer {player} not found on the database.");
                }
                if (node.node("stafftime_invalid_type").empty()) {
                    node.node("stafftime_invalid_type").set("&cInvalid type. Use day, week or month");
                }
                if (node.node("vlist_no_players").empty()) {
                    node.node("vlist_no_players").set("&cThere are no players online.");
                }
                if (node.node("movecommands_no_servers").empty()) {
                    node.node("movecommands_no_servers").set("&cThere are no servers configured for this command");
                }
                if (node.node("movecommands_server_not_found").empty()) {
                    node.node("movecommands_server_not_found").set("&cThat server is not available at this moment.");
                }
                if (node.node("movecommands_already_connected").empty()) {
                    node.node("movecommands_already_connected").set("&cYou are already connected to that server");
                }
                if (node.node("messagescommands_no_message_console").empty()) {
                    node.node("messagescommands_no_message_console").set("&cThe messagecommand message is empty: {command}");
                }
                if (node.node("messagescommands_no_action_or_hover_console").empty()) {
                    node.node("messagescommands_no_action_or_hover_console").set("&cThe messagecommand {command} has action set to true, but no action or hover set");
                }
                if (node.node("messagescommands_error_player").empty()) {
                    node.node("messagescommands_error_player").set("&cThat messagecommand doesnt work as intended, contact an administrator");
                }
                if (node.node("stream_usage").empty()) {
                    node.node("stream_usage").set("&cUsage: /stream <url>");
                }
                if (node.node("stream_invalid_url").empty()) {
                    node.node("stream_invalid_url").set("&cThats not a valid stream url");
                }
                if (node.node("stream_cooldown").empty()) {
                    node.node("stream_cooldown").set("&cYou have to wait {cooldown} before using /stream again");
                }
                if (node.node("serverexecute_usage").empty()) {
                    node.node("serverexecute_usage").set("&cUsage: /serverexecute <server> <command>");
                }
                if (node.node("serverexecute_server_not_found").empty()) {
                    node.node("serverexecute_server_not_found").set("&cServer {server} not found");
                }
                if (node.node("serverexecute_sent").empty()) {
                    node.node("serverexecute_sent").set("&aSent to server {server}, the command: /{command}");
                }
                if (node.node("togglesc_enabled").empty()) {
                    node.node("togglesc_enabled").set("&aStaff chat messages will be shown");
                }
                if (node.node("togglesc_disabled").empty()) {
                    node.node("togglesc_disabled").set("&cStaff chat messages will be hidden");
                }
                if (node.node("usage_ban").empty()) {
                    node.node("usage_ban").set("&cUsage: /vban <player> [reason]");
                }
                if (node.node("usage_banip").empty()) {
                    node.node("usage_banip").set("&cUsage: /vbanip <player> [reason]");
                }
                if (node.node("usage_unban").empty()) {
                    node.node("usage_unban").set("&cUsage: /vunban <player>");
                }
                if (node.node("usage_kick").empty()) {
                    node.node("usage_kick").set("&cUsage: /vkick <player> [reason]");
                }
                if (node.node("usage_checkban").empty()) {
                    node.node("usage_checkban").set("&cUsage: /vcheckban <player>");
                }
                if (node.node("ban_success").empty()) {
                    node.node("ban_success").set("&cYou have banned &b{player} &cfor &b{reason}");
                }
                if (node.node("banip_success").empty()) {
                    node.node("banip_success").set("&cYou have ip banned &b{player} &cfor &b{reason}");
                }
                if (node.node("unban_success").empty()) {
                    node.node("unban_success").set("&aYou have unbanned &b{player}");
                }
                if (node.node("kick_success").empty()) {
                    node.node("kick_success").set("&cYou have kicked &b{player} &cfor &b{reason}");
                }
                if (node.node("checkban_banned").empty()) {
                    node.node("checkban_banned").set("&c{player} is banned by {banned_by}! Reason: &b{reason}");
                }
                if (node.node("checkban_banned_ip").empty()) {
                    node.node("checkban_banned_ip").set("&c{player} is banned by IP ({ip_playername})! Banned by {banned_by}! Reason: &b{reason}");
                }
                if (node.node("checkban_not_banned").empty()) {
                    node.node("checkban_not_banned").set("&a{player} is not banned!");
                }
                if (node.node("already_banned").empty()) {
                    node.node("already_banned").set("&c{player} is already banned!");
                }
                if (node.node("not_banned").empty()) {
                    node.node("not_banned").set("&c{player} is not banned!");
                }
                if (node.node("not_connected").empty()) {
                    node.node("not_connected").set("&c{player} is not connected!");
                }
                if (node.node("not_ip_registered").empty()) {
                    node.node("not_ip_registered").set("&c{player} had never entered the server and doesnt have an ip registered!");
                }
                if (node.node("try_join_ban").empty()) {
                    node.node("try_join_ban").set("&c{player} tried to join but is banned! Reason: &b{reason}");
                }
                if (node.node("try_join_banip").empty()) {
                    node.node("try_join_banip").set("&c{player} tried to join but their IP is banned ({ip_playername})! Reason: &b{reason}");
                }
                if (node.node("ban_notify").empty()) {
                    node.node("ban_notify").set("&c{player} was banned by {banned_by} for {reason}");
                }
                if (node.node("banip_notify").empty()) {
                    node.node("banip_notify").set("&c{player} was IP banned by {banned_by} for {reason}");
                }
                if (node.node("unban_notify").empty()) {
                    node.node("unban_notify").set("&c{player} was unbanned by {unbanned_by}");
                }
                if (node.node("kick_notify").empty()) {
                    node.node("kick_notify").set("&c{player} was kicked by {kicked_by} for {reason}");
                }
                if (node.node("serverwhitelist_tried").empty()) {
                    node.node("serverwhitelist_tried").set("&c{player} tried to join {server} but its whitelisted!");
                }
                if (node.node("serverwhitelist_usage").empty()) {
                    node.node("serverwhitelist_usage").set("&cUsage: /serverwhitelist <add|remove> <server> | /serverwhitelist list");
                }
                if (node.node("serverwhitelist_server_not_found").empty()) {
                    node.node("serverwhitelist_server_not_found").set("&cServer {server} not found");
                }
                if (node.node("serverwhitelist_already_on_list").empty()) {
                    node.node("serverwhitelist_already_on_list").set("&cThat server is already with whitelist on");
                }
                if (node.node("serverwhitelist_server_added").empty()) {
                    node.node("serverwhitelist_server_added").set("&aServer {server} added to whitelist");
                }
                if (node.node("serverwhitelist_server_not_on_list").empty()) {
                    node.node("serverwhitelist_server_not_on_list").set("&cServer {server} is not on the whitelist");
                }
                if (node.node("serverwhitelist_server_removed").empty()) {
                    node.node("serverwhitelist_server_removed").set("&cServer {server} removed from whitelist");
                }
                if (node.node("serverwhitelist_list_empty").empty()) {
                    node.node("serverwhitelist_list_empty").set("&cThere are no servers on the whitelist");
                }
                if (node.node("serverwhitelist_list_header").empty()) {
                    node.node("serverwhitelist_list_header").set("&6Whitelisted servers:");
                }
                if (node.node("serverwhitelist_list_format").empty()) {
                    node.node("serverwhitelist_list_format").set("&7- &e{server}");
                }
                if (node.node("msg_usage").empty()) {
                    node.node("msg_usage").set("&cUsage: /vmsg <player> <message>");
                }
                if (node.node("msg").empty()) {
                    node.node("msg").set("&7[&b{player} &7({player_server}) &e> &b{target} &7({target_server})&7] &e{message}");
                }
                if (node.node("msg_offline").empty()) {
                    node.node("msg_offline").set("&c{player} is currently offline.");
                }
                if (node.node("msg_self").empty()) {
                    node.node("msg_self").set("&cYou cannot message yourself.");
                }
                if (node.node("msg_ignoring").empty()) {
                    node.node("msg_ignoring").set("&c{player} is ignoring you.");
                }
                if (node.node("reply_usage").empty()) {
                    node.node("reply_usage").set("&cUsage: /vreply <message>");
                }
                if (node.node("reply_offline").empty()) {
                    node.node("reply_offline").set("&cYou have no one to reply to.");
                }
                if (node.node("ignore_usage").empty()) {
                    node.node("ignore_usage").set("&cUsage: /vignore <player>");
                }
                if (node.node("ignore_self").empty()) {
                    node.node("ignore_self").set("&cYou cannot ignore yourself.");
                }
                if (node.node("ignore_offline").empty()) {
                    node.node("ignore_offline").set("&c{player} is currently offline.");
                }
                if (node.node("ignore_bypass").empty()) {
                    node.node("ignore_bypass").set("&cYou cannot ignore {player}.");
                }
                if (node.node("ignore_added").empty()) {
                    node.node("ignore_added").set("&aYou have ignored {player}.");
                }
                if (node.node("ignore_removed").empty()) {
                    node.node("ignore_removed").set("&aYou have unignored {player}.");
                }
                if (node.node("spy_self").empty()) {
                    node.node("spy_self").set("&cYou cannot spy on yourself.");
                }
                if (node.node("spy_offline").empty()) {
                    node.node("spy_offline").set("&c{player} is currently offline.");
                }
                if (node.node("spy_enabled").empty()) {
                    node.node("spy_enabled").set("&aYou have enabled spy mode.");
                }
                if (node.node("spy_disabled").empty()) {
                    node.node("spy_disabled").set("&cYou have disabled spy mode.");
                }
                if (node.node("spy_player_enabled").empty()) {
                    node.node("spy_player_enabled").set("&aYou have enabled spy mode for {player}.");
                }
                if (node.node("spy_player_disabled").empty()) {
                    node.node("spy_player_disabled").set("&cYou have disabled spy mode for {player}.");
                }
                if (node.node("disabled_features_servers").empty()) {
                    node.node("disabled_features_servers").set("&cThat action is disabled on this server.");
                }
                if (node.node("day_simbol").empty()) {
                    node.node("day_simbol").set("d");
                }
                if (node.node("hour_simbol").empty()) {
                    node.node("hour_simbol").set("h");
                }
                if (node.node("minute_simbol").empty()) {
                    node.node("minute_simbol").set("m");
                }
                if (node.node("second_simbol").empty()) {
                    node.node("second_simbol").set("s");
                }
                
                // Guardar en caso de que se hayan agregado valores predeterminados
                messagesLoader.save(node);
            }
        } catch (SerializationException e) {
            System.err.println("Error al serializar/deserializar la configuración de messages.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error al leer/escribir el archivo de configuración de messages.");
            e.printStackTrace();
        }
    }

    public void saveMessages() {
        try {
            ConfigurationNode node = messagesLoader.createNode();

            // 💡 Crear la estructura correctamente sin inline mapping
            node.node("no_permission").set("&cYou don't have permission to use this command");
            node.node("no_console").set("&cOnly players can use this command");
            node.node("new_version_available").set("&cA new version of VelocityUtils is available (&b{version}&c)! &e{url}");
            node.node("alert_usage").set("&cUsage: /alert <message>");
            node.node("configuration_reloaded").set("&aConfiguration reloaded successfully! For some changes to take effect, you may need to restart the proxy.");
            node.node("velocityutils_usage").set("&cUsage: /velocityutils <reload|version>");
            node.node("velocityutils_version").set("&eVelocityUtils version: &b{version} &eby &b{author}");
            node.node("maintenance_not_on_list").set("&cThe server is under maintenance!");
            node.node("maintenance_usage").set("&cUsage: /maintenance <on|off>");
            node.node("maintenance_activated").set("&aMaintenance mode activated.");
            node.node("maintenance_deactivated").set("&cMaintenance mode deactivated.");
            node.node("report_usage").set("&cUsage: /report <nick> <reason>");
            node.node("report_player_not_found").set("&cPlayer {player} not found");
            node.node("report_not_own").set("&cYou cannot report yourself");
            node.node("report_sent").set("&aYour report for the player {target} was sent");
            node.node("report_hover").set("&bClick to teleport");
            node.node("report_cooldown").set("&cYou have {time}s before using /report again");
            node.node("report_webhook_error").set("&cError trying to send discord report webhook");
            node.node("helpop_usage").set("&cUsage: /helpop <reason>");
            node.node("helpop_cooldown").set("&cYou have {time}s before using /helpop again");
            node.node("helpop_hover").set("&bClick to teleport");
            node.node("helpop_sent").set("&aYour help request was sent");
            node.node("goto_usage").set("&cUsage: /goto <player>");
            node.node("goto_player_not_found").set("&cPlayer {player} not found");
            node.node("goto_server_not_found").set("&cServer could not be found");
            node.node("goto_same_server").set("&cYou are currently on the same server as {player}");
            node.node("goto_connecting").set("&aConnecting with {player} server");
            node.node("find_usage").set("&cUsage: /find <player>");
            node.node("find_player_not_found").set("&cPlayer {player} not found");
            node.node("find_where").set("&b{player} &eis on &b{server}");
            node.node("find_last_seen").set("&e{player} &cis not connected. Last seen: &e{time} ago");
            node.node("find_less_minute").set("Less than 1 minute");
            node.node("server_unknown").set("Unknown");
            node.node("stafflist_no_staff").set("&cThere are no staff online");
            node.node("stafflist_header").set("&b&lStaff List");
            node.node("stafflist_staff").set("{prefix} &f{player} &7- &b{server}");
            node.node("staffchat_disabled").set("&eStaff chat &cdisabled");
            node.node("staffchat_enabled").set("&eStaff chat &aenabled");
            node.node("staffchat_format").set("&8[&bStaffChat&8] &7{server} - {prefix} &b{player}&7: &f{message}");
            node.node("adminchat_disabled").set("&eAdmin chat &cdisabled");
            node.node("adminchat_enabled").set("&eAdmin chat &aenabled");
            node.node("adminchat_format").set("&8[&dAdminChat&8] &7{server} - {prefix} &d{player}&7: &f{message}");
            node.node("stafftime_usage").set("&cUsage: /stafftime <player> [day|week|month]");
            node.node("stafftime_not_found").set("&cPlayer {player} not found on the database.");
            node.node("stafftime_invalid_type").set("&cInvalid type. Use day, week or month");
            node.node("vlist_no_players").set("&cThere are no players online.");
            node.node("movecommands_no_servers").set("&cThere are no servers configured for this command");
            node.node("movecommands_server_not_found").set("&cThat server is not available at this moment.");
            node.node("movecommands_already_connected").set("&cYou are already connected to that server");
            node.node("messagescommands_no_message_console").set("&cThe messagecommand message is empty: {command}");
            node.node("messagescommands_no_action_or_hover_console").set("&cThe messagecommand {command} has action set to true, but no action or hover set");
            node.node("messagescommands_error_player").set("&cThat messagecommand doesnt work as intended, contact an administrator");
            node.node("stream_usage").set("&cUsage: /stream <url>");
            node.node("stream_invalid_url").set("&cThats not a valid stream url");
            node.node("stream_cooldown").set("&cYou have to wait {cooldown} before using /stream again");
            node.node("serverexecute_usage").set("&cUsage: /serverexecute <server> <command>");
            node.node("serverexecute_server_not_found").set("&cServer {server} not found");
            node.node("serverexecute_sent").set("&aSent to server {server}, the command: /{command}");
            node.node("togglesc_enabled").set("&aStaff chat messages will be shown");
            node.node("togglesc_disabled").set("&cStaff chat messages will be hidden");
            node.node("usage_ban").set("&cUsage: /vban <player> [reason]");
            node.node("usage_banip").set("&cUsage: /vbanip <player> [reason]");
            node.node("usage_unban").set("&cUsage: /vunban <player>");
            node.node("usage_kick").set("&cUsage: /vkick <player> [reason]");
            node.node("usage_checkban").set("&cUsage: /vcheckban <player>");
            node.node("ban_success").set("&cYou have banned &b{player} &cfor &b{reason}");
            node.node("banip_success").set("&cYou have ip banned &b{player} &cfor &b{reason}");
            node.node("unban_success").set("&aYou have unbanned &b{player}");
            node.node("kick_success").set("&cYou have kicked &b{player} &cfor &b{reason}");
            node.node("checkban_banned").set("&c{player} is banned by {banned_by}! Reason: &b{reason}");
            node.node("checkban_banned_ip").set("&c{player} is banned by IP ({ip_playername})! Banned by {banned_by}! Reason: &b{reason}");
            node.node("checkban_not_banned").set("&a{player} is not banned!");
            node.node("already_banned").set("&c{player} is already banned!");
            node.node("not_banned").set("&c{player} is not banned!");
            node.node("not_connected").set("&c{player} is not connected!");
            node.node("not_ip_registered").set("&c{player} had never entered the server and doesnt have an ip registered!");
            node.node("try_join_ban").set("&c{player} tried to join but is banned! Reason: &b{reason}");
            node.node("try_join_banip").set("&c{player} tried to join but their IP is banned ({ip_playername})! Reason: &b{reason}");
            node.node("ban_notify").set("&c{player} was banned by {banned_by} for {reason}");
            node.node("banip_notify").set("&c{player} was IP banned by {banned_by} for {reason}");
            node.node("unban_notify").set("&c{player} was unbanned by {unbanned_by}");
            node.node("kick_notify").set("&c{player} was kicked by {kicked_by} for {reason}");

            node.node("serverwhitelist_tried").set("&c{player} tried to join {server} but its whitelisted!");
            node.node("serverwhitelist_usage").set("&cUsage: /serverwhitelist <add|remove> <server> | /serverwhitelist list");
            node.node("serverwhitelist_server_not_found").set("&cServer {server} not found");
            node.node("serverwhitelist_already_on_list").set("&cThat server is already with whitelist on");
            node.node("serverwhitelist_server_added").set("&aServer {server} added to whitelist");
            node.node("serverwhitelist_server_not_on_list").set("&cServer {server} is not on the whitelist");
            node.node("serverwhitelist_server_removed").set("&cServer {server} removed from whitelist");
            node.node("serverwhitelist_list_empty").set("&cThere are no servers on the whitelist");
            node.node("serverwhitelist_list_header").set("&6Whitelisted servers:");
            node.node("serverwhitelist_list_format").set("&7- &e{server}");

            node.node("msg_usage").set("&cUsage: /vmsg <player> <message>");
            node.node("msg").set("&7[&b{player} &7({player_server}) &e> &b{target} &7({target_server})&7] &e{message}");
            node.node("msg_offline").set("&c{player} is currently offline.");
            node.node("msg_self").set("&cYou cannot message yourself.");
            node.node("msg_ignoring").set("&c{player} is ignoring you.");
            node.node("reply_usage").set("&cUsage: /vreply <message>");
            node.node("reply_offline").set("&cYou have no one to reply to.");
            node.node("ignore_usage").set("&cUsage: /vignore <player>");
            node.node("ignore_self").set("&cYou cannot ignore yourself.");
            node.node("ignore_offline").set("&c{player} is currently offline.");
            node.node("ignore_bypass").set("&cYou cannot ignore {player}.");
            node.node("ignore_added").set("&aYou have ignored {player}.");
            node.node("ignore_removed").set("&aYou have unignored {player}.");
            node.node("spy_self").set("&cYou cannot spy on yourself.");
            node.node("spy_offline").set("&c{player} is currently offline.");
            node.node("spy_enabled").set("&aYou have enabled spy mode.");
            node.node("spy_disabled").set("&cYou have disabled spy mode.");
            node.node("spy_player_enabled").set("&aYou have enabled spy mode for {player}.");
            node.node("spy_player_disabled").set("&cYou have disabled spy mode for {player}.");
            node.node("disabled_features_servers").set("&cThat action is disabled on this server.");

            node.node("day_simbol").set("d");
            node.node("hour_simbol").set("h");
            node.node("minute_simbol").set("m");
            node.node("second_simbol").set("s");

            messagesLoader.save(node);
        } catch (SerializationException e) {
            System.err.println("Error al serializar la configuración de mensajes.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo de configuración de mensajes.");
            e.printStackTrace();
        }
    }
    
    public Component getMessage(String key, String... replacements) {
        try {
            ConfigurationNode node = messagesLoader.load();
            
            for (String part : key.split("\\.")) {
                node = node.node(part);
            }

            String line = node.getString("&cMessage not found: " + key);

            for (int i = 0; i + 1 < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }

            if (line.startsWith("{center}")) {
                line = line.replaceFirst("^\\{center\\}\\s*", "");
                line = getCenteredMessage(line);
            }

            return legacy(line);
        } catch (IOException e) {
            e.printStackTrace();
            return legacy("&cError loading message: " + key);
        }
    }

    public String getMessageString(String key, String... replacements) {
        try {
            ConfigurationNode node = messagesLoader.load();

            for (String part : key.split("\\.")) {
                node = node.node(part);
            }

            String line = node.getString("&cMessage not found: " + key);

            for (int i = 0; i + 1 < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }

            if (line.startsWith("{center}")) {
                line = line.replaceFirst("^\\{center\\}\\s*", "");
                line = getCenteredMessage(line);
            }

            return line;
        } catch (IOException e) {
            e.printStackTrace();
            return "&cError loading message: " + key;
        }
    }

    private static final LegacyComponentSerializer LEGACY_HEX_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    public Component legacy(String text) {
        return LEGACY_HEX_SERIALIZER.deserialize(text);
    }

    public String getCenteredMessage(String message){
        String original = message;

        if (message.contains("<") && message.contains(">")) {
            message = LegacyComponentSerializer.legacyAmpersand().serialize(
                    MiniMessage.miniMessage().deserialize(message)
            );
        } else {
            // Si es legacy (&), normalízalo (también soporta hex)
            message = LEGACY_HEX_SERIALIZER.serialize(
                    LEGACY_HEX_SERIALIZER.deserialize(message)
            );
        }

        int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : message.toCharArray()){
            if(c == '&'){
                previousCode = true;
                continue;
            }

            if(previousCode){
                previousCode = false;
                if (c == 'l' || c == 'L') {
                    isBold = true;
                } else {
                    isBold = false;
                }
            }else{
                DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PX - halvedMessageSize;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while(compensated < toCompensate){
            sb.append(" ");
            compensated += spaceLength;
        }
        return (sb.toString() + original);
    }
}