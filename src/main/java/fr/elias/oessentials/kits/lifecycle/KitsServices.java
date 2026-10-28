package fr.elias.oessentials.kits.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface KitsServices {
    fr.elias.oessentials.kits.internal.KitsManager getKitsManager();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IKitsAPI getKitsApi() {
        var s = getKitsManager();
        return s == null ? null : new fr.elias.oessentials.kits.internal.api.KitsAPIImpl(s);
    }
}
