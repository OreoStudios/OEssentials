package fr.elias.oessentials.enderchest.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface EnderChestServices {
    fr.elias.oessentials.enderchest.internal.EnderChestConfig getEcConfig();
    fr.elias.oessentials.enderchest.internal.EnderChestService getEcService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IEnderChestAPI getEnderChestApi() {
        var s = getEcService();
        return s == null ? null : new fr.elias.oessentials.enderchest.internal.api.EnderChestAPIImpl(s);
    }

    /** Rows of the player's ender chest even while they are offline (0 when unavailable). */
    default int resolveSlotsOffline(java.util.UUID playerId) {
        var service = getEcService();
        return service == null ? 0 : service.resolveSlotsOffline(playerId);
    }

    default org.bukkit.inventory.ItemStack[] loadFor(java.util.UUID playerId, int rows) {
        var service = getEcService();
        return service == null ? null : service.loadFor(playerId, rows);
    }

    default void saveFor(java.util.UUID playerId, int rows, org.bukkit.inventory.ItemStack[] contents) {
        var service = getEcService();
        if (service != null) service.saveFor(playerId, rows, contents);
    }
}
