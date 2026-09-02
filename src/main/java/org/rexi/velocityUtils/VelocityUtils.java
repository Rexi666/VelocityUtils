package org.rexi.velocityUtils;

import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.velocity.Metrics;
import org.rexi.velocityUtils.api.VelocityUtilsAPI;
import org.rexi.velocityUtils.api.VelocityUtilsAPIImpl;
import org.rexi.velocityUtils.api.VelocityUtilsProvider;
import org.rexi.velocityUtils.commands.*;
import org.rexi.velocityUtils.commands.banSystem.*;
import org.rexi.velocityUtils.listeners.*;
import org.rexi.velocityUtils.managers.*;
import org.rexi.velocityUtils.utils.*;
import org.rexi.velocityUtils.utils.tebex.TebexService;
import org.slf4j.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "velocityutils",
        name = "VelocityUtils",
        version = BuildConstants.VERSION,
        authors = {"Rexi666"},
        dependencies = {
                @Dependency(id = "luckperms"),
                @Dependency(id = "limboapi", optional = true)}
        )
public class VelocityUtils {

    private final ProxyServer server;

    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final BanManager banManager;
    private CommandManager commandManager; // se inicializa más tarde con luckperms, por eso no final
    private final AlertManager alertManager;
    private final PluginMessageManager pluginMessageManager;

    private final PluginContainer plugin;
    private final BrandListener brandListener;
    private TebexService tebexService;
    private LuckPerms luckPerms = null;
    private VelocityUtilsAPI api;
    private final UpdateChecker updateChecker;
    private final DateUtils dateUtils;

    private final MotdListener motdListener;

    private final DiscordWebhook webhook;

    public final Set<UUID> staffChatToggled = ConcurrentHashMap.newKeySet();
    public final Set<UUID> adminChatToggled = ConcurrentHashMap.newKeySet();

    private final Map<UUID, StaffSession> staffSessions = new ConcurrentHashMap<>();

    public final Map<String, List<String>> pendingCommands = new HashMap<>();

    public List<UUID> disabledSC = new ArrayList<>();

    @Inject public Logger logger;

    @Inject
    public VelocityUtils(ProxyServer server, PluginContainer plugin) {
        this.server = server;
        this.plugin = plugin;

        this.configManager = new ConfigManager();
        this.databaseManager = new DatabaseManager(configManager, logger);
        this.banManager = new BanManager(configManager, databaseManager, server);
        this.pluginMessageManager = new PluginMessageManager(server, logger);
        this.alertManager = new AlertManager(server, configManager, logger, pluginMessageManager, this);

        this.brandListener = new BrandListener(configManager, server);
        this.webhook = new DiscordWebhook(configManager);
        this.updateChecker = new UpdateChecker(server, plugin, configManager, BuildConstants.VERSION);
        this.dateUtils = new DateUtils(configManager);
        this.motdListener = new MotdListener(configManager);
    }

    @Inject private Metrics.Factory metricsFactory;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        pluginMessageManager.registerChannels();

        configManager.loadConfig();
        configManager.loadMessages();

        databaseManager.createTables();

        tebexService = new TebexService(logger, configManager.getString("tebex_link.secret"), configManager.getInt("tebex_link.refresh_minutes"));
        try {
            this.luckPerms = LuckPermsProvider.get();
            logger.info("[VelocityUtils] LuckPerms detected.");
        } catch (IllegalStateException e) {
            this.luckPerms = null;
            logger.warn("[VelocityUtils] LuckPerms not detected.");
        }

        String version = plugin.getDescription().getVersion().orElse("Unknown");
        String author = plugin.getDescription().getAuthors().stream().findFirst().orElse("Unknown");

        commandManager = new CommandManager(
                server,
                configManager,
                this,
                databaseManager,
                banManager,
                luckPerms,
                webhook,
                brandListener,
                alertManager,
                pluginMessageManager,
                tebexService,
                motdListener,
                version,
                author
        );

        if (this.server.getPluginManager().getPlugin("limboapi").isPresent()) {
            server.getEventManager().register(this, new LimboAPIListener(this));
        }

        server.getEventManager().register(this, new ChatListener(this));
        server.getEventManager().register(this, new StaffConnectionListener(staffSessions, configManager, server, luckPerms, webhook, dateUtils, databaseManager));
        server.getEventManager().register(this, brandListener);
        server.getEventManager().register(this, new ServerWhitelistListener(configManager, server, this, pluginMessageManager));

        server.getEventManager().register(this, new PluginMessageListenerStaffChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerAdminChat(this, server, configManager, webhook, luckPerms));
        server.getEventManager().register(this, new PluginMessageListenerPlaceholders(server, configManager, tebexService));
        server.getEventManager().register(this, new PluginMessageListenerAlerts(server, configManager));

