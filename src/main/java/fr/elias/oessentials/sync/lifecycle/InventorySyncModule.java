package fr.elias.oessentials.sync.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import org.bukkit.Bukkit;

@PluginModule(value = "inventory-sync", dependencies = {"configuration::services", "storage::services"}, after = {"ender-chest"})
public final class InventorySyncModule extends ManagedModule implements InventorySyncServices {

    @Override
    protected void start() {
        cleanup("online-inventories", this::saveOnlineInventories);
        initInventorySync();
    }

    private void initInventorySync() {
        final boolean invSyncEnabled = services(ConfigurationServices.class).getSettingsConfig().featureOption("cross-server", "inventory", true);

        fr.elias.oessentials.sync.internal.playersync.PlayerSyncStorage invStorage;
        if (invSyncEnabled && "mongodb".equalsIgnoreCase(plugin.getConfig().getString("essentials.storage", "yaml")) && services(StorageServices.class).getHomesMongoClient() != null) {
            String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
            String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");
            invStorage = new fr.elias.oessentials.sync.internal.playersync.MongoPlayerSyncStorage(services(StorageServices.class).getHomesMongoClient(), dbName, prefix);
            plugin.getLogger().info("[SYNC] Using MongoDB storage.");
        } else {
            invStorage = new fr.elias.oessentials.sync.internal.playersync.YamlPlayerSyncStorage(plugin);
            plugin.getLogger().info("[SYNC] Using local YAML storage.");
        }

        final boolean syncSaveOnQuit = plugin.getConfig().getBoolean("playersync.save-on-quit", true);
        if (!syncSaveOnQuit) plugin.getLogger().info("[SYNC] save-on-quit=false — this server will load sync but not save on player quit (lobby/hub mode).");

        final var syncPrefsStore    = new fr.elias.oessentials.sync.internal.playersync.PlayerSyncPrefsStore(plugin);
        final var playerSyncService = new fr.elias.oessentials.sync.internal.playersync.PlayerSyncService(plugin, invStorage, syncPrefsStore);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.sync.internal.playersync.PlayerSyncListener(playerSyncService, invSyncEnabled, syncSaveOnQuit), plugin);

        final fr.elias.oessentials.sync.internal.playersync.PlayerSyncStorage finalInvStorage = invStorage;
        fr.elias.oessentials.sync.internal.services.InventoryService invSvc = new fr.elias.oessentials.sync.internal.services.InventoryService() {
            @Override public Snapshot load(java.util.UUID uuid) {
                try {
                    var s = finalInvStorage.load(uuid);
                    if (s == null) return null;
                    Snapshot snap = new Snapshot();
                    snap.contents = s.inventory; snap.armor = s.armor; snap.offhand = s.offhand;
                    return snap;
                } catch (Exception e) { plugin.getLogger().warning("[INVSEE] load failed: " + e.getMessage()); return null; }
            }
            @Override public void save(java.util.UUID uuid, Snapshot snapshot) {
                try {
                    var s = new fr.elias.oessentials.sync.internal.playersync.PlayerSyncSnapshot();
                    s.inventory = snapshot.contents; s.armor = snapshot.armor; s.offhand = snapshot.offhand;
                    finalInvStorage.save(uuid, s);
                } catch (Exception e) { plugin.getLogger().warning("[INVSEE] save failed: " + e.getMessage()); }
            }
        };
        Bukkit.getServicesManager().register(fr.elias.oessentials.sync.internal.services.InventoryService.class, invSvc, plugin, org.bukkit.plugin.ServicePriority.Normal);

        services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.sync.internal.playersync.PlayerSyncCommand(plugin, playerSyncService, invSyncEnabled));
    }

    private void saveOnlineInventories() {
        try {
            fr.elias.oessentials.sync.internal.services.InventoryService invSvc =
                    org.bukkit.Bukkit.getServicesManager().load(fr.elias.oessentials.sync.internal.services.InventoryService.class);
            if (invSvc != null) {
                plugin.getLogger().info("[SHUTDOWN] Saving inventories of " + org.bukkit.Bukkit.getOnlinePlayers().size() + " online players...");
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    try {
                        fr.elias.oessentials.sync.internal.services.InventoryService.Snapshot snap = new fr.elias.oessentials.sync.internal.services.InventoryService.Snapshot();
                        snap.contents = p.getInventory().getContents();
                        snap.armor    = p.getInventory().getArmorContents();
                        snap.offhand  = p.getInventory().getItemInOffHand();
                        invSvc.save(p.getUniqueId(), snap);
                        plugin.getLogger().info("[SHUTDOWN] Saved inventory for " + p.getName());
                    } catch (Exception ex) {
                        plugin.getLogger().warning("[SHUTDOWN] Failed to save inventory for " + p.getName() + ": " + ex.getMessage());
                    }
                }
            } else {
                plugin.getLogger().info("[SHUTDOWN] No InventoryService registered; skipping inventory save.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[SHUTDOWN] Error while saving inventories on shutdown: " + t.getMessage());
        }

    }
}
