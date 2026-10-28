package fr.elias.oessentials.jails.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface JailsServices {
    fr.elias.oessentials.jails.internal.jail.JailService getJailService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IJailAPI getJailApi() {
        var s = getJailService();
        return s == null ? null : new fr.elias.oessentials.jails.internal.api.JailAPIImpl(s);
    }
}
