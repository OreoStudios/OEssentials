package fr.elias.oessentials.skins.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.skins.internal.skin.SkinDebug;
import fr.elias.oessentials.skins.internal.skin.SkinRefresherBootstrap;

@PluginModule(value = "skins", after = {"command-control"}, optional = true)
public final class SkinsModule extends ManagedModule implements SkinsServices {

    @Override
    protected void start() {
        initSkins();
    }

    private void initSkins() {
        SkinRefresherBootstrap.init(plugin);
        SkinDebug.init(plugin);
    }
}
