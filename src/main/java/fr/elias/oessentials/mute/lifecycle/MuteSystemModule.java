package fr.elias.oessentials.mute.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.chat.internal.MuteListener;
import fr.elias.oessentials.chat.internal.chatservices.MuteService;

@PluginModule(value = "mute-system", after = {"daily"})
public final class MuteSystemModule extends ManagedModule implements MuteSystemServices {
    private MuteService muteService;

    @Override
    protected void start() {
        initMuteSystem();
    }

    @Override public MuteService getMuteService() { return muteService; }

    private void initMuteSystem() {
        this.muteService = new MuteService(plugin);
        plugin.getServer().getPluginManager().registerEvents(new MuteListener(muteService), plugin);
    }
}
