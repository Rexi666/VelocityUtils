package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.ConfigManager;
import org.rexi.velocityUtils.utils.DefaultFontInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StreamCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final LuckPerms luckPerms;

    public StreamCommand(ConfigManager configManager, ProxyServer server, LuckPerms luckPerms) {
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
    }

    private final Map<UUID, Long> streamCooldowns = new HashMap<>();

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(legacy(configManager.getMessage("no_console")));
            return;
        }

        if (!player.hasPermission("velocityutils.stream")) {
            player.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        long now = System.currentTimeMillis();
        long cooldown = configManager.getInt("stream.cooldown_seconds") * 1000L;
        Long lastUse = streamCooldowns.get(player.getUniqueId());

        if (lastUse != null && now - lastUse < cooldown) {
            long remaining = (cooldown - (now - lastUse)) / 1000; // en segundos
            long minutes = remaining / 60;
            long seconds = remaining % 60;

            String minute_simbol = configManager.getMessage("minute_simbol");
            String second_simbol = configManager.getMessage("second_simbol");

            String cooldownFormatted;
            if (minutes > 0) {
                cooldownFormatted = minutes + minute_simbol + " " + seconds + second_simbol;
            } else {
                cooldownFormatted = seconds + second_simbol;
            }

            player.sendMessage(legacy(configManager.getMessage("stream_cooldown")
                    .replace("{cooldown}", cooldownFormatted)));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(legacy(configManager.getMessage("stream_usage")));
            return;
        }

        String url = args[0];
        if (configManager.getBoolean("stream.whitelist")) {
            List<String> whitelistLinks = configManager.getStringList("stream.whitelist_links");
            boolean whitelisted = false;
            for (String link : whitelistLinks) {
                if (url.startsWith(link)) {
                    whitelisted = true;
                    break;
                }
            }
            if (whitelisted) {
                streamCooldowns.put(player.getUniqueId(), now);
                sendMessage(player, url);
            } else {
                player.sendMessage(legacy(configManager.getMessage("stream_invalid_url")));
            }
        } else {
            sendMessage(player, url);
            streamCooldowns.put(player.getUniqueId(), now);
        }
    }

    private static final LegacyComponentSerializer LEGACY_HEX_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors() // Habilita el soporte de hex
            .useUnusualXRepeatedCharacterHexFormat() // Soporta &x&r&r&g&g&b&b
            .build();
    private Component legacy(String s) {
        return LEGACY_HEX_SERIALIZER.deserialize(s);
    }

    private void sendMessage(Player player, String url) {
        String rangoRaw = obtenerRango(player);
        Component prefix = deserializePrefix(rangoRaw);
        String semiformated = configManager.getString("stream.message")
                .replace("{player}", player.getUsername())
                .replace("{url}", url);

        List<String> messageList = configManager.getStringList("stream.messagelist");
        boolean list = false;
        if (!messageList.isEmpty()) {
            list = true;
        }
        Component semibasemessage = legacy(semiformated);

        Component baseMessage = semibasemessage.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{rank}")
                .replacement(prefix)
                .build());

        Component finalMessage;
        boolean hoverEnabled = configManager.getBoolean("stream.hover_enabled");
        if (hoverEnabled) {
            String hover = configManager.getString("stream.hover");
            finalMessage = baseMessage
                    .clickEvent(ClickEvent.openUrl(url))
                    .hoverEvent(HoverEvent.showText(
                            legacy(hover)));
        } else {
            finalMessage = baseMessage.clickEvent(ClickEvent.openUrl(url));
        }

        if (list) {
            String hover = configManager.getString("stream.hover");
            for (String line : messageList) {
                line = line
                        .replace("{player}", player.getUsername())
                        .replace("{url}", url);
                if (line.startsWith("[center]")) {
                    line = line.replace("[center]", "");
                    line = getCenteredMessage(line);
                }
                Component semiLine = legacy(line);
                Component baseLine = semiLine.replaceText(TextReplacementConfig.builder()
                        .matchLiteral("{rank}")
                        .replacement(prefix)
                        .build());

                Component finalLine;
                if (hoverEnabled) {
                    finalLine = baseLine
                            .clickEvent(ClickEvent.openUrl(url))
                            .hoverEvent(HoverEvent.showText(
                                    legacy(hover)));
                } else {
                    finalLine = baseLine.clickEvent(ClickEvent.openUrl(url));
                }
                for (Player onlinePlayer : server.getAllPlayers()) {
                    onlinePlayer.sendMessage(finalLine);
                }
            }
        } else {
            for (Player onlinePlayer : server.getAllPlayers()) {
                onlinePlayer.sendMessage(finalMessage);
            }
        }
    }

    private String obtenerRango(Player player) {
        if (luckPerms == null) return "";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "";

        // Primero intentamos obtener el prefix del propio usuario (o el que LuckPerms determine como prioritario)
        String prefix = user.getCachedData().getMetaData().getPrefix();
        if (prefix != null && !prefix.isEmpty()) {
            return prefix;
        }

        // Si no tiene prefix propio, usamos el del grupo principal
        String primaryGroupName = user.getPrimaryGroup();
        var group = luckPerms.getGroupManager().getGroup(primaryGroupName);
        if (group != null) {
            String groupPrefix = group.getCachedData().getMetaData().getPrefix();
            if (groupPrefix != null && !groupPrefix.isEmpty()) {
                return groupPrefix;
            }
            return primaryGroupName;
        }

        return primaryGroupName;
    }

    private Component deserializePrefix(String input) {
        // Si contiene <...> asumimos que es MiniMessage
        if (input.contains("<") && input.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(input);
            } catch (Exception e) {
                // En caso de error, usa como texto plano
                return Component.text(input);
            }
        }

        // Si no, asumimos que es con códigos &
        return LegacyComponentSerializer.legacyAmpersand().deserialize(input);
    }

    public static String getCenteredMessage(String message){
        int CENTER_PX = 154;
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;

        for(char c : message.toCharArray()){
            if(c == '§'){
                previousCode = true;
                continue;
            }else if(previousCode == true){
                previousCode = false;
                if(c == 'l' || c == 'L'){
                    isBold = true;
                    continue;
                } else isBold = false;
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
        return (sb.toString() + message);
    }
}
