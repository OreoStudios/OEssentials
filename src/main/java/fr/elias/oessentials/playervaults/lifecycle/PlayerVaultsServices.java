package fr.elias.oessentials.playervaults.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface PlayerVaultsServices {
    fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig getPlayerVaultsConfig();
    fr.elias.oessentials.playervaults.internal.PlayerVaultsService getPlayervaultsService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IPlayerVaultsAPI getPlayerVaultsApi() {
        var s = getPlayervaultsService();
        return s == null ? null : new fr.elias.oessentials.playervaults.internal.api.PlayerVaultsAPIImpl(s);
    }
}
