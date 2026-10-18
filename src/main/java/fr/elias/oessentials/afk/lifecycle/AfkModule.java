package fr.elias.oessentials.afk.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.afk.internal.AfkListener;
import fr.elias.oessentials.afk.internal.AfkPoolService;
import fr.elias.oessentials.afk.internal.AfkService;

@PluginModule(value = "afk", after = {"proxy-messaging"})
public final class AfkModule extends ManagedModule implements AfkServices {
    private AfkService afkService;
    private AfkPoolService afkPoolService;

    @Override
    protected void start() {
        cleanup("afkService", () -> { if (afkService != null) afkService.shutdown(); });
        cleanup("afkPoolService", () -> { if (afkPoolService != null) afkPoolService.cleanupAll(); });
        initAfk();
    }

    @Override public AfkService getAfkService() { return afkService; }
    @Override public AfkPoolService getAfkPoolService() { return afkPoolService; }

    private void initAfk() {
        this.afkService = new AfkService(plugin);
        plugin.getServer().getPluginManager().registerEvents(new AfkListener(plugin, afkService), plugin);

        if (afkService.getAfkConfig().poolEnabled()) {
            try {
                this.afkPoolService = new AfkPoolService(plugin, afkService);
                afkService.setPoolService(afkPoolService);
                plugin.getLogger().info("[AfkPool] Enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[AfkPool] Failed to initialize: " + t.getMessage());
                this.afkPoolService = null;
            }
        } else {
            plugin.getLogger().info("[AfkPool] Disabled by afk/config.yml");
            this.afkPoolService = null;
        }
    }
}
