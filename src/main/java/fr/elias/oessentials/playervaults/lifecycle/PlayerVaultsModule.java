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
        registerReloadStep(12, "playervaults", this::reloadPlayervaults);
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

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadPlayervaults(org.bukkit.command.CommandSender sender) {
        try {
            var pv = playervaultsService;
            if (pv != null) {
                pv.reload();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.playervaults",
                        "<green>✔ Reloaded <white>playervaults</white> <gray>(%storage%)</gray></green>",
                        java.util.Map.of("storage", storageName(pv)));
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.playervaults-skip",
                    "<yellow>• Skipped playervaults (service unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.playervaults-fail",
                    "<red>✘ Failed reloading playervaults: <gray>%error%</gray></red>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
        }
    }

    private static String storageName(fr.elias.oessentials.playervaults.internal.PlayerVaultsService svc) {
        try {
            var f = svc.getClass().getDeclaredField("storage");
            f.setAccessible(true);
            Object st = f.get(svc);
            if (st == null) return "disabled";
            String n = st.getClass().getSimpleName().toLowerCase();
            if (n.contains("mongo")) return "mongodb";
            if (n.contains("yaml"))  return "yaml";
            return n;
        } catch (Throwable ignored) {
            return "ok";
        }
    }
}
