package fr.elias.oessentials.daily.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "daily", dependencies = {"configuration::services"}, after = {"freeze"})
public final class DailyModule extends ManagedModule implements DailyServices {
    private fr.elias.oessentials.daily.internal.DailyConfig dailyConfig;
    private fr.elias.oessentials.daily.internal.RewardsConfig dailyRewardsConfig;
    private fr.elias.oessentials.daily.internal.DailyService dailyService;
    private fr.elias.oessentials.daily.internal.DailyMongoStore dailyStore;

    @Override
    protected void start() {
        cleanup("dailyStore", () -> { if (dailyStore != null) dailyStore.close(); });
        initDaily();
        providePublicApi(fr.elias.oessentials.api.IDailyAPI.class, this::getDailyApi);
        registerReloadStep(14, "daily", this::reloadDaily);
        registerConfigFile("dailyrewards.yml", () -> { plugin.getDailyConfig().load(); plugin.getDailyRewardsConfig().load(); });
    }

    @Override public fr.elias.oessentials.daily.internal.DailyConfig getDailyConfig() { return dailyConfig; }
    @Override public fr.elias.oessentials.daily.internal.RewardsConfig getDailyRewardsConfig() { return dailyRewardsConfig; }
    @Override public fr.elias.oessentials.daily.internal.DailyService getDailyService() { return dailyService; }
    @Override public fr.elias.oessentials.daily.internal.DailyMongoStore getDailyStore() { return dailyStore; }

    private void initDaily() {
        this.dailyConfig = new fr.elias.oessentials.daily.internal.DailyConfig(plugin);
        this.dailyConfig.load();
        fr.elias.oessentials.daily.internal.DailyStorage dailyStorage = fr.elias.oessentials.daily.internal.DailyStorage.create(plugin, dailyConfig);
        this.dailyRewardsConfig = new fr.elias.oessentials.daily.internal.RewardsConfig(plugin);
        this.dailyRewardsConfig.load();
        this.dailyService = new fr.elias.oessentials.daily.internal.DailyService(plugin, dailyConfig, dailyStorage, dailyRewardsConfig);
        var dailyCmd = new fr.elias.oessentials.daily.internal.DailyCommand(plugin, dailyConfig, dailyService, dailyRewardsConfig);
        services(ConfigurationServices.class).getCommands().registerLegacy("daily", dailyCmd, dailyCmd);
        plugin.getLogger().info("[Daily] Rewards system initialized with " + (dailyConfig.mongo.enabled ? "MongoDB" : "file-based") + " storage.");
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadDaily(org.bukkit.command.CommandSender sender) {
        try {
            var dc = plugin.getDailyConfig();
            var rc = plugin.getDailyRewardsConfig();
            if (dc != null && rc != null) {
                dc.load();
                rc.load();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.dailyrewards",
                        "<green>✔ Reloaded <white>dailyrewards/dailyrewards.yml</white></green>");
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.dailyrewards-skip",
                    "<yellow>• Skipped dailyrewards (service unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.dailyrewards-fail",
                    "<red>✘ Failed reloading dailyrewards: <gray>%error%</gray></red>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
        }
    }
}
