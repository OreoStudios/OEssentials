package fr.elias.oessentials.freeze.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.api.IFreezeAPI;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.freeze.internal.FreezeCommand;
import fr.elias.oessentials.freeze.internal.api.FreezeAPIImpl;
import fr.elias.oessentials.freeze.internal.freeze.FreezeManager;
import fr.elias.oessentials.freeze.internal.state.FreezeListener;
import fr.elias.oessentials.freeze.internal.state.FreezeService;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "freeze", dependencies = {"configuration::services"}, after = {"custom-crafting"})
public final class FreezeModule extends ManagedModule implements FreezeServices {
    private FreezeManager freezeManager;
    private FreezeService freezeService;
    private IFreezeAPI freezeApi;

    @Override
    protected void start() {
        initFreeze();
        providePublicApi(fr.elias.oessentials.api.IFreezeAPI.class, this::getFreezeApi);
    }

    @Override public FreezeManager getFreezeManager() { return freezeManager; }
    @Override public FreezeService getFreezeService() { return freezeService; }
    @Override public IFreezeAPI getFreezeApi() { return freezeApi; }

    private void initFreeze() {
        this.freezeManager = new FreezeManager(plugin);
        this.freezeService = new FreezeService();
        this.freezeApi = new FreezeAPIImpl(freezeService);
        plugin.getServer().getPluginManager().registerEvents(new FreezeListener(freezeService), plugin);
        services(ConfigurationServices.class).getCommands().register(new FreezeCommand(freezeService));
    }
}
