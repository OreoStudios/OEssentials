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

    /** Writes maintenance.yml keys and reloads; {@code false} when maintenance is not running. */
    default boolean applySettingsPatch(java.util.Map<String, Object> values) throws java.io.IOException {
        var service = getMaintenanceService();
        if (service == null) return false;
        service.getConfig().applyPatch(values);
        service.reload();
        return true;
    }
}
