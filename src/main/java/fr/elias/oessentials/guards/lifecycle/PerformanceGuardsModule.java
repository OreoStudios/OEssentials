package fr.elias.oessentials.guards.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.guards.internal.listeners.ContainerSpamGuardListener;
import fr.elias.oessentials.guards.internal.services.JoinFloodGuardService;

@PluginModule(value = "performance-guards", dependencies = {"configuration::services"})
public final class PerformanceGuardsModule extends ManagedModule implements PerformanceGuardsServices {
    private JoinFloodGuardService joinFloodGuardService;
    private final fr.elias.oessentials.guards.internal.services.VisitorService visitors = new fr.elias.oessentials.guards.internal.services.VisitorService();

    @Override
    protected void start() {
        initPerformanceGuards();
    }

    @Override public JoinFloodGuardService getJoinFloodGuardService() { return joinFloodGuardService; }
    @Override public void setVisitor(java.util.UUID playerId, boolean visitor) { visitors.setVisitor(playerId, visitor); }

    private void initPerformanceGuards() {
        this.joinFloodGuardService = new JoinFloodGuardService(plugin);
        plugin.getServer().getPluginManager().registerEvents(this.joinFloodGuardService, plugin);
        plugin.getServer().getPluginManager().registerEvents(new ContainerSpamGuardListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.guards.internal.listeners.VisitorGuardListener(visitors), plugin);

        plugin.getLogger().info("[Performance] Join flood protection "
                + (services(ConfigurationServices.class).getPerformanceGuardSettings().joinFloodProtectionEnabled() ? "enabled." : "disabled."));
        plugin.getLogger().info("[Performance] Container spam guard "
                + (services(ConfigurationServices.class).getPerformanceGuardSettings().containerSpamGuardEnabled() ? "enabled." : "disabled."));
    }
}
