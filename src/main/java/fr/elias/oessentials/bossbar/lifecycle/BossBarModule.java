package fr.elias.oessentials.bossbar.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.bossbar.internal.BossBarService;
import fr.elias.oessentials.bossbar.internal.BossBarToggleCommand;

@PluginModule(value = "boss-bar", dependencies = {"configuration::services"}, after = {"rtp"}, optional = true)
public final class BossBarModule extends ManagedModule implements BossBarServices {
    private fr.elias.oessentials.bossbar.internal.BossBarService bossBarService;

    @Override
    protected void start() {
        cleanup("bossBarService", () -> { if (bossBarService != null) bossBarService.stop(); });
        initBossBar();
        providePublicApi(fr.elias.oessentials.api.IBossBarAPI.class, this::getBossBarApi);
        registerReloadStep(8, "bossbar", this::reloadBossbar);
    }

    @Override public fr.elias.oessentials.bossbar.internal.BossBarService getBossBarService() { return bossBarService; }

    private void initBossBar() {
        if (services(ConfigurationServices.class).getFeatureSettings().bossbarEnabled() && BootstrapSupport.uiModuleAllowed(plugin, "bossbar")) {
            this.bossBarService = new BossBarService(plugin);
            this.bossBarService.start();
            services(ConfigurationServices.class).getCommands().register(new BossBarToggleCommand(this.bossBarService));
            plugin.getLogger().info("[BossBar] Enabled.");
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "bossbar");
            plugin.getLogger().info("[BossBar] Disabled by settings.yml.");
        }
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadBossbar(org.bukkit.command.CommandSender sender) {
        try {
            var target = plugin.getBossBarService();
            if (target != null) {
                target.reload();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.bossbar",
                        "<green>✔ Reloaded <white>bossbar</white></green>");
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.bossbar-skip",
                    "<yellow>• Skipped bossbar (service unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.bossbar-fail",
                    "<red>✘ Failed reloading bossbar: <gray>%error%</gray></red>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.FAILED;
        }
    }
}
