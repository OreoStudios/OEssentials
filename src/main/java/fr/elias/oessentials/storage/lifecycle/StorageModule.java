package fr.elias.oessentials.storage.lifecycle;

import com.mongodb.client.MongoClient;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
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

@PluginModule(value = "storage", dependencies = {"configuration::services"}, after = {"performance-guards"})
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

    @Override
    protected void start() {
        cleanup("mongo", () -> { if (homesMongoClient != null) homesMongoClient.close(); });
        cleanup("storage-close", () -> { if (storage != null) storage.close(); });
        cleanup("storage-flush", () -> { if (storage != null) storage.flush(); });
        initStorage();
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
}
