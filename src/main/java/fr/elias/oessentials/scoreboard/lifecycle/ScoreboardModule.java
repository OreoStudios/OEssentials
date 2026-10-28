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
}
