package org.rexi.velocityUtils.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.rexi.velocityUtils.ConfigManager;

import java.util.*;

public class VListCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final ProxyServer server;
    private final LuckPerms luckPerms;

    public VListCommand(ConfigManager configManager, ProxyServer server, LuckPerms luckPerms) {
        this.configManager = configManager;
        this.server = server;
        this.luckPerms = luckPerms;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocityutils.vlist")) {
            source.sendMessage(legacy(configManager.getMessage("no_permission")));
            return;
        }

        String mode;
        if (args.length > 0 && (args[0].equalsIgnoreCase("server") || args[0].equalsIgnoreCase("rank"))) {
            mode = args[0].toLowerCase();
        } else {
            mode = configManager.getString("vlist.default_mode").toLowerCase();
        }

        int playerCount = server.getAllPlayers().size();
        if (playerCount == 0) {
            source.sendMessage(legacy(configManager.getMessage("vlist_no_players")));
            return;
        }

        if (mode.equals("rank")) {
            mostrarPorRangos(source, playerCount);
        } else {
            mostrarPorServidores(source, playerCount);
        }
    }

    private void mostrarPorServidores(CommandSource source, int totalPlayers) {
        Map<String, List<String>> servidores = new HashMap<>();

        for (Player player : server.getAllPlayers()) {
            String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse(configManager.getMessage("server_unknown"));
            servidores.computeIfAbsent(serverName, k -> new ArrayList<>()).add(player.getUsername());
        }

        List<String> lines = configManager.getStringList("vlist.server.message");
        for (String line : lines) {
            if (line.contains("{servercount}")) {
                for (Map.Entry<String, List<String>> entry : servidores.entrySet()) {
                    String formatted = configManager.getString("vlist.server.servercount")
                            .replace("{server}", entry.getKey())
                            .replace("{count}", String.valueOf(entry.getValue().size()))
                            .replace("{players}", String.join(", ", entry.getValue()));
                    source.sendMessage(legacy(formatted));
                }
            } else {
                source.sendMessage(legacy(line.replace("{count}", String.valueOf(totalPlayers))));
            }
        }
    }

    private void mostrarPorRangos(CommandSource source, int totalPlayers) {
        // Map de rango -> lista de jugadores
        Map<String, List<String>> rangos = new HashMap<>();
        // Map de rango -> weight
        Map<String, Integer> rangosWeight = new HashMap<>();
        Map<String, Component> rangosComponentes = new HashMap<>();

        for (Player player : server.getAllPlayers()) {
            String rangoRaw = obtenerRango(player);
            Component prefix = deserializePrefix(rangoRaw);
            int weight = obtenerWeightRango(player);

            rangos.computeIfAbsent(rangoRaw, k -> new ArrayList<>()).add(player.getUsername());
            rangosWeight.put(rangoRaw, weight); // Guardamos el weight asociado al rango
            rangosComponentes.put(rangoRaw, prefix);
        }

        List<String> lines = configManager.getStringList("vlist.rank.message");
        for (String line : lines) {
            if (line.contains("{rankcount}")) {
                rangos.entrySet().stream()
                        .sorted((e1, e2) -> Integer.compare(
                                rangosWeight.getOrDefault(e2.getKey(), 0),
                                rangosWeight.getOrDefault(e1.getKey(), 0)
                        ))
                        .forEach(entry -> {
                            String semiFormatted = configManager.getString("vlist.rank.rankcount")
                                    .replace("{count}", String.valueOf(entry.getValue().size()))
                                    .replace("{players}", String.join(", ", entry.getValue()));

                            Component formatted = LegacyComponentSerializer.legacyAmpersand().deserialize(semiFormatted);

                            formatted = formatted.replaceText(TextReplacementConfig.builder()
                                    .matchLiteral("{rank}")
                                    .replacement(rangosComponentes.getOrDefault(entry.getKey(), Component.text(entry.getKey())))
                                    .build());

                            source.sendMessage(formatted);
                        });
            } else {
                source.sendMessage(legacy(line.replace("{count}", String.valueOf(totalPlayers))));
            }
        }
    }

    private String obtenerRango(Player player) {
        if (luckPerms == null) return "Default";

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            String primaryGroupName = user.getPrimaryGroup();
            var group = luckPerms.getGroupManager().getGroup(primaryGroupName);

            if (group != null) {
                String groupPrefix = group.getCachedData().getMetaData().getPrefix();
                if (groupPrefix != null && !groupPrefix.isEmpty()) {
                    return groupPrefix;
                }
                return primaryGroupName; // Si el grupo no tiene prefix, devolvemos el nombre del grupo
            }
            return primaryGroupName; // Si no encuentra el grupo, devolvemos el nombre
        }
        return "Default";
    }

    private int obtenerWeightRango(Player player) {
        if (luckPerms == null) return 0;

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            var group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
            if (group != null && group.getWeight().isPresent()) {
                return group.getWeight().getAsInt();
            }
        }
        return 0; // Default weight
    }


    private Component legacy(String s) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            // No se ha escrito nada aún, sugerimos los modos disponibles
            return List.of("rank", "server");
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> options = List.of("rank", "server");
            List<String> suggestions = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(partial)) {
                    suggestions.add(option);
                }
            }
            return suggestions;
        }

        return Collections.emptyList();
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

}