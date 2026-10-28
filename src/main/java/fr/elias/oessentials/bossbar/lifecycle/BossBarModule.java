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
}
