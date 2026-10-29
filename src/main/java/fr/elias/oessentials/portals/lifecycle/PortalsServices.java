package fr.elias.oessentials.portals.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface PortalsServices {
    fr.elias.oessentials.portals.internal.PortalsManager getPortals();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IPortalsAPI getPortalsApi() {
        var s = getPortals();
        return s == null ? null : new fr.elias.oessentials.portals.internal.api.PortalsAPIImpl(s);
    }
}
