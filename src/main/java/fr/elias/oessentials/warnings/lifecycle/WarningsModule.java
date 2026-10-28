package fr.elias.oessentials.warnings.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "warnings", dependencies = {"configuration::services"}, after = {"ignore"})
public final class WarningsModule extends ManagedModule implements WarningsServices {
    private fr.elias.oessentials.warnings.internal.WarnService warnService;

    @Override
    protected void start() {
        initWarnings();
        registerCommands();
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

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.warnings.internal.WarnCommand(warnService))
                .register(new fr.elias.oessentials.warnings.internal.WarningsCommand(warnService))
                .register(new fr.elias.oessentials.warnings.internal.UnwarnCommand(warnService));
    }
}
