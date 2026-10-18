package fr.elias.oessentials.rules.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "rules", dependencies = {"configuration::services"}, after = {"motd"})
public final class RulesModule extends ManagedModule implements RulesServices {

    @Override
    protected void start() {
        initRules();
    }

    private void initRules() {
        try {
            var rulesCmd = new fr.elias.oessentials.rules.internal.RulesCommand(plugin);
            services(ConfigurationServices.class).getCommands().register(rulesCmd);
            plugin.getLogger().info("[Rules] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Rules] Failed to initialize: " + t.getMessage());
        }
    }
}
