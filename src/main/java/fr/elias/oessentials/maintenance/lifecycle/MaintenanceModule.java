package fr.elias.oessentials.maintenance.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import java.io.File;

@PluginModule(value = "maintenance", dependencies = {"configuration::services"}, after = {"afk"})
public final class MaintenanceModule extends ManagedModule implements MaintenanceServices {
    private fr.elias.oessentials.maintenance.internal.MaintenanceConfig maintenanceConfig;
    private fr.elias.oessentials.maintenance.internal.MaintenanceService maintenanceService;

    @Override
    protected void start() {
        cleanup("maintenanceService", () -> { if (maintenanceService != null) maintenanceService.shutdown(); });
        initMaintenance();
        providePublicApi(fr.elias.oessentials.api.IMaintenanceAPI.class, this::getMaintenanceApi);
    }

    @Override public fr.elias.oessentials.maintenance.internal.MaintenanceConfig getMaintenanceConfig() { return maintenanceConfig; }
    @Override public fr.elias.oessentials.maintenance.internal.MaintenanceService getMaintenanceService() { return maintenanceService; }

    private void initMaintenance() {
        if (!services(ConfigurationServices.class).getFeatureSettings().maintenanceEnabled()) {
            BootstrapSupport.unregisterCommandHard(plugin, "maintenance");
            plugin.getLogger().info("[Maintenance] Disabled by settings.yml");
            return;
        }
        try {
            File maintenanceFile = new File(plugin.getDataFolder(), "config/server/maintenance.yml");
            if (!maintenanceFile.exists()) { maintenanceFile.getParentFile().mkdirs(); plugin.saveResource("config/server/maintenance.yml", false); }

            this.maintenanceConfig  = new fr.elias.oessentials.maintenance.internal.MaintenanceConfig(plugin);
            this.maintenanceService = new fr.elias.oessentials.maintenance.internal.MaintenanceService(plugin, maintenanceConfig);
            plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.maintenance.internal.MaintenanceModuleListener(plugin, maintenanceService), plugin);

            var maintenanceCmd = new fr.elias.oessentials.maintenance.internal.MaintenanceCommand(plugin, maintenanceService);
            services(ConfigurationServices.class).getCommands().registerLegacy("maintenance", maintenanceCmd, maintenanceCmd);

            if (maintenanceConfig.isTimerExpired() && maintenanceConfig.isEnabled()) {
                maintenanceService.disable();
                plugin.getLogger().info("[Maintenance] Auto-disabled (timer expired while offline)");
            }

            if (maintenanceService.isEnabled()) {
                plugin.getLogger().warning("+=============================================================+");
                plugin.getLogger().warning("|                    !  MAINTENANCE ACTIVE  !                |");
                if (maintenanceConfig.isUseTimer() && maintenanceConfig.getRemainingTime() > 0) {
                plugin.getLogger().warning(String.format("|  Time remaining: %-41s |", maintenanceService.getFormattedTimeRemaining()));
                }
                plugin.getLogger().warning("+=============================================================+");
            } else {
                plugin.getLogger().info("[Maintenance] System initialized (currently disabled)");
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("[Maintenance] Failed to initialize: " + t.getMessage());
            t.printStackTrace();
            this.maintenanceService = null;
            this.maintenanceConfig  = null;
        }
    }
}
