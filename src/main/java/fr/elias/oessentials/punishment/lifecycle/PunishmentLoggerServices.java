package fr.elias.oessentials.punishment.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface PunishmentLoggerServices {
    fr.elias.oessentials.punishment.internal.PunishmentLogger getPunishmentLogger();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IPunishmentAPI getPunishmentApi() {
        var s = getPunishmentLogger();
        return s == null ? null : new fr.elias.oessentials.punishment.internal.api.PunishmentAPIImpl(s);
    }
}
