package fr.elias.oessentials.commandcontrol.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface CommandControlServices {
    fr.elias.oessentials.commandcontrol.internal.CommandControlService getCommandControlService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ICommandControlAPI getCommandControlApi() {
        var s = getCommandControlService();
        return s == null ? null : new fr.elias.oessentials.commandcontrol.internal.api.CommandControlAPIImpl(s);
    }
}
