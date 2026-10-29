package fr.elias.oessentials.sync.api;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Persisted player inventories; registered with Bukkit's ServicesManager by inventory-sync. */
@dev.oreo.modulith.core.ModuleApi("services")
public interface InventoryService {

    @dev.oreo.modulith.core.ModuleApi("services")
    final class Snapshot {
        public ItemStack[] contents;
        public ItemStack[] armor;
        public ItemStack   offhand;

        public int level;

        public float exp;

        public int totalExp;
    }

    Snapshot load(UUID uuid);

    void save(UUID uuid, Snapshot snapshot);


    static void clearPersistentInventory(InventoryService invService, UUID uuid) {
        Snapshot snap = new Snapshot();
        snap.contents = new ItemStack[41];
        snap.armor    = new ItemStack[4];
        snap.offhand  = null;

        snap.level    = 0;
        snap.exp      = 0.0f;
        snap.totalExp = 0;

        invService.save(uuid, snap);
    }
}
