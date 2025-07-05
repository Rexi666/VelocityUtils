package org.rexi.velocityUtils;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {

    private final ConfigManager configManager;

    private final String webhookUrl;
    private String avatarUrl;
    private String username;
    private String title;
    private int[] colorRGB = new int[]{240, 43, 20};

    public DiscordWebhook(String webhookUrl, ConfigManager configManager) {
        this.webhookUrl = webhookUrl;
        this.configManager = configManager;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setColorRGB(String rgb) {
        try {
            String[] parts = rgb.split(",");
            if (parts.length == 3) {
                colorRGB = new int[]{
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim())
                };
            }
        } catch (Exception ignored) {}
    }

    public void sendReport(String content) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            int color = (colorRGB[0] << 16) + (colorRGB[1] << 8) + colorRGB[2];

            String payload = """
            {
              "username": "%s",
              "avatar_url": "%s",
              "embeds": [{
                "title": "%s",
                "description": "%s",
                "color": %d
              }]
            }
            """.formatted(
                    escape(username),
                    escape(avatarUrl),
                    escape(title),
                    escape(content),
                    color
            );

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            connection.getResponseCode(); // opcional: fuerza la ejecución
        } catch (Exception e) {
            String report_webhook_error = configManager.getMessage("report_webhook_error");
            System.err.println(LegacyComponentSerializer.legacyAmpersand().deserialize(report_webhook_error));
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
}