        server.getEventManager().register(this, new ServerExecuteListener(this, server, pluginMessageManager));

        server.getEventManager().register(this, banManager);
        server.getEventManager().register(this, updateChecker);

        server.getEventManager().register(this, motdListener);
        motdListener.reload();
        server.getEventManager().register(this, new MaintenanceListener(configManager, motdListener));

        commandManager.registerCommands();
        commandManager.registerMoveCommands();
        commandManager.registerMessagesCommands();
        alertManager.startRegularAlerts();

        if (configManager.getBoolean("tebex_link.enabled")
                && !configManager.getString("tebex_link.secret").equalsIgnoreCase("YOUR_TEBEX_SECRET_KEY")) {
            tebexService.refresh();
        }

        Metrics metrics = metricsFactory.make(this, 26742);

        this.api = new VelocityUtilsAPIImpl(this, server, configManager, luckPerms, webhook, pluginMessageManager);
        VelocityUtilsProvider.register(this.api);

        server.sendMessage(Component.text("VelocityUtils has been activated").color(NamedTextColor.GREEN));
        server.sendMessage(Component.text("Thank you for using Rexi666 plugins").color(NamedTextColor.BLUE));

        updateChecker.checkForUpdatesConsole();
    }

    public VelocityUtilsAPI getAPI() {
        return api;
    }

    public Set<UUID> getStaffChatToggled() {
        return staffChatToggled;
    }

    public Set<UUID> getAdminChatToggled() {
        return adminChatToggled;
    }

    public Map<UUID, StaffSession> getStaffSessions() {
        return staffSessions;
    }

    public final Map<UUID, String> playersInSpecialServers = new ConcurrentHashMap<>();

    public String getServerName(Player player) {
        // Primero comprueba si está en un Limbo
        if (playersInSpecialServers.containsKey(player.getUniqueId())) {
            return playersInSpecialServers.get(player.getUniqueId()); // ej: "Limbo"
        }
        // Si no, el servidor normal de Velocity
        return player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse(configManager.getMessageString("server_unknown"));
    }

    // Vmsg

    public Map<UUID, UUID> messageReplies = new HashMap<>();
    private Map<UUID, Set<UUID>> ignoredPlayers = new HashMap<>();
    public Map<UUID, List<UUID>> spyPlayers = new HashMap<>(); // Target - List<UUID> of players spying on them
    public List<UUID> spyGlobalPlayers = new ArrayList<>();

    public void getIgnoredPlayersFromDB(Player player) {
        try {
            ignoredPlayers.put(
                    player.getUniqueId(),
                    databaseManager.getIgnoredPlayers(player.getUniqueId().toString())
            );
        } catch (SQLException e) {
            logger.warn("Failed to load player ignored list on the database: " + e.getMessage());
        }
    }
    public void setIgnoredPlayers(UUID player, UUID ignored, boolean active) {
        if (active) {
            ignoredPlayers.computeIfAbsent(player, k -> new HashSet<>()).add(ignored);
            try {
                databaseManager.addIgnoredPlayer(player.toString(), ignored.toString());
            } catch (SQLException e) {
                logger.warn("Failed to save player ignored status on the database: " + e.getMessage());
            }
        } else {
            Set<UUID> ignoredSet = ignoredPlayers.get(player);
            if (ignoredSet != null) {
                ignoredSet.remove(ignored);
            }
            try {
                databaseManager.removeIgnoredPlayer(player.toString(), ignored.toString());
            } catch (SQLException e) {
                logger.warn("Failed to save player ignored status on the database: " + e.getMessage());
            }
        }
    }
    public boolean checkIgnoredPlayers(UUID player, UUID ignored) {
        return ignoredPlayers.getOrDefault(player, Collections.emptySet()).contains(ignored);
    }
    public void cacheRemoveIgnoredPlayers(Player player) {
        ignoredPlayers.remove(player.getUniqueId());
    }

    public void removeSpy(UUID spyUUID) {
        Iterator<Map.Entry<UUID, List<UUID>>> iterator = spyPlayers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, List<UUID>> entry = iterator.next();

            List<UUID> spies = entry.getValue();
            spies.remove(spyUUID);

            if (spies.isEmpty()) {
                iterator.remove();
            }
        }
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        getIgnoredPlayersFromDB(player);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        cacheRemoveIgnoredPlayers(player);
        UUID playerUUID = player.getUniqueId();
        spyGlobalPlayers.remove(playerUUID);
        removeSpy(playerUUID);
    }

    public boolean isPlayerInDisabledServer(Player player) {
        String serverName = player.getCurrentServer()
                .map(s -> s.getServerInfo().getName())
                .orElse(configManager.getMessageString("server_unknown"));
        if (configManager.getStringList("disabled_features_servers").contains(serverName)) {
            return true;
        } else {
            return false;
        }
    }
}
