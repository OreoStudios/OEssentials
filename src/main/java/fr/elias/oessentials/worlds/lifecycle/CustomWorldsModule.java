package fr.elias.oessentials.worlds.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "custom-worlds", after = {"currency-system"})
public final class CustomWorldsModule extends ManagedModule implements CustomWorldsServices {

    @Override
    protected void start() {
        initCustomWorlds();
    }

    private void initCustomWorlds() { fr.elias.oessentials.worlds.internal.commands.core.admins.OeWorldCommand.loadCustomWorlds(plugin); }
}
