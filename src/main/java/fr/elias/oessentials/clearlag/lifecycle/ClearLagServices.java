package fr.elias.oessentials.clearlag.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.clearlag.internal.ClearLagManager;

@ModuleApi("services")
public interface ClearLagServices {
    ClearLagManager getClearLag();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IClearLagAPI getClearLagApi() {
        var s = getClearLag();
        return s == null ? null : new fr.elias.oessentials.clearlag.internal.api.ClearLagAPIImpl(s);
    }
}
