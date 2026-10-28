package fr.elias.oessentials.commandtoggle.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleConfig;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleService;

@ModuleApi("services")
public interface CommandToggleServices {
    CommandToggleConfig getCommandToggleConfig();
    CommandToggleService getCommandToggleService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ICommandToggleAPI getCommandToggleApi() {
        var s = getCommandToggleService();
        return s == null ? null : new fr.elias.oessentials.commandtoggle.internal.api.CommandToggleAPIImpl(s);
    }
}
