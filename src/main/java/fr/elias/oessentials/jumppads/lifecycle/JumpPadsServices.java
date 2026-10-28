package fr.elias.oessentials.jumppads.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface JumpPadsServices {
    fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsManager getJumpPads();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IJumpPadsAPI getJumpPadsApi() {
        var s = getJumpPads();
        return s == null ? null : new fr.elias.oessentials.jumppads.internal.api.JumpPadsAPIImpl(s);
    }
}
