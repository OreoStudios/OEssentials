package fr.elias.oessentials.mobs.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.commands.internal.commands.core.admins.KillallLogViewCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.KillallRecorderCommand;

@PluginModule(value = "mobs", dependencies = {"configuration::services"}, after = {"discord-integration"}, optional = true)
public final class MobsModule extends ManagedModule implements MobsServices {
    private fr.elias.oessentials.mobs.internal.HealthBarListener healthBarListener;

    @Override
    protected void start() {
        initMobs();
    }

    @Override public fr.elias.oessentials.mobs.internal.HealthBarListener getHealthBarListener() { return healthBarListener; }

    private void initMobs() {
        if (!services(ConfigurationServices.class).getSettingsConfig().mobsEnabled()) {
            plugin.getLogger().info("[MOBS] Disabled by settings.yml.");
            return;
        }
        try {
            fr.elias.ultimateChristmas.UltimateChristmas xmasHook = null;
            try {
                var maybe = plugin.getServer().getPluginManager().getPlugin("UltimateChristmas");
                if (maybe instanceof fr.elias.ultimateChristmas.UltimateChristmas uc && maybe.isEnabled()) {
                    xmasHook = uc;
                    plugin.getLogger().info("[MOBS] UltimateChristmas hooked.");
                }
            } catch (Throwable ignored) {}

            if (services(ConfigurationServices.class).getSettingsConfig().mobsHealthbarEnabled()) {
                try {
                    var hbl = new fr.elias.oessentials.mobs.internal.HealthBarListener(plugin, xmasHook);
                    this.healthBarListener = hbl;
                    plugin.getServer().getPluginManager().registerEvents(hbl, plugin);
                    plugin.getLogger().info("[MOBS] Health bars enabled.");
                } catch (Throwable t) {
                    plugin.getLogger().warning("[MOBS] Failed to init health bars: " + t.getMessage());
                }
            } else {
                plugin.getLogger().info("[MOBS] Disabled by settings.yml.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[MOBS] Unexpected failure: " + t.getMessage());
        }

        var killExec = new KillallRecorderCommand(plugin, services(ConfigurationServices.class).getKillallLogger());
        services(ConfigurationServices.class).getCommands().registerLegacy("killallr", killExec, killExec);
        services(ConfigurationServices.class).getCommands().registerLegacy("killallrlog", new KillallLogViewCommand(services(ConfigurationServices.class).getKillallLogger()));

        final fr.elias.oessentials.mobs.internal.SpawnMobCommand spawnCmd = new fr.elias.oessentials.mobs.internal.SpawnMobCommand();
        services(ConfigurationServices.class).getCommands().registerLegacy("spawnmob", spawnCmd);
    }
}
