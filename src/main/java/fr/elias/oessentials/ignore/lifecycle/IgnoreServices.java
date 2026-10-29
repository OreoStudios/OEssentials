package fr.elias.oessentials.ignore.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface IgnoreServices {
    fr.elias.oessentials.ignore.internal.IgnoreService getIgnoreService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IIgnoreAPI getIgnoreApi() {
        var s = getIgnoreService();
        return s == null ? null : new fr.elias.oessentials.ignore.internal.api.IgnoreAPIImpl(s);
    }
}
