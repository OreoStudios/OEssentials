package fr.elias.oessentials.tab.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "tab", dependencies = {"configuration::services"}, after = {"scoreboard"}, optional = true)
public final class TabModule extends ManagedModule implements TabServices {
    private fr.elias.oessentials.tab.internal.TabListManager tabListManager;

    @Override
    protected void start() {
        cleanup("tabListManager", () -> { if (tabListManager != null) tabListManager.stop(); });
        initTab();
    }

    @Override public fr.elias.oessentials.tab.internal.TabListManager getTabListManager() { return tabListManager; }

    private void initTab() {
        if (services(ConfigurationServices.class).getSettingsConfig().tabEnabled() && BootstrapSupport.uiModuleAllowed(plugin, "tab")) {
            this.tabListManager = new fr.elias.oessentials.tab.internal.TabListManager(plugin);
            this.tabListManager.start();
            plugin.getLogger().info("[TAB] Custom tab-list enabled.");
        } else {
            this.tabListManager = null;
            plugin.getLogger().info("[TAB] Disabled by settings.yml.");
        }
    }
}
