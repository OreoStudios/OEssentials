package fr.elias.oessentials.skins.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.skins.internal.skin.SkinDebug;
import fr.elias.oessentials.skins.internal.skin.SkinRefresherBootstrap;

@PluginModule(value = "skins", dependencies = {"configuration::services"}, after = {"command-control"}, optional = true)
public final class SkinsModule extends ManagedModule implements SkinsServices {

    @Override
    protected void start() {
        initSkins();
        registerCommands();
    }

    private void initSkins() {
        SkinRefresherBootstrap.init(plugin);
        SkinDebug.init(plugin);
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.skins.internal.skin.SkinCommand());
        commands.rewireTab("skin", new fr.elias.oessentials.skins.internal.skin.SkinCommand());
        commands.register(new fr.elias.oessentials.skins.internal.skin.CloneCommand());
        commands.rewireTab("clone", new fr.elias.oessentials.skins.internal.skin.CloneCommand());
    }
}
