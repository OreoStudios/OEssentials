package fr.elias.oessentials.bossbar.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface BossBarServices {
    fr.elias.oessentials.bossbar.internal.BossBarService getBossBarService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IBossBarAPI getBossBarApi() {
        var s = getBossBarService();
        return s == null ? null : new fr.elias.oessentials.bossbar.internal.api.BossBarAPIImpl(s);
    }
}
