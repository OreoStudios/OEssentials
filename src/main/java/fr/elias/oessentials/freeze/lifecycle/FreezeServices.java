package fr.elias.oessentials.freeze.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.api.IFreezeAPI;
import fr.elias.oessentials.freeze.internal.freeze.FreezeManager;
import fr.elias.oessentials.freeze.internal.state.FreezeService;

@ModuleApi("services")
public interface FreezeServices {
    FreezeManager getFreezeManager();

    /** Released facade type; other modules use {@link #getFreezeApi()}. */
    FreezeService getFreezeService();

    /** Who is frozen, as exposed by the public OEssentials API. */
    IFreezeAPI getFreezeApi();
}
