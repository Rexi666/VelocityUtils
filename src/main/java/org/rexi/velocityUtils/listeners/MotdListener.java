package org.rexi.velocityUtils.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.rexi.velocityUtils.managers.ConfigManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;

public class MotdListener {

    private final ConfigManager configManager;

    private Component normalMotd;
    private Component maintenanceMotd;
    private boolean motdEnabled;
    private boolean maintenanceEnabled;

    private boolean serverIconEnabled;
    private Favicon serverIconFavicon;

    public MotdListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void reload() {
        motdEnabled = configManager.getBoolean("motd.enabled");
        maintenanceEnabled = configManager.getBoolean("maintenance.active");

        normalMotd = buildMotd(
                configManager.getString("motd.line1"),
                configManager.getString("motd.line2")
        );

        maintenanceMotd = buildMotd(
                configManager.getString("maintenance.motd.line1"),
                configManager.getString("maintenance.motd.line2")
        );

        serverIconEnabled = configManager.getBoolean("server_icon.enabled");
        serverIconFavicon = null;
        if (serverIconEnabled) {
            try {
                File icon = new File("plugins/VelocityUtils", configManager.getString("server_icon.file"));
                if (icon.exists()) {
                    BufferedImage image = ImageIO.read(icon);

                    if (image != null) {
                        if (image.getWidth() != 64 || image.getHeight() != 64) {
                            image = resizeImage(image, 64, 64);
                        }
                        ImageIO.write(image, "png", icon);

                        serverIconFavicon = Favicon.create(image);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Component buildMotd(String line1, String line2) {
        // Convertir
        Component component1 = line1.contains("<") && line1.contains(">") ? MINI_MESSAGE.deserialize(line1) : configManager.legacy(line1);
        Component component2 = line2.contains("<") && line2.contains(">") ? MINI_MESSAGE.deserialize(line2) : configManager.legacy(line2);

        return Component.text()
                .append(component1)
                .append(Component.newline())
                .append(component2)
                .build();
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder builder = event.getPing().asBuilder();
        if (motdEnabled || maintenanceEnabled) {
            Component motd = maintenanceEnabled
                    ? maintenanceMotd
                    : normalMotd;

                    builder.description(motd);
        }
        if (serverIconEnabled) {
            if (serverIconFavicon != null) {
                builder.favicon(serverIconFavicon);
            }
        }

        if (motdEnabled || maintenanceEnabled || serverIconEnabled) {
            event.setPing(builder.build());
        }
    }

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    public void changeMaintenanceActive(boolean enabled) {
        maintenanceEnabled = enabled;
    }

    public boolean getMaintenanceStatus() {
        return maintenanceEnabled;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }
}
