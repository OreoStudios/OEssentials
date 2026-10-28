package fr.elias.oessentials.afk.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.afk.internal.AfkPoolService;
import fr.elias.oessentials.afk.internal.AfkService;

@ModuleApi("services")
public interface AfkServices {
    AfkService getAfkService();
    AfkPoolService getAfkPoolService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IAfkAPI getAfkApi() {
        var s = getAfkService();
        return s == null ? null : new fr.elias.oessentials.afk.internal.api.AfkAPIImpl(s);
    }
}
