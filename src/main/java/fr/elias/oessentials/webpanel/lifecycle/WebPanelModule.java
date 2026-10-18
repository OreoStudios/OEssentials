package fr.elias.oessentials.webpanel.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.afk.lifecycle.AfkServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "web-panel", dependencies = {"afk::services", "configuration::services"}, after = {"playtime"}, optional = true)
public final class WebPanelModule extends ManagedModule implements WebPanelServices {
    // Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬ Web Panel Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬Ã¢â€â‚¬
    private fr.elias.oessentials.webpanel.internal.WebPanelSyncService webPanelSyncService;

    @Override
    protected void start() {
        cleanup("webPanelSyncService", () -> { if (webPanelSyncService != null) webPanelSyncService.stop(); });
        initWebPanel();
    }

    @Override public fr.elias.oessentials.webpanel.internal.WebPanelSyncService getWebPanelSyncService() { return webPanelSyncService; }

    private void initWebPanel() {
        fr.elias.oessentials.webpanel.internal.WebPanelConfig wpConfig =
                new fr.elias.oessentials.webpanel.internal.WebPanelConfig(plugin);
        fr.elias.oessentials.webpanel.internal.WebPanelClient client =
                new fr.elias.oessentials.webpanel.internal.WebPanelClient(wpConfig, plugin.getLogger());

        services(ConfigurationServices.class).getCommands().registerLegacy("weblink",
                new fr.elias.oessentials.webpanel.internal.WebLinkCommand(wpConfig, client));

        if (!wpConfig.isEnabled()) {
            plugin.getLogger().info("[WebPanel] Disabled (set web-panel.enabled=true and api-key in config.yml to enable).");
            return;
        }

        // Start event
        // Start event-driven player sync (join/quit/block events + periodic)
        this.webPanelSyncService = new fr.elias.oessentials.webpanel.internal.WebPanelSyncService(plugin, client);
        this.webPanelSyncService.start();
        if (services(AfkServices.class).getAfkService() != null) services(AfkServices.class).getAfkService().setWebPanelSync(this.webPanelSyncService);

        // Register reward (oreopanel/reward.yml)
        fr.elias.oessentials.webpanel.internal.RegisterRewardConfig rrConfig =
                new fr.elias.oessentials.webpanel.internal.RegisterRewardConfig(plugin);
        fr.elias.oessentials.shop.internal.hooks.ItemsAdderHook rrIaHook =
                new fr.elias.oessentials.shop.internal.hooks.ItemsAdderHook(plugin);
        fr.elias.oessentials.shop.internal.hooks.NexoHook rrNexoHook =
                new fr.elias.oessentials.shop.internal.hooks.NexoHook(plugin);
        fr.elias.oessentials.webpanel.internal.RegisterRewardService rrService =
                new fr.elias.oessentials.webpanel.internal.RegisterRewardService(
                        plugin, rrConfig, client, rrIaHook, rrNexoHook);
        plugin.getServer().getPluginManager().registerEvents(rrService, plugin);
        services(ConfigurationServices.class).getCommands().registerLegacy("registerreward",
                new fr.elias.oessentials.webpanel.internal.RegisterRewardCommand(
                        plugin, rrConfig, rrService, rrIaHook, rrNexoHook));

        plugin.getLogger().info("[WebPanel] Enabled Ã¢â‚¬â€ syncing to " + wpConfig.getUrl());
    }
}
