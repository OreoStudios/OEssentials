package fr.elias.oessentials.tab.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "tab", dependencies = {"configuration::services", "storage::services"}, after = {"scoreboard"}, optional = true)
public final class TabModule extends ManagedModule implements TabServices {
    private fr.elias.oessentials.tab.internal.TabListManager tabListManager;

    @Override
    protected void start() {
        cleanup("tabListManager", () -> { if (tabListManager != null) tabListManager.stop(); });
        initTab();
        registerReloadStep(4, "tab", this::reloadTab);
        registerConfigFile("tab.yml", () -> plugin.getTabListManager().reload());
    }

    @Override public fr.elias.oessentials.tab.internal.TabListManager getTabListManager() { return tabListManager; }

    private void initTab() {
        if (services(ConfigurationServices.class).getFeatureSettings().tabEnabled() && BootstrapSupport.uiModuleAllowed(plugin, "tab")) {
            this.tabListManager = new fr.elias.oessentials.tab.internal.TabListManager(plugin);
            this.tabListManager.start();
            plugin.getLogger().info("[TAB] Custom tab-list enabled.");
        } else {
            this.tabListManager = null;
            plugin.getLogger().info("[TAB] Disabled by settings.yml.");
        }
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadTab(org.bukkit.command.CommandSender sender) {
        try {
            var target = plugin.getTabListManager();
            if (target != null) {
                target.reload();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.tab",
                        "<green>✔ Reloaded <white>tab.yml</white></green>");
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.tab-skip",
                    "<yellow>• Skipped tab.yml (manager unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.tab-skip",
                    "<yellow>• Skipped tab.yml: <gray>%error%</gray></yellow>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        }
    }
}
