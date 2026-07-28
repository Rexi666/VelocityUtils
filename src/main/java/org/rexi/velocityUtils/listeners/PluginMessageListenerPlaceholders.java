package org.rexi.velocityUtils.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.rexi.velocityUtils.managers.ConfigManager;
import org.rexi.velocityUtils.utils.tebex.DonorStats;
import org.rexi.velocityUtils.utils.tebex.TebexService;
import org.rexi.velocityUtils.utils.tebex.TopDonor;

import java.util.UUID;

public class PluginMessageListenerPlaceholders {

    private final ProxyServer server;
    private final TebexService tebexService;
    private final ConfigManager configManager;
    private final MinecraftChannelIdentifier PLACEHOLDER_CHANNEL = MinecraftChannelIdentifier.create("velocityutils", "placeholders");

    public PluginMessageListenerPlaceholders(ProxyServer server, ConfigManager configManager, TebexService tebexService) {
        this.server = server;
        this.configManager = configManager;
        this.tebexService = tebexService;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(PLACEHOLDER_CHANNEL)) return;

        if (!(event.getSource() instanceof ServerConnection serverConn)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String playerUUIDStr = in.readUTF();
        String placeholder = in.readUTF();

        // Tebex placeholders - async

        if (placeholder.startsWith("tebex_")) {
            UUID playerUUID;
            try { playerUUID = UUID.fromString(playerUUIDStr); }
            catch (IllegalArgumentException e) { return; }

            tebexService.getStats().thenAccept(stats ->
                    sendResponse(serverConn, placeholder, resolveTebex(placeholder, stats))
            );
            return; // respuesta se envía en el callback
        }

        // Sync placeholders

        String response = switch (placeholder) {
            case "globalplayers" ->  {
                String val = String.valueOf(server.getAllPlayers().size());
                yield val;
            }
            default -> {
                if (placeholder.startsWith("players_")) {
                    String serverName = placeholder.substring("players_".length());
                    var serverOpt = server.getServer(serverName);
                    if (serverOpt.isPresent()) {
                        yield String.valueOf(serverOpt.get().getPlayersConnected().size());
                    } else {
                        yield "0";
                    }
                } else yield "null";
            }
        };

        UUID playerUUID;
        try {
            playerUUID = UUID.fromString(playerUUIDStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        sendResponse(serverConn, placeholder, response);
    }

    // ------------------------------------------------------------------ //
    //  Tebex placeholders resolution                                     //
    // ------------------------------------------------------------------ //

    private String resolveTebex(String placeholder, DonorStats s) {
        if (!configManager.getBoolean("tebex_link.enabled")) return "null";
        if (configManager.getString("tebex_link.secret").equalsIgnoreCase("YOUR_TEBEX_SECRET_KEY")) return "null";

        return switch (placeholder) {
            // Nombres
            case "tebex_topdonor_alltime_name" -> name(s.allTimeLeader());
            case "tebex_topdonor_month_name"   -> name(s.monthlyLeader());
            case "tebex_topdonor_week_name"    -> name(s.weeklyLeader());
            case "tebex_topdonor_day_name"     -> name(s.dailyLeader());
            // Dinero
            case "tebex_topdonor_alltime_money" -> money(s.allTimeLeader());
            case "tebex_topdonor_month_money"   -> money(s.monthlyLeader());
            case "tebex_topdonor_week_money"    -> money(s.weeklyLeader());
            case "tebex_topdonor_day_money"     -> money(s.dailyLeader());
            // Ingresos
            case "tebex_revenue_alltime"      -> fmt(s.revenueAllTime());
            case "tebex_revenue_month"        -> fmt(s.revenueThisMonth());
            case "tebex_revenue_week"         -> fmt(s.revenueThisWeek());
            case "tebex_revenue_day"          -> fmt(s.revenueToday());
            default -> "null";
        };
    }

    private String name(TopDonor d)  { return d != null ? d.playerName() : "N/A"; }
    private String money(TopDonor d) { return d != null ? fmt(d.totalAmount()) : "0.00"; }
    private String fmt(double v)     { return String.format("%.2f", v); }

    // Send response

    private void sendResponse(ServerConnection conn, String placeholder, String value) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(placeholder);
        out.writeUTF(value);
        conn.sendPluginMessage(PLACEHOLDER_CHANNEL, out.toByteArray());
    }
}