package fr.elias.oessentials.sync.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface InventorySyncServices {
    fr.elias.oessentials.sync.internal.invsee.InvseeService getInvseeService();
    fr.elias.oessentials.sync.internal.invsee.rabbit.InvseeCrossServerBroker getInvseeBroker();
    fr.elias.oessentials.sync.internal.invlook.manager.InvlookManager getInvlookManager();

    /**
     * Saves {@code player}'s synced state now (synchronously), so another server, or this one after
     * a crash, restores exactly this inventory. Does nothing while inventory sync is off.
     */
    void saveInventoryNow(org.bukkit.entity.Player player);

    /** Snapshots for the inventory viewer, or {@code null} while invsee is not running. */
    default fr.elias.oessentials.sync.api.InvseeSnapshots getInvseeSnapshots() {
        return getInvseeService();
    }
}
