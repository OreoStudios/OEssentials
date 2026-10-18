package fr.elias.oessentials.kits.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "kits", dependencies = {"configuration::services"}, after = {"command-toggle"})
public final class KitsModule extends ManagedModule implements KitsServices {
    private fr.elias.oessentials.kits.internal.KitsManager kitsManager;

    @Override
    protected void start() {
        cleanup("kitsManager", () -> { if (kitsManager != null) kitsManager.saveData(); });
        initKits();
    }

    @Override public fr.elias.oessentials.kits.internal.KitsManager getKitsManager() { return kitsManager; }

    private void initKits() {
        boolean kitsFeature  = services(ConfigurationServices.class).getSettingsConfig().kitsEnabled();
        boolean kitsRegister = services(ConfigurationServices.class).getSettingsConfig().kitsCommandsEnabled();

        if (kitsFeature) {
            this.kitsManager = new fr.elias.oessentials.kits.internal.KitsManager(plugin);
            if (kitsRegister) {
                new fr.elias.oessentials.kits.internal.KitCommands(plugin, this.kitsManager);
                plugin.getLogger().info("[Kits] Loaded " + this.kitsManager.getKits().size() + " kits.");
            } else {
                BootstrapSupport.unregisterCommandHard(plugin, "kits");
                BootstrapSupport.unregisterCommandHard(plugin, "kit");
                plugin.getLogger().info("[Kits] Module loaded, commands NOT registered.");
            }
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "kits");
            BootstrapSupport.unregisterCommandHard(plugin, "kit");
            this.kitsManager = null;
            plugin.getLogger().info("[Kits] Disabled by config.");
        }
    }
}
