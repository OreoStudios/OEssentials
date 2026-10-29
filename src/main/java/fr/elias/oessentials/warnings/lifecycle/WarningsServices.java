package fr.elias.oessentials.warnings.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface WarningsServices {
    fr.elias.oessentials.warnings.internal.WarnService getWarnService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IWarningsAPI getWarningsApi() {
        var s = getWarnService();
        return s == null ? null : new fr.elias.oessentials.warnings.internal.api.WarningsAPIImpl(s);
    }
}
