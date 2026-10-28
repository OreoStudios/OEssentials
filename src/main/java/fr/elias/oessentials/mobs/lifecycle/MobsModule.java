package fr.elias.oessentials.mobs.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.mobs.internal.killall.KillallLogViewCommand;
import fr.elias.oessentials.mobs.internal.killall.KillallLogger;
import fr.elias.oessentials.mobs.internal.killall.KillallRecorderCommand;

@PluginModule(value = "mobs", dependencies = {"configuration::services"}, after = {"discord-integration"}, optional = true)
public final class MobsModule extends ManagedModule implements MobsServices {
    private fr.elias.oessentials.mobs.internal.HealthBarListener healthBarListener;

    @Override
    protected void start() {
        initMobs();
        registerReloadStep(11, "healthbars", this::reloadHealthbars);
    }

    @Override public fr.elias.oessentials.mobs.internal.HealthBarListener getHealthBarListener() { return healthBarListener; }

    private void initMobs() {
        if (!services(ConfigurationServices.class).getFeatureSettings().mobsEnabled()) {
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

            if (services(ConfigurationServices.class).getFeatureSettings().mobsHealthbarEnabled()) {
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

        var killallLogger = new KillallLogger(plugin);
        var killExec = new KillallRecorderCommand(plugin, killallLogger);
        services(ConfigurationServices.class).getCommands().registerLegacy("killallr", killExec, killExec);
        services(ConfigurationServices.class).getCommands().registerLegacy("killallrlog", new KillallLogViewCommand(killallLogger));

        final fr.elias.oessentials.mobs.internal.SpawnMobCommand spawnCmd = new fr.elias.oessentials.mobs.internal.SpawnMobCommand();
        services(ConfigurationServices.class).getCommands().registerLegacy("spawnmob", spawnCmd);
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadHealthbars(org.bukkit.command.CommandSender sender) {
        // Replaces this module's listener; the old reload stacked a new one and reported "skipped".
        try {
            fr.elias.ultimateChristmas.UltimateChristmas xmasHook = null;
            try {
                var maybe = plugin.getServer().getPluginManager().getPlugin("UltimateChristmas");
                if (maybe instanceof fr.elias.ultimateChristmas.UltimateChristmas uc && maybe.isEnabled()) xmasHook = uc;
            } catch (Throwable ignored) {}
            if (healthBarListener != null) org.bukkit.event.HandlerList.unregisterAll(healthBarListener);
            var hbl = new fr.elias.oessentials.mobs.internal.HealthBarListener(plugin, xmasHook);
            if (hbl.isEnabled()) {
                plugin.getServer().getPluginManager().registerEvents(hbl, plugin);
                this.healthBarListener = hbl;
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.healthbars",
                        "<green>✔ Reloaded <white>mob health bars</white></green>");
            } else {
                this.healthBarListener = null;
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.healthbars-disabled",
                        "<yellow>• Mob health bars disabled by config</yellow>");
            }
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.healthbars-skip",
                    "<yellow>• Skipped mob health bars: <gray>%error%</gray></yellow>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
        }
        // Never counted towards the summary, as before.
        return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
    }
}
