package fr.elias.oessentials.warnings.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "warnings", after = {"ignore"})
public final class WarningsModule extends ManagedModule implements WarningsServices {
    private fr.elias.oessentials.warnings.internal.WarnService warnService;

    @Override
    protected void start() {
        initWarnings();
    }

    @Override public fr.elias.oessentials.warnings.internal.WarnService getWarnService() { return warnService; }

    private void initWarnings() {
        try {
            this.warnService = new fr.elias.oessentials.warnings.internal.WarnService(plugin);
            plugin.getLogger().info("[Warnings] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Warnings] Failed to initialize: " + t.getMessage());
        }
    }
}
