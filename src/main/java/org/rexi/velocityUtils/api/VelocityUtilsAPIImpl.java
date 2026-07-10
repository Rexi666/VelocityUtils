package org.rexi.velocityUtils.api;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.DiscordWebhook;
import org.rexi.velocityUtils.VelocityUtils;
import org.rexi.velocityUtils.commands.AdminChatCommand;
import org.rexi.velocityUtils.commands.AlertCommand;
import org.rexi.velocityUtils.commands.StaffChatCommand;
import org.rexi.velocityUtils.commands.StaffListCommand;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class VelocityUtilsAPIImpl implements VelocityUtilsAPI {

    private final VelocityUtils plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final LuckPerms luckperms;
    private final DiscordWebhook webhook;

    public VelocityUtilsAPIImpl(VelocityUtils plugin, ProxyServer server, ConfigManager configManager, LuckPerms luckPerms, DiscordWebhook webhook) {
        this.plugin = plugin;
        this.server = server;
        this.configManager = configManager;
        this.luckperms = luckPerms;
        this.webhook = webhook;
    }

    @Override
    public void sendAlert(String message) {
        new AlertCommand(configManager, server, plugin).sendAlert(message);
    }

    public Map<String, String[]> getStaffList() {
        StaffListCommand staffListCommand = new StaffListCommand(configManager, server, luckperms, plugin);
        List<Player> staff = staffListCommand.getStaffOnline();
        if (staff.isEmpty()) {
            return Map.of();
        }

        Map<String, String[]> staffList = new HashMap<>(); // Player name, Prefix, Server

        for (Player player : staff) {
            String rank = staffListCommand.obtenerRangoPrincipal(player);

            String serverName = player.getCurrentServer()
                    .map(s -> s.getServerInfo().getName())
                    .orElse(configManager.getMessage("server_unknown"));

            staffList.put(player.getUsername(), new String[]{rank, serverName});
        }
        return staffList;
    }

    public Map<String, List<String>> getList(Boolean byRank) {
        if (server.getAllPlayers().isEmpty()) return Map.of();

        StaffListCommand staffListCommand = new StaffListCommand(configManager, server, luckperms, plugin);
        Map<String, List<String>> finalList = new HashMap<>();

        if (byRank) {
            Map<String, Integer> rangosWeight = new HashMap<>();

            for (Player player : server.getAllPlayers()) {
                String rank = staffListCommand.obtenerRangoPrincipal(player);
                int weight = staffListCommand.getGroupWeight(player);
                rangosWeight.put(rank, weight);
                finalList.computeIfAbsent(rank, k -> new ArrayList<>()).add(player.getUsername());
            }

            finalList = finalList.entrySet()
                    .stream()
                    .sorted((e1, e2) -> rangosWeight.get(e2.getKey()) - rangosWeight.get(e1.getKey()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        } else {
            for (Player player : server.getAllPlayers()) {
                String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessage("server_unknown"));
                finalList.computeIfAbsent(serverName, k -> new ArrayList<>()).add(player.getUsername());
            }
        }
        return finalList;
    }

    public void sendStaffChatMessage(String playerName, String message, @Nullable String serverName) {
        String format = configManager.getMessage("staffchat_format")
                .replace("{player}", playerName)
                .replace("{message}", message)
                .replace("{server}", serverName != null ? serverName : configManager.getMessage("server_unknown"))
                .replace("{prefix}", "");

        server.getAllPlayers().forEach(target -> {
            if (target.hasPermission("velocityutils.staffchat")) {
                target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            }
        });

        server.getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(format));

        if (configManager.getBoolean("staffchat.discord_hook.enabled")) {
            String raw = configManager.getString("staffchat.discord_hook.message");
            String msgToSend = raw
                    .replace("{player}", playerName)
                    .replace("{message}", message)
                    .replace("{server}", serverName != null ? serverName : configManager.getMessage("server_unknown"));

            new StaffChatCommand(plugin, configManager, server, webhook, luckperms).sendStaffChatWebhook(playerName, msgToSend);
        }
    }

    public void sendAdminChatMessage(String playerName, String message, @Nullable String serverName) {
        String format = configManager.getMessage("adminchat_format")
                .replace("{player}", playerName)
                .replace("{message}", message)
                .replace("{server}", serverName != null ? serverName : configManager.getMessage("server_unknown"))
                .replace("{prefix}", "");

        server.getAllPlayers().forEach(target -> {
            if (target.hasPermission("velocityutils.adminchat")) {
                target.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(format));
            }
        });

        server.getConsoleCommandSource().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(format));

        if (configManager.getBoolean("adminchat.discord_hook.enabled")) {
            String raw = configManager.getString("adminchat.discord_hook.message");
            String msgToSend = raw
                    .replace("{player}", playerName)
                    .replace("{message}", message)
                    .replace("{server}", serverName != null ? serverName : configManager.getMessage("server_unknown"));

            new AdminChatCommand(plugin, configManager, server, webhook, luckperms).sendAdminChatWebhook(playerName, msgToSend);
        }
    }
}
