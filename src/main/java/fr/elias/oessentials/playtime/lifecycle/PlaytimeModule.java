package fr.elias.oessentials.playtime.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "playtime", dependencies = {"configuration::services"}, after = {"sharding"}, optional = true)
public final class PlaytimeModule extends ManagedModule implements PlaytimeServices {
    private fr.elias.oessentials.playtime.internal.PlaytimeRewardsService playtimeRewards;
    private fr.elias.oessentials.playtime.internal.PlaytimeTracker playtimeTracker;

    @Override
    protected void start() {
        cleanup("playtimeTracker", () -> { if (playtimeTracker != null) playtimeTracker.shutdown(); });
        initPlaytime();
    }

    @Override public fr.elias.oessentials.playtime.internal.PlaytimeRewardsService getPlaytimeRewards() { return playtimeRewards; }
    @Override public fr.elias.oessentials.playtime.internal.PlaytimeTracker getPlaytimeTracker() { return playtimeTracker; }

    private void initPlaytime() {
        this.playtimeTracker = new fr.elias.oessentials.playtime.internal.PlaytimeTracker(plugin);
        this.playtimeRewards = new fr.elias.oessentials.playtime.internal.PlaytimeRewardsService(plugin, playtimeTracker);
        this.playtimeRewards.init();

        if (!services(ConfigurationServices.class).getSettingsConfig().playtimeRewardsEnabled()) {
            this.playtimeRewards.setEnabled(false);
            plugin.getLogger().info("[Prewards] Disabled by settings.yml.");
        } else {
            plugin.getLogger().info("[Prewards] Enabled.");
        }

        if (services(ConfigurationServices.class).getSettingsConfig().playtimeRewardsEnabled()) {
            var prewardsCmd = new fr.elias.oessentials.playtime.internal.PrewardsCommand(plugin, this.playtimeRewards);
            services(ConfigurationServices.class).getCommands().registerLegacy("prewards", prewardsCmd, prewardsCmd);
        }

        var playtimeCmd = new fr.elias.oessentials.playtime.internal.commands.core.playercommands.PlaytimeCommand();
        services(ConfigurationServices.class).getCommands().registerLegacy("playtime", playtimeCmd, playtimeCmd);
    }
}
