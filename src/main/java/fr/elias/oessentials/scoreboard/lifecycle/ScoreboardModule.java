package fr.elias.oessentials.scoreboard.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.scoreboard.internal.ScoreboardConfig;
import fr.elias.oessentials.scoreboard.internal.ScoreboardService;
import fr.elias.oessentials.scoreboard.internal.ScoreboardToggleCommand;

@PluginModule(value = "scoreboard", dependencies = {"configuration::services"}, after = {"boss-bar"}, optional = true)
public final class ScoreboardModule extends ManagedModule implements ScoreboardServices {
    private ScoreboardService scoreboardService;

    @Override
    protected void start() {
        cleanup("scoreboardService", () -> { if (scoreboardService != null) scoreboardService.stop(); });
        initScoreboard();
        providePublicApi(fr.elias.oessentials.api.IScoreboardAPI.class, this::getScoreboardApi);
        registerReloadStep(9, "scoreboard", this::reloadScoreboard);
    }

    @Override public ScoreboardService getScoreboardService() { return scoreboardService; }

    private void initScoreboard() {
        if (services(ConfigurationServices.class).getFeatureSettings().scoreboardEnabled() && BootstrapSupport.uiModuleAllowed(plugin, "scoreboard")) {
            ScoreboardConfig sbCfg = ScoreboardConfig.load(plugin);
            this.scoreboardService = new ScoreboardService(plugin, sbCfg);
            this.scoreboardService.start();
            services(ConfigurationServices.class).getCommands().register(new ScoreboardToggleCommand(this.scoreboardService));
            plugin.getLogger().info("[Scoreboard] Enabled.");
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "scoreboard");
            BootstrapSupport.unregisterCommandHard(plugin, "sb");
            plugin.getLogger().info("[Scoreboard] Disabled via settings.yml");
        }
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadScoreboard(org.bukkit.command.CommandSender sender) {
        try {
            var target = plugin.getScoreboardService();
            if (target != null) {
                target.reload();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.scoreboard",
                        "<green>✔ Reloaded <white>scoreboard</white> <gray>(config + live refresh)</gray></green>");
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.scoreboard-skip",
                    "<yellow>• Skipped scoreboard (service unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.scoreboard-fail",
                    "<red>✘ Failed reloading scoreboard: <gray>%error%</gray></red>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
        }
    }
}
