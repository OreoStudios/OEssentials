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
    }

    @Override public fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig getPlayerVaultsConfig() { return playerVaultsConfig; }
    @Override public fr.elias.oessentials.playervaults.internal.PlayerVaultsService getPlayervaultsService() { return playervaultsService; }

    private void initPlayerVaults() {
        this.playerVaultsConfig  = new fr.elias.oessentials.playervaults.internal.PlayerVaultsConfig(plugin);
        this.playervaultsService = new fr.elias.oessentials.playervaults.internal.PlayerVaultsService(plugin);
        if (this.playervaultsService.enabled()) { plugin.getLogger().info("[Vaults] PlayerVaults enabled."); }
        else { plugin.getLogger().info("[Vaults] PlayerVaults disabled by config or storage unavailable."); }
    }
}
