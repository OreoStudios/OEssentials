package fr.elias.oessentials.commands.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface CommandsServices {
    fr.elias.oessentials.commands.internal.ic.ICManager getIcManager();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IInteractiveCommandsAPI getInteractiveCommandsApi() {
        var s = getIcManager();
        return s == null ? null : new fr.elias.oessentials.commands.internal.api.InteractiveCommandsAPIImpl(s);
    }
}
