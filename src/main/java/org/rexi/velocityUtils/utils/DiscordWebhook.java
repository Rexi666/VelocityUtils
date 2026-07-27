package org.rexi.velocityUtils.utils;

import org.rexi.velocityUtils.ConfigManager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DiscordWebhook {

    private final ConfigManager configManager;

    public DiscordWebhook(ConfigManager configManager) {
        this.configManager = configManager;
    }

    int[] colorRGBfinal = new int[]{240, 43, 20};

    public void send(String content, String webhookUrl, String avatarUrl, String username, String colorRGB, String thumbnailUrl, String title) {
        setColorRGB(colorRGB);
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            int color = (colorRGBfinal[0] << 16) + (colorRGBfinal[1] << 8) + colorRGBfinal[2];

            String payload = """
            {
              "username": "%s",
              "avatar_url": "%s",
              "embeds": [{
                "title": "%s",
                "description": "%s",
                "color": %d,
                "thumbnail": {
                  "url": "%s"
                }
              }]
            }
            """.formatted(
                    escape(username),
                    escape(avatarUrl),
                    escape(title),
                    escape(content),
                    color,
                    escape(thumbnailUrl)
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            connection.getResponseCode(); // fuerza la ejecución
        } catch (Exception e) {
            System.err.println(configManager.getMessage("report_webhook_error"));
            e.printStackTrace();
        }
    }

    private String escape(String s) {
        return s == null ? "" : s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    public static String getUuidFromName(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                String responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";

                int idKeyIndex = responseBody.indexOf("\"id\"");
                if (idKeyIndex == -1) return null;

                int colonIndex = responseBody.indexOf(":", idKeyIndex);
                if (colonIndex == -1) return null;

                int quoteStart = responseBody.indexOf("\"", colonIndex);
                if (quoteStart == -1) return null;

                int quoteEnd = responseBody.indexOf("\"", quoteStart + 1);
                if (quoteEnd == -1) return null;

                return responseBody.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPlayerAvatar(String playerName) {
        String uuid = getUuidFromName(playerName);
        String avatar = (uuid != null)
                ? "https://minotar.net/helm/" + uuid + "/64.png"
                : "https://i.pinimg.com/564x/54/f4/b5/54f4b55a59ff9ddf2a2655c7f35e4356.jpg";
        return avatar;
    }

    public void setColorRGB(String color) {
        try {
            String[] parts = color.split(",");
            if (parts.length == 3) {
                colorRGBfinal = new int[]{
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                };
            }
        } catch (Exception ignored) {}
    }
}
