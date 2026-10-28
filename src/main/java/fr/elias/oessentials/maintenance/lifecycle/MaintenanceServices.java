package fr.elias.oessentials.maintenance.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface MaintenanceServices {
    fr.elias.oessentials.maintenance.internal.MaintenanceConfig getMaintenanceConfig();
    fr.elias.oessentials.maintenance.internal.MaintenanceService getMaintenanceService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IMaintenanceAPI getMaintenanceApi() {
        var s = getMaintenanceService();
        return s == null ? null : new fr.elias.oessentials.maintenance.internal.api.MaintenanceAPIImpl(s);
    }
}
