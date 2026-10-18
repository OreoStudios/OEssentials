package fr.elias.oessentials.help.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "help", dependencies = {"configuration::services"}, after = {"maintenance"})
public final class HelpModule extends ManagedModule implements HelpServices {

    @Override
    protected void start() {
        initHelp();
    }

    private void initHelp() {
        try {
            var helpConfig = new fr.elias.oessentials.help.internal.HelpConfig(plugin);
            var helpCmd    = new fr.elias.oessentials.help.internal.HelpCommand(helpConfig);
            services(ConfigurationServices.class).getCommands().register(helpCmd);
            plugin.getLogger().info("[Help] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Help] Failed to initialize: " + t.getMessage());
        }
    }
}
