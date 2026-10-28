package fr.elias.oessentials.ignore.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "ignore", dependencies = {"configuration::services"}, after = {"help"})
public final class IgnoreModule extends ManagedModule implements IgnoreServices {
    private fr.elias.oessentials.ignore.internal.IgnoreService ignoreService;

    @Override
    protected void start() {
        initIgnore();
        registerCommands();
    }

    @Override public fr.elias.oessentials.ignore.internal.IgnoreService getIgnoreService() { return ignoreService; }

    private void initIgnore() {
        try {
            this.ignoreService = new fr.elias.oessentials.ignore.internal.IgnoreService(plugin);
            plugin.getLogger().info("[Ignore] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Ignore] Failed to initialize: " + t.getMessage());
        }
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.ignore.internal.IgnoreCommand(ignoreService));
    }
}
