package fr.elias.oessentials.player.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.player.internal.listeners.GodListener;
import fr.elias.oessentials.player.internal.back.service.BackService;
import fr.elias.oessentials.messaging.internal.cross.PlayerTrackingListener;
import fr.elias.oessentials.player.internal.deathback.DeathBackListener;
import fr.elias.oessentials.player.internal.deathback.DeathBackService;
import fr.elias.oessentials.storage.internal.homes.TeleportBroker;
import fr.elias.oessentials.player.internal.tp.service.TeleportService;
import fr.elias.oessentials.player.internal.services.GodService;
import fr.elias.oessentials.player.internal.services.MessageService;
import fr.elias.oessentials.player.internal.services.VanishService;

@PluginModule(value = "player-services", dependencies = {"configuration::services", "storage::services"}, after = {"inventory-sync"})
public final class PlayerServicesModule extends ManagedModule implements PlayerServices {
    private TeleportService teleportService;
    private BackService backService;
    private MessageService messageService;
    private DeathBackService deathBackService;
    private GodService godService;
    // Temporary holder so initCommands can access vanishService
    private transient VanishService _vanishService;
    private TeleportBroker teleportBroker;

    @Override
    protected void start() {
        cleanup("teleportService", () -> { if (teleportService != null) teleportService.shutdown(); });
        initCoreServices();
    }

    @Override public TeleportService getTeleportService() { return teleportService; }
    @Override public BackService getBackService() { return backService; }
    @Override public MessageService getMessageService() { return messageService; }
    @Override public DeathBackService getDeathBackService() { return deathBackService; }
    @Override public GodService getGodService() { return godService; }
    @Override public VanishService getVanishService() { return _vanishService; }
    @Override public TeleportBroker getTeleportBroker() { return teleportBroker; }

    private void initCoreServices() {
        this.backService      = new BackService(services(StorageServices.class).getStorage());
        this.messageService   = new MessageService();
        this.teleportService  = new TeleportService(plugin, backService, services(ConfigurationServices.class).getServerConfig());
        this.deathBackService = new DeathBackService();
        this.godService       = new GodService();

        boolean vanishCrossServer = services(ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "vanish", true);
        fr.elias.oessentials.player.internal.services.vanish.VanishStateStorage vanishStorage =
                new fr.elias.oessentials.player.internal.services.vanish.YamlVanishStateStorage(plugin);

        if (vanishCrossServer && services(StorageServices.class).getHomesMongoClient() != null) {
            String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
            String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");
            vanishStorage = new fr.elias.oessentials.player.internal.services.vanish.MongoVanishStateStorage(services(StorageServices.class).getHomesMongoClient(), dbName, prefix);
            plugin.getLogger().info("[VANISH] Using MongoDB storage for persistent cross-server state.");
        } else {
            plugin.getLogger().info("[VANISH] Using local YAML storage for persistent state.");
        }

        VanishService vanishService = new VanishService(plugin, vanishStorage, vanishCrossServer, services(ConfigurationServices.class).getServerConfig().serverName());
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.player.internal.listeners.VanishListener(vanishService, plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerTrackingListener(backService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DeathBackListener(deathBackService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GodListener(godService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.rtp.internal.listeners.DeathRespawnListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.storage.internal.spawn.FirstJoinSpawnListener(plugin), plugin);
        vanishService.restoreOnlinePlayers();

        // store vanishService ref for commands block
        this._vanishService = vanishService;
    }
}
