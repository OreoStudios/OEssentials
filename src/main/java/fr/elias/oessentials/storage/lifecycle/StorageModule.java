package fr.elias.oessentials.storage.lifecycle;

import com.mongodb.client.MongoClient;
import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.storage.api.BalanceImportTarget;
import fr.elias.oessentials.storage.internal.migration.cmi.CMIMigrateCommand;
import fr.elias.oessentials.storage.internal.migration.commands.ZEssentialsHomesImportCommand;
import fr.elias.oessentials.storage.internal.migration.commands.ZImportCommand;
import fr.elias.oessentials.storage.internal.migration.essentialsx.command.MigrateEssentialsXCommand;
import fr.elias.oessentials.storage.internal.persistence.mongoservices.MongoHomeDirectory;
import fr.elias.oessentials.storage.internal.persistence.mongoservices.MongoHomesMigrator;
import fr.elias.oessentials.storage.internal.persistence.mongoservices.MongoHomesStorage;
import fr.elias.oessentials.storage.internal.persistence.mongoservices.MongoSpawnDirectory;
import fr.elias.oessentials.storage.internal.persistence.mongoservices.MongoWarpDirectory;
import fr.elias.oessentials.storage.internal.homes.home.HomeDirectory;
import fr.elias.oessentials.storage.internal.homes.home.HomeService;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpDirectory;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpService;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpStorage;
import fr.elias.oessentials.storage.internal.playerwarp.mongo.MongoPlayerWarpDirectory;
import fr.elias.oessentials.storage.internal.playerwarp.mongo.MongoPlayerWarpStorage;
import fr.elias.oessentials.storage.internal.spawn.SpawnDirectory;
import fr.elias.oessentials.storage.internal.spawn.SpawnService;
import fr.elias.oessentials.storage.internal.warps.WarpService;
import fr.elias.oessentials.storage.internal.warps.rabbit.WarpDirectory;
import fr.elias.oessentials.storage.internal.services.JsonStorage;
import fr.elias.oessentials.storage.internal.services.StorageApi;
import fr.elias.oessentials.storage.internal.services.yaml.YamlPlayerWarpStorage;

@PluginModule(value = "storage", dependencies = {"configuration::services", "messaging::events", "messaging::services"}, after = {"performance-guards"})
public final class StorageModule extends ManagedModule implements StorageServices {
    private MongoClient homesMongoClient;
    private HomeDirectory homeDirectory;
    private StorageApi storage;
    private SpawnService spawnService;
    private WarpService warpService;
    private HomeService homeService;
    private PlayerWarpService playerWarpService;
    private PlayerWarpDirectory playerWarpDirectory;
    private WarpDirectory warpDirectory;
    private SpawnDirectory spawnDirectory;
    private org.bukkit.configuration.file.FileConfiguration playerWarpsConfig;
    private fr.elias.oessentials.storage.internal.directory.PlayerDirectory playerDirectory;
    private volatile BalanceImportTarget balanceImportTarget;
    private fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker homeTpBroker;
    private fr.elias.oessentials.storage.api.Homes homes;
    private fr.elias.oessentials.storage.api.PlayerWarps playerWarps;
    private fr.elias.oessentials.api.IWarpsAPI warpsApi;
    private fr.elias.oessentials.api.ISpawnAPI spawnApi;
    private fr.elias.oessentials.api.IPlayerWarpsAPI playerWarpsApi;

