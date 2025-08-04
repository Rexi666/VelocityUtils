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

                // Cargar los mensajes si no existen

                if (node.node("alert", "enabled").empty()) {
                    node.node("alert", "enabled").set(true);
                }
                if (node.node("alert", "prefix").empty()) {
                    node.node("alert", "prefix").set("&7[&b&lSERVER&7]");
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
                if (node.node("maintenance", "allowed").empty()) {
                    node.node("maintenance", "allowed").setList(String.class, List.of("Rexigamer666"));
                }

                if (node.node("report", "enabled").empty()) {
                    node.node("report", "enabled").set(true);
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
                            "&fServer: &b{server}",
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

                if (node.node("helpop", "enabled").empty()) {
                    node.node("helpop", "enabled").set(true);
                }
                if (node.node("helpop", "teleport_on_click").empty()) {
                    node.node("helpop", "teleport_on_click").set(true);
                }
                if (node.node("helpop", "message").empty()) {
                    node.node("helpop", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&eNew Help Request from {player}!",
                            "&fReason: &b{reason}",
                            "&fServer: &b{server}",
                            "&eClick to teleport",
                            "&f-----------------------------"));
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
                            "&f-----------------------------",
                            "&eStaff Time from {player}",
                            "&fToday: &b{day}",
                            "&fWeek: &b{week}",
                            "&fMonth: &b{month}",
                            "&f-----------------------------"));
                }
                if (node.node("stafftime", "command", "type").empty()) {
                    node.node("stafftime", "command", "type").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&eStaff Time from {player} ({type})",
                            "&f{type}: &b{time}",
                            "&f-----------------------------"));
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
                            "&f-----------------------------",
                            "&eThere are {count} players online",
                            "{servercount}",
                            "&f-----------------------------"));
                }
                if (node.node("vlist", "server", "servercount").empty()) {
                    node.node("vlist", "server", "servercount").set("&7[&b{server} &7(&b{count}&7)] - &f{players}");
                }
                if (node.node("vlist", "rank", "message").empty()) {
                    node.node("vlist", "rank", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&eThere are {count} players online",
                            "{rankcount}",
                            "&f-----------------------------"));
                }
                if (node.node("vlist", "rank", "rankcount").empty()) {
                    node.node("vlist", "rank", "rankcount").set("&7[&b{rank} &7(&b{count}&7)] - &f{players}");
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
                            "&f-----------------------------",
                            "&fJoin ur &9discord",
                            "&9https://discord.com/invite/a3zkKtrjTr",
                            "&f-----------------------------"));
                    node.node("messagescommands", "discord", "click_action").set("OPEN_URL");
                    node.node("messagescommands", "discord", "action").set("https://discord.com/invite/a3zkKtrjTr");
                    node.node("messagescommands", "discord", "hover").set("&9Click to join ur discord");

                    node.node("messagescommands", "newgamemode", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&6New Game Mode realeased",
                            "&#c3d600&lJ&#c6c900&lO&#cabd00&lI&#cdb000&lN &#d1a400&lT&#d49700&lH&#d78a00&lE&#db7e00&lN&#de7100&lE&#e26500&lW &#e55800&lS&#e94c00&lU&#ec3f00&lR&#ef3200&lV&#f32600&lI&#f61900&lV&#fa0d00&lA&#fd0000&lL",
                            "&f-----------------------------"));
                    node.node("messagescommands", "newgamemode", "click_action").set("RUN_COMMAND");
                    node.node("messagescommands", "newgamemode", "action").set("/survival");
                    node.node("messagescommands", "newgamemode", "hover").set("&6Click to join ur new survival");

                    node.node("messagescommands", "rules", "message").setList(String.class, List.of(
                            "&f-----------------------------",
                            "&6Remember to read all the server rules",
                            "&f-----------------------------"));
                    node.node("messagescommands", "rules", "click_action").set("NONE");
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

                if (node.node("messages", "no_permission").empty()) {
                    node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
                }
                if (node.node("messages", "no_console").empty()) {
                    node.node("messages", "no_console").set("&cOnly players can use this command");
                }
                if (node.node("messages", "new_version_available").empty()) {
                    node.node("messages", "new_version_available").set("&cA new version of VelocityUtils is available (&b{version}&c)! &e{url}");
                }
                if (node.node("messages", "alert_usage").empty()) {
                    node.node("messages", "alert_usage").set("&cUsage: /alert <message>");
                }
                if (node.node("messages", "configuration_reloaded").empty()) {
                    node.node("messages", "configuration_reloaded").set("&aConfiguration reloaded successfully! For some changes to take effect, you may need to restart the proxy.");
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
                if (node.node("messages", "helpop_usage").empty()) {
                    node.node("messages", "helpop_usage").set("&cUsage: /helpop <reason>");
                }
                if (node.node("messages", "helpop_cooldown").empty()) {
                    node.node("messages", "helpop_cooldown").set("&cYou have {time}s before using /helpop again");
                }
                if (node.node("messages", "helpop_hover").empty()) {
                    node.node("messages", "helpop_hover").set("&bClick to teleport");
                }
                if (node.node("messages", "helpop_sent").empty()) {
                    node.node("messages", "helpop_sent").set("&aYour help request was sent");
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
                if (node.node("messages", "server_unknown").empty()) {
                    node.node("messages", "server_unknown").set("Unknown");
                }
                if (node.node("messages", "find_where").empty()) {
                    node.node("messages", "find_where").set("&b{player} &eis on &b{server}");
                }
                if (node.node("messages", "stafflist_no_staff").empty()) {
                    node.node("messages", "stafflist_no_staff").set("&cThere are no staff online");
                }
                if (node.node("messages", "stafflist_header").empty()) {
                    node.node("messages", "stafflist_header").set("&b&lStaff List");
                }
                if (node.node("messages", "stafflist_staff").empty()) {
                    node.node("messages", "stafflist_staff").set("{prefix} &f{player} &7- &b{server}");
                }
                if (node.node("messages", "staffchat_disabled").empty()) {
                    node.node("messages", "staffchat_disabled").set("&eStaff chat &cdisabled");
                }
                if (node.node("messages", "staffchat_enabled").empty()) {
                    node.node("messages", "staffchat_enabled").set("&eStaff chat &aenabled");
                }
                if (node.node("messages", "staffchat_format").empty()) {
                    node.node("messages", "staffchat_format").set("&8[&bStaffChat&8] &7{server} - {prefix} &b{player}&7: &f{message}");
                }

                if (node.node("messages", "adminchat_disabled").empty()) {
                    node.node("messages", "adminchat_disabled").set("&eAdmin chat &cdisabled");
                }
                if (node.node("messages", "adminchat_enabled").empty()) {
                    node.node("messages", "adminchat_enabled").set("&eAdmin chat &aenabled");
                }
                if (node.node("messages", "adminchat_format").empty()) {
                    node.node("messages", "adminchat_format").set("&8[&dAdminChat&8] &7{server} - {prefix} &d{player}&7: &f{message}");
                }
                if (node.node("messages", "stafftime_usage").empty()) {
                    node.node("messages", "stafftime_usage").set("&cUsage: /stafftime <player> [day|week|month]");
                }
                if (node.node("messages", "stafftime_not_found").empty()) {
                    node.node("messages", "stafftime_not_found").set("&cPlayer {player} not found on the database.");
                }
                if (node.node("messages", "stafftime_invalid_type").empty()) {
                    node.node("messages", "stafftime_invalid_type").set("&cInvalid type. Use day, week or month");
                }
                if (node.node("messages", "vlist_no_players").empty()) {
                    node.node("messages", "vlist_no_players").set("&cThere are no players online.");
                }
                if (node.node("messages", "movecommands_no_servers").empty()) {
                    node.node("messages", "movecommands_no_servers").set("&cThere are no servers configured for this command");
                }
                if (node.node("messages", "movecommands_server_not_found").empty()) {
                    node.node("messages", "movecommands_server_not_found").set("&cThat server is not available at this moment.");
                }
                if (node.node("messages", "movecommands_already_connected").empty()) {
                    node.node("messages", "movecommands_already_connected").set("&cYou are already connected to that server");
                }
                if (node.node("messages", "messagescommands_no_message_console").empty()) {
                    node.node("messages", "messagescommands_no_message_console").set("&cThe messagecommand message is empty: {command}");
                }
                if (node.node("messages", "messagescommands_no_action_or_hover_console").empty()) {
                    node.node("messages", "messagescommands_no_action_or_hover_console").set("&cThe messagecommand {command} has action set to true, but no action or hover set");
                }
                if (node.node("messages", "messagescommands_error_player").empty()) {
                    node.node("messages", "messagescommands_error_player").set("&cThat messagecommand doesnt work as intended, contact an administrator");
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
            node.node("alert", "enabled").set(true);
            node.node("alert", "prefix").set("&7[&b&lSERVER&7]");

            // Agregar mensajes predeterminados
            node.node("motd", "enabled").set(true);
            node.node("motd", "line1").set("&aWelcome to this Velocity Server!");
            node.node("motd", "line2").set("<bold><gradient:yellow:green>Enjoy your stay</gradient></bold>");

            node.node("maintenance", "enabled").set(true);
            node.node("maintenance", "active").set(false);
            node.node("maintenance", "motd", "line1").set("&cServer under maintenance!");
            node.node("maintenance", "motd", "line2").set("<bold><gradient:red:yellow>Try again later</gradient></bold>");
            node.node("maintenance", "allowed").setList(String.class, List.of("Rexigamer666"));

            node.node("report", "enabled").set(true);
            node.node("report", "teleport_on_click").set(true);
            node.node("report", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eNew Report from {player}!",
                    "&fReported: &c{reported}",
                    "&fReason: &b{reason}",
                    "&fServer: &b{server}",
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

            node.node("helpop", "enabled").set(true);
            node.node("helpop", "teleport_on_click").set(true);
            node.node("helpop", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eNew Help Request from {player}!",
                    "&fReason: &b{reason}",
                    "&fServer: &b{server}",
                    "&eClick to teleport",
                    "&f-----------------------------"));
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
                    "&f-----------------------------",
                    "&eStaff Time from {player}",
                    "&fToday: &b{day}",
                    "&fWeek: &b{week}",
                    "&fMonth: &b{month}",
                    "&f-----------------------------"));
            node.node("stafftime", "command", "type").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eStaff Time from {player} ({type})",
                    "&f{type}: &b{time}",
                    "&f-----------------------------"));
            node.node("stafftime", "command", "day").set("Day");
            node.node("stafftime", "command", "week").set("Week");
            node.node("stafftime", "command", "month").set("Month");

            node.node("vlist", "enabled").set(true);
            node.node("vlist", "default_mode").set("server");
            node.node("vlist", "server", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eThere are {count} players online",
                    "{servercount}",
                    "&f-----------------------------"));
            node.node("vlist", "server", "servercount").set("&7[&b{server} &7(&b{count}&7)] - &f{players}");
            node.node("vlist", "rank", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&eThere are {count} players online",
                    "{rankcount}",
                    "&f-----------------------------"));
            node.node("vlist", "rank", "rankcount").set("&7[&b{rank} &7(&b{count}&7)] - &f{players}");

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
                    "&f-----------------------------",
                    "&fJoin ur &9discord",
                    "&9https://discord.com/invite/a3zkKtrjTr",
                    "&f-----------------------------"));
            node.node("messagescommands", "discord", "click_action").set("OPEN_URL");
            node.node("messagescommands", "discord", "action").set("https://discord.com/invite/a3zkKtrjTr");
            node.node("messagescommands", "discord", "hover").set("&9Click to join ur discord");
            node.node("messagescommands", "newgamemode", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&6New Game Mode realeased",
                    "&#c3d600&lJ&#c6c900&lO&#cabd00&lI&#cdb000&lN &#d1a400&lT&#d49700&lH&#d78a00&lE&#db7e00&lN&#de7100&lE&#e26500&lW &#e55800&lS&#e94c00&lU&#ec3f00&lR&#ef3200&lV&#f32600&lI&#f61900&lV&#fa0d00&lA&#fd0000&lL",
                    "&f-----------------------------"));
            node.node("messagescommands", "newgamemode", "click_action").set("RUN_COMMAND");
            node.node("messagescommands", "newgamemode", "action").set("/survival");
            node.node("messagescommands", "newgamemode", "hover").set("&6Click to join ur new survival");
            node.node("messagescommands", "rules", "message").setList(String.class, List.of(
                    "&f-----------------------------",
                    "&6Remember to read all the server rules",
                    "&f-----------------------------"));
            node.node("messagescommands", "rules", "click_action").set("NONE");

            node.node("find", "enabled").set(true);
            node.node("goto", "enabled").set(true);
            node.node("stafflist", "enabled").set(true);

            node.node("staffjoin", "enabled").set(true);
            node.node("staffjoin", "join_message").set("&b&lStaff - &a{rank} {player} has joined the server");
            node.node("staffjoin", "leave_message").set("&b&lStaff - &c{rank} {player} has left the server");
            node.node("staffjoin", "change_message").set("&b&lStaff - &e{rank} {player} has changed the server to &b{server}");

            node.node("messages", "no_permission").set("&cYou don't have permission to use this command");
            node.node("messages", "no_console").set("&cOnly players can use this command");
            node.node("messages", "new_version_available").set("&cA new version of VelocityUtils is available (&b{version}&c)! &e{url}");
            node.node("messages", "alert_usage").set("&cUsage: /alert <message>");
            node.node("messages", "configuration_reloaded").set("&aConfiguration reloaded successfully! For some changes to take effect, you may need to restart the proxy.");
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
            node.node("messages", "helpop_usage").set("&cUsage: /helpop <reason>");
            node.node("messages", "helpop_cooldown").set("&cYou have {time}s before using /helpop again");
            node.node("messages", "helpop_hover").set("&bClick to teleport");
            node.node("messages", "helpop_sent").set("&aYour help request was sent");
            node.node("messages", "goto_usage").set("&cUsage: /goto <player>");
            node.node("messages", "goto_player_not_found").set("&cPlayer {player} not found");
            node.node("messages", "goto_server_not_found").set("&cServer could not be found");
            node.node("messages", "goto_same_server").set("&cYou are currently on the same server as {player}");
            node.node("messages", "goto_connecting").set("&aConnecting with {player} server");
            node.node("messages", "find_usage").set("&cUsage: /find <player>");
            node.node("messages", "find_player_not_found").set("&cPlayer {player} not found");
            node.node("messages", "server_unknown").set("Unknown");
            node.node("messages", "find_where").set("&b{player} &eis on &b{server}");
            node.node("messages", "stafflist_no_staff").set("&cThere are no staff online");
            node.node("messages", "stafflist_header").set("&b&lStaff List");
            node.node("messages", "stafflist_staff").set("{prefix} &f{player} &7- &b{server}");
            node.node("messages", "staffchat_disabled").set("&eStaff chat &cdisabled");
            node.node("messages", "staffchat_enabled").set("&eStaff chat &aenabled");
            node.node("messages", "staffchat_format").set("&8[&bStaffChat&8] &7{server} - {prefix} &b{player}&7: &f{message}");
            node.node("messages", "adminchat_disabled").set("&eAdmin chat &cdisabled");
            node.node("messages", "adminchat_enabled").set("&eAdmin chat &aenabled");
            node.node("messages", "adminchat_format").set("&8[&dAdminChat&8] &7{server} - {prefix} &d{player}&7: &f{message}");
            node.node("messages", "stafftime_usage").set("&cUsage: /stafftime <player> [day|week|month]");
            node.node("messages", "stafftime_not_found").set("&cPlayer {player} not found on the database.");
            node.node("messages", "stafftime_invalid_type").set("&cInvalid type. Use day, week or month");
            node.node("messages", "vlist_no_players").set("&cThere are no players online.");
            node.node("messages", "movecommands_no_servers").set("&cThere are no servers configured for this command");
            node.node("messages", "movecommands_server_not_found").set("&cThat server is not available at this moment.");
            node.node("messages", "movecommands_already_connected").set("&cYou are already connected to that server");
            node.node("messages", "messagescommands_no_message_console").set("&cThe messagecommand message is empty: {command}");
            node.node("messages", "messagescommands_no_action_or_hover_console").set("&cThe messagecommand {command} has action set to true, but no action or hover set");
            node.node("messages", "messagescommands_error_player").set("&cThat messagecommand doesnt work as intended, contact an administrator");

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

    public ConfigurationNode getRootNode() {
        try {
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}