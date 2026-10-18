package fr.elias.oessentials.commandtoggle.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleConfig;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleListener;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleService;

@PluginModule(value = "command-toggle", dependencies = {"configuration::services"}, after = {"skins"})
public final class CommandToggleModule extends ManagedModule implements CommandToggleServices {
    private CommandToggleConfig commandToggleConfig;
    private CommandToggleService commandToggleService;

    @Override
    protected void start() {
        initCommandToggle();
    }

    @Override public CommandToggleConfig getCommandToggleConfig() { return commandToggleConfig; }
    @Override public CommandToggleService getCommandToggleService() { return commandToggleService; }

    private void initCommandToggle() {
        try {
            this.commandToggleConfig  = new CommandToggleConfig(plugin);
            var commands = services(ConfigurationServices.class).getCommands();
            this.commandToggleService = new CommandToggleService(plugin, commandToggleConfig, commands);
            // Modules starting after this point never register a disabled command.
            final CommandToggleConfig toggles = commandToggleConfig;
            commands.setRegistrationGate(toggles::isCommandEnabled);
            cleanup("registrationGate", () -> commands.setRegistrationGate(null));
            plugin.getServer().getPluginManager().registerEvents(new CommandToggleListener(plugin, commandToggleConfig), plugin);
            plugin.getLogger().info("[CommandToggle] Command toggle system initialized");
        } catch (Exception e) {
            plugin.getLogger().severe("[CommandToggle] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
