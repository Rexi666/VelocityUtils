package org.rexi.velocityUtils.api;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface VelocityUtilsAPI {

    /**
     * Send an alert to all network members.
     *
     * @param message The message you want to send as an alert.
     */
    void sendAlert(String message);

    /**
     * Obtain a list of staff members online.
     *
     * @return A map containing Staff names, and an Array with the rank (0) and server (1).
     *          If no online staff, returns an empty map.
     */
    Map<String, String[]> getStaffList();

    /**
     * Obtain a list of every player online.
     *
     * @return If true, a map containing Rank, and a list with every player with that rank.
     *         If false, a map containing Server, and a list with every player on that server.
     *         If no players online, returns an empty map.
     */
    Map<String, List<String>> getList(Boolean byRank);

    /**
     * Send a message to all staff members.
     *
     * @param playerName The name of the player sending the message.
     * @param message The message to send.
     * @param serverName The name of the server the player is on. Can be null (to indicate unknown server).
     */
    void sendStaffChatMessage(String playerName, String message, @Nullable String serverName);

    /**
     * Send a message to all admin members.
     *
     * @param playerName The name of the player sending the message.
     * @param message The message to send.
     * @param serverName The name of the server the player is on. Can be null (to indicate unknown server).
     */
    void sendAdminChatMessage(String playerName, String message, @Nullable String serverName);
}
