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
}