    @Override
    protected void start() {
        cleanup("mongo", () -> { if (homesMongoClient != null) homesMongoClient.close(); });
        cleanup("storage-close", () -> { if (storage != null) storage.close(); });
        cleanup("storage-flush", () -> { if (storage != null) storage.flush(); });
        var messaging = services(MessagingServices.class);
        messaging.registerNamespace(new fr.elias.oessentials.storage.internal.homes.rabbit.packet.HomesPacketNamespace());
        messaging.registerNamespace(new fr.elias.oessentials.storage.internal.warps.rabbit.namespace.WarpsPacketNamespace());
        messaging.registerNamespace(new fr.elias.oessentials.storage.internal.spawn.rabbit.namespace.SpawnPacketNamespace());
        messaging.registerNamespace(new fr.elias.oessentials.storage.internal.playerwarp.rabbit.PlayerWarpPacketNamespace());
        initStorage();
        publishContracts();
        registerImportCommands();
        registerCommands();
        listenForModuleEvents(this);
        providePublicApi(fr.elias.oessentials.api.ISpawnAPI.class, this::getSpawnApi);
        providePublicApi(fr.elias.oessentials.api.IWarpsAPI.class, this::getWarpsApi);
        providePublicApi(fr.elias.oessentials.api.IPlayerWarpsAPI.class, this::getPlayerWarpsApi);
        registerReloadStep(13, "playerwarps", this::reloadPlayerwarps);
        registerConfigFile("playerwarps.yml", this::reloadPlayerWarpsConfig);
    }

