package fr.elias.oessentials.worlds.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "custom-worlds", dependencies = {"configuration::services"}, after = {"currency-system"})
public final class CustomWorldsModule extends ManagedModule implements CustomWorldsServices {

    @Override
    protected void start() {
        initCustomWorlds();
        registerCommands();
    }

    private void initCustomWorlds() { fr.elias.oessentials.worlds.internal.commands.core.admins.OeWorldCommand.loadCustomWorlds(plugin); }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.worlds.internal.commands.core.admins.OeWorldCommand(plugin));
        commands.registerLegacy("world", new fr.elias.oessentials.worlds.internal.commands.core.admins.WorldTeleportCommand());
    }
}
