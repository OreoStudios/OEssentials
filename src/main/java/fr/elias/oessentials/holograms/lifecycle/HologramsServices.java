package fr.elias.oessentials.holograms.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface HologramsServices {
    fr.elias.oessentials.holograms.internal.OHolograms getEmbeddedOHolograms();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IHologramsAPI getHologramsApi() {
        var mgr = fr.elias.oessentials.holograms.internal.OHolograms.canGet()
                ? fr.elias.oessentials.holograms.internal.OHolograms.get() : null;
        if (mgr == null) return null;
        var holMgr = mgr.getHologramManager();
        return holMgr == null ? null : new fr.elias.oessentials.holograms.internal.api.HologramsAPIImpl(holMgr);
    }
}