    /** Cross-server homes and player warps, once RabbitMQ is connected. */
    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive()) return;
        final var cs = plugin.getCrossServerFeatures();
        if (cs.spawn() || cs.warps()) {
            new fr.elias.oessentials.storage.internal.rabbit.CrossServerTeleportBroker(plugin, spawnService, warpService, event.packets(),
                    services(ConfigurationServices.class).getServerConfig().serverName());
            plugin.getLogger().info("[BROKER] CrossServerTeleportBroker ready.");
        }
        if (cs.homes()) {
            this.homeTpBroker = new fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker(plugin, homeService, event.packets());
            plugin.getLogger().info("[BROKER] HomeTeleportBroker ready.");
        }
        var connector = services(ConfigurationServices.class).getServerConnector();
        if (playerWarpService != null && connector != null) {
            try {
                new fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpCrossServerBroker(plugin, playerWarpService, event.packets(), connector, services(ConfigurationServices.class).getServerConfig().serverName());
                plugin.getLogger().info("[BROKER] PlayerWarpCrossServerBroker enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[BROKER] Failed to init PlayerWarpCrossServerBroker: " + t.getMessage());
            }
        }
    }

    @Override public MongoClient getHomesMongoClient() { return homesMongoClient; }
    @Override public HomeDirectory getHomeDirectory() { return homeDirectory; }
    @Override public StorageApi getStorage() { return storage; }
    @Override public SpawnService getSpawnService() { return spawnService; }
    @Override public WarpService getWarpService() { return warpService; }
    @Override public HomeService getHomeService() { return homeService; }
    @Override public PlayerWarpService getPlayerWarpService() { return playerWarpService; }
    @Override public PlayerWarpDirectory getPlayerWarpDirectory() { return playerWarpDirectory; }
    @Override public WarpDirectory getWarpDirectory() { return warpDirectory; }
    @Override public SpawnDirectory getSpawnDirectory() { return spawnDirectory; }
    @Override public org.bukkit.configuration.file.FileConfiguration getPlayerWarpsConfig() { return playerWarpsConfig; }
    @Override public fr.elias.oessentials.storage.internal.directory.PlayerDirectory getPlayerDirectory() { return playerDirectory; }
    @Override public void setBalanceImportTarget(BalanceImportTarget target) { this.balanceImportTarget = target; }
    @Override public fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker() { return homeTpBroker; }
    @Override public fr.elias.oessentials.storage.internal.homes.TeleportBroker getTeleportBroker() { return null; }
    @Override public fr.elias.oessentials.storage.api.PlayerLocator getPlayerLocator() { return playerDirectory; }
    @Override public fr.elias.oessentials.storage.api.Homes getHomes() { return homes; }
    @Override public fr.elias.oessentials.storage.api.PlayerWarps getPlayerWarps() { return playerWarps; }
    @Override public fr.elias.oessentials.storage.api.BackDataStore getBackDataStore() { return storage; }
    @Override public fr.elias.oessentials.api.IWarpsAPI getWarpsApi() { return warpsApi; }
    @Override public fr.elias.oessentials.api.ISpawnAPI getSpawnApi() { return spawnApi; }
    @Override public fr.elias.oessentials.api.IPlayerWarpsAPI getPlayerWarpsApi() { return playerWarpsApi; }

    private void publishContracts() {
        var messaging = services(MessagingServices.class);
        this.homes = new fr.elias.oessentials.storage.internal.homes.HomesAdapter(homeService, messaging::getPacketBus);
        this.warpsApi = new fr.elias.oessentials.storage.internal.api.WarpsAPIImpl(warpService);
        this.spawnApi = new fr.elias.oessentials.storage.internal.api.SpawnAPIImpl(spawnService);
        if (playerWarpService != null) {
            this.playerWarps = new fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpsAdapter(playerWarpService);
            this.playerWarpsApi = new fr.elias.oessentials.storage.internal.api.PlayerWarpsAPIImpl(playerWarpService);
        }
    }

    /** Spawn, warp, home and player-warp commands, and the first-join spawn. */
    private void registerCommands() {
        var commands = services(ConfigurationServices.class).getCommands();
        final boolean homes = services(ConfigurationServices.class).getFeatureSettings().homesEnabled();
        commands
                .register(new fr.elias.oessentials.storage.internal.spawn.SpawnCommand(spawnService))
                .register(new fr.elias.oessentials.storage.internal.spawn.SetSpawnCommand(spawnService))
                .register(new fr.elias.oessentials.storage.internal.spawn.SetFirstSpawnCommand(spawnService))
                .register(new fr.elias.oessentials.storage.internal.warps.commands.WarpCommand(warpService))
                .register(new fr.elias.oessentials.storage.internal.warps.commands.SetWarpCommand(warpService))
                .register(new fr.elias.oessentials.storage.internal.warps.commands.WarpsCommand(warpService))
                .register(new fr.elias.oessentials.storage.internal.warps.commands.WarpsAdminCommand(warpService))
                .registerIf(homes, new fr.elias.oessentials.storage.internal.homes.home.HomeCommand(homeService))
                .register(new fr.elias.oessentials.storage.internal.warps.commands.DelWarpCommand(warpService))
                .registerIf(homes, new fr.elias.oessentials.storage.internal.homes.home.SetHomeCommand(homeService, services(ConfigurationServices.class).getServerConfig()))
                .registerIf(homes, new fr.elias.oessentials.storage.internal.homes.home.DelHomeCommand(homeService))
                .registerIf(homes, new fr.elias.oessentials.storage.internal.homes.HomesCommand(homeService))
                .registerIf(homes, new fr.elias.oessentials.storage.internal.homes.home.HomesGuiCommand(homeService));
        if (homes) {
            var otherHomesCmd = new fr.elias.oessentials.storage.internal.homes.OtherHomesListCommand(plugin, homeService);
            commands.registerLegacy("otherhomes", otherHomesCmd, otherHomesCmd);
            // otherhome is an OreoCommand; register() already wires its tab completer.
            commands.register(new fr.elias.oessentials.storage.internal.homes.OtherHomeCommand(plugin, homeService));
        }
        commands.rewireTab("home", new fr.elias.oessentials.storage.internal.homes.HomeTabCompleter(homeService));
        commands.rewireTab("warp", new fr.elias.oessentials.storage.internal.warps.WarpTabCompleter(warpService));

        // /pw keeps PlayerWarpCommand's own completer: the old PlayerWarpTabCompleter rewire ran
        // before /pw was registered and never took effect.
        if (playerWarpService != null && services(ConfigurationServices.class).getFeatureSettings().playerWarpsEnabled()) {
            commands.register(new fr.elias.oessentials.storage.internal.playerwarp.command.PlayerWarpCommand(playerWarpService));
            var pwwCmd = new fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpWhitelistCommand(playerWarpService);
            commands.registerLegacy("pwwhitelist", pwwCmd, pwwCmd);
            plugin.getLogger().info("[PlayerWarps] /pwwhitelist registered.");
        }

        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.storage.internal.spawn.FirstJoinSpawnListener(plugin), plugin);
    }

    /** CMI, EssentialsX and zEssentials data importers; balances go to whichever economy registered itself. */
    private void registerImportCommands() {
        services(ConfigurationServices.class).getCommands()
                .register(new ZEssentialsHomesImportCommand(plugin, storage, homeDirectory))
                .register(new ZImportCommand(plugin, homeDirectory, () -> balanceImportTarget))
                .register(new MigrateEssentialsXCommand(plugin, homeDirectory, () -> balanceImportTarget))
                .register(new CMIMigrateCommand(plugin, homeDirectory, () -> balanceImportTarget));
    }

    private void initStorage() {
        // Load playerwarps dedicated config
        java.io.File pwCfgFile = new java.io.File(plugin.getDataFolder(), "data/playerwarps/config.yml");
        if (!pwCfgFile.exists()) {
            pwCfgFile.getParentFile().mkdirs();
            plugin.saveResource("data/playerwarps/config.yml", false);
        }
        this.playerWarpsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(pwCfgFile);

        final String essentialsStorage = plugin.getConfig().getString("essentials.storage", "yaml").toLowerCase();
        var pwRoot = services(ConfigurationServices.class).getFeatureSettings().getRoot().getConfigurationSection("features.playerwarps");
        if (pwRoot == null) {
            pwRoot = services(ConfigurationServices.class).getFeatureSettings().getRoot().getConfigurationSection("playerwarps");
        }
        boolean pwEnabled = (pwRoot == null) || pwRoot.getBoolean("enabled", true);
        boolean pwCross   = (pwRoot == null) || pwRoot.getBoolean("cross-server", true);

        switch (essentialsStorage) {
            case "mongodb" -> {
                String uri    = plugin.getConfig().getString("storage.mongo.uri", "mongodb://localhost:27017");
                String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
                String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");

                this.homesMongoClient = com.mongodb.client.MongoClients.create(uri);

                this.playerDirectory = new fr.elias.oessentials.storage.internal.directory.PlayerDirectory(this.homesMongoClient, dbName, prefix);
                var dirListener = new fr.elias.oessentials.storage.internal.directory.DirectoryPresenceListener(this.playerDirectory, services(ConfigurationServices.class).getServerConfig().serverName());
                plugin.getServer().getPluginManager().registerEvents(dirListener, plugin);
                dirListener.backfillOnline();
                try {
                    new fr.elias.oessentials.storage.internal.directory.DirectoryHeartbeat(this.playerDirectory, services(ConfigurationServices.class).getServerConfig().serverName()).start();
                    plugin.getLogger().info("[PlayerDirectory] Heartbeat started.");
                } catch (Throwable t) {
                    plugin.getLogger().warning("[PlayerDirectory] Heartbeat failed to start: " + t.getMessage());
                }

                try {
                    MongoHomesMigrator.run(this.homesMongoClient, dbName, prefix, org.bukkit.Bukkit.getServer().getName(), services(ConfigurationServices.class).getServerConfig().serverName(), plugin.getLogger());
                } catch (Throwable ignored) {
                    plugin.getLogger().info("[STORAGE] MongoHomesMigrator skipped.");
                }

                this.storage       = new MongoHomesStorage(this.homesMongoClient, dbName, prefix, services(ConfigurationServices.class).getServerConfig().serverName());
                this.homeDirectory = new MongoHomeDirectory(this.homesMongoClient, dbName, prefix + "home_directory");

                try { this.warpDirectory  = new MongoWarpDirectory(this.homesMongoClient, dbName, prefix + "warp_directory"); }
                catch (Throwable ignored) { this.warpDirectory = null; }
                try { this.spawnDirectory = new MongoSpawnDirectory(this.homesMongoClient, dbName, prefix + "spawn_directory"); }
                catch (Throwable ignored) { this.spawnDirectory = null; }

                if (pwEnabled) {
                    PlayerWarpStorage pwStorage = new MongoPlayerWarpStorage(this.homesMongoClient, dbName, prefix + "playerwarps");
                    PlayerWarpDirectory pwDir = null;
                    if (pwCross) {
                        try {
                            pwDir = new MongoPlayerWarpDirectory(this.homesMongoClient, dbName, prefix + "playerwarp_directory");
                            plugin.getLogger().info("[PlayerWarps] MongoPlayerWarpDirectory initialized.");
                        } catch (Throwable t) {
                            plugin.getLogger().warning("[PlayerWarps] Failed to init MongoPlayerWarpDirectory: " + t.getMessage());
                        }
                    }
                    this.playerWarpDirectory = pwDir;
                    this.playerWarpService   = new PlayerWarpService(pwStorage, pwDir);
                    plugin.getLogger().info("[PlayerWarps] Enabled with MongoDB. cross-server=" + pwCross);
                } else {
                    this.playerWarpDirectory = null;
                    this.playerWarpService   = null;
                    plugin.getLogger().info("[PlayerWarps] Disabled by settings.yml.");
                }
                plugin.getLogger().info("[STORAGE] Using MongoDB.");
            }
            case "json" -> {
                this.storage = new JsonStorage(plugin);
                this.homeDirectory = null; this.warpDirectory = null; this.spawnDirectory = null;
                if (pwEnabled) {
                    this.playerWarpService   = new PlayerWarpService(new YamlPlayerWarpStorage(plugin), null);
                    this.playerWarpDirectory = null;
                    plugin.getLogger().info("[PlayerWarps] Enabled with local YAML storage.");
                } else {
                    this.playerWarpService = null; this.playerWarpDirectory = null;
                    plugin.getLogger().info("[PlayerWarps] Disabled.");
                }
                plugin.getLogger().info("[STORAGE] Using JSON.");
            }
            default -> {
                this.storage = new fr.elias.oessentials.storage.internal.services.YamlStorage(plugin);
                this.homeDirectory = null; this.warpDirectory = null; this.spawnDirectory = null;
                if (pwEnabled) {
                    this.playerWarpService   = new PlayerWarpService(new YamlPlayerWarpStorage(plugin), null);
                    this.playerWarpDirectory = null;
                    plugin.getLogger().info("[PlayerWarps] Enabled with local YAML storage.");
                } else {
                    this.playerWarpService = null; this.playerWarpDirectory = null;
                    plugin.getLogger().info("[PlayerWarps] Disabled.");
                }
                plugin.getLogger().info("[STORAGE] Using YAML.");
            }
        }

        this.spawnService = new SpawnService(storage, services(ConfigurationServices.class).getServerConfig().serverName());
        this.warpService  = new WarpService(storage, this.warpDirectory);
        this.homeService  = new HomeService(this.storage, services(ConfigurationServices.class).getServerConfig(), this.homeDirectory);
    }

    public void reloadPlayerWarpsConfig() {
        java.io.File f = new java.io.File(plugin.getDataFolder(), "data/playerwarps/config.yml");
        if (!f.exists()) plugin.saveResource("data/playerwarps/config.yml", false);
        this.playerWarpsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadPlayerwarps(org.bukkit.command.CommandSender sender) {
        try {
            reloadPlayerWarpsConfig();
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.playerwarps",
                    "<green>✔ Reloaded <white>playerwarps/config.yml</white></green>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.playerwarps-fail",
                    "<red>✘ Failed reloading playerwarps config: <gray>%error%</gray></red>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
        }
    }
}
