package fr.elias.oessentials.motd.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "motd", dependencies = {"configuration::services"}, after = {"punishment-logger"})
public final class MotdModule extends ManagedModule implements MotdServices {

    @Override
    protected void start() {
        initMotd();
    }

    private void initMotd() {
        try {
            var motdConfig  = new fr.elias.oessentials.motd.internal.MotdConfig(plugin);
            var motdService = new fr.elias.oessentials.motd.internal.MotdService(plugin, motdConfig);
            plugin.getServer().getPluginManager().registerEvents(motdService, plugin);
            services(ConfigurationServices.class).getCommands().register(motdService);
            plugin.getLogger().info("[MOTD] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[MOTD] Failed to initialize: " + t.getMessage());
        }
    }
}
