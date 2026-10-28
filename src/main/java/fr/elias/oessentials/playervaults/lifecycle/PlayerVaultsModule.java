package fr.elias.oessentials.playervaults.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "player-vaults", dependencies = {"configuration::services"}, after = {"jump-pads"}, optional = true)
public final class PlayerVaultsModule extends ManagedModule implements PlayerVaultsServices {
    private fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig playerVaultsConfig;
    private fr.elias.oessentials.playervaults.internal.PlayerVaultsService playervaultsService;

    @Override
    protected void start() {
        cleanup("playervaultsService", () -> { if (playervaultsService != null) playervaultsService.stop(); });
        initPlayerVaults();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.IPlayerVaultsAPI.class, this::getPlayerVaultsApi);
    }

    @Override public fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig getPlayerVaultsConfig() { return playerVaultsConfig; }
    @Override public fr.elias.oessentials.playervaults.internal.PlayerVaultsService getPlayervaultsService() { return playervaultsService; }

    private void initPlayerVaults() {
        this.playerVaultsConfig  = new fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig(plugin);
        this.playervaultsService = new fr.elias.oessentials.playervaults.internal.PlayerVaultsService(plugin);
        if (this.playervaultsService.enabled()) { plugin.getLogger().info("[Vaults] PlayerVaults enabled."); }
        else { plugin.getLogger().info("[Vaults] PlayerVaults disabled by config or storage unavailable."); }
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.registerIf(services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getFeatureSettings().playerVaultsEnabled(),
                new fr.elias.oessentials.playervaults.internal.commands.core.playercommands.VaultsCommand());
    }
}
