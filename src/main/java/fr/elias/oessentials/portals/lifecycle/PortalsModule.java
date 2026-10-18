package fr.elias.oessentials.portals.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;

@PluginModule(value = "portals", dependencies = {"messaging::events", "configuration::services", "messaging::services"}, after = {"jails"})
public final class PortalsModule extends ManagedModule implements PortalsServices {
    private fr.elias.oessentials.portals.internal.PortalsManager portals;

    @Override
    protected void start() {
        initPortals();
        listenForModuleEvents(this);
    }

    @Override public fr.elias.oessentials.portals.internal.PortalsManager getPortals() { return portals; }

    private void initPortals() {
        this.portals = new fr.elias.oessentials.portals.internal.PortalsManager(plugin);

        var wandListener = new fr.elias.oessentials.portals.internal.PortalWandListener(
                this.portals, this.portals.getConfig());
        plugin.getServer().getPluginManager().registerEvents(wandListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new fr.elias.oessentials.portals.internal.PortalsListener(this.portals, wandListener), plugin);

        // Register BungeeCord outgoing channel for cross-server connects
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");

        // Wire up cross-server support if RabbitMQ is available
        if (services(MessagingServices.class).getPacketManager() != null && services(MessagingServices.class).getPacketManager().isInitialized()) {
            this.portals.initCrossServer(services(MessagingServices.class).getPacketManager());
            plugin.getLogger().info("[Portals] Cross-server support enabled.");
        } else {
            plugin.getLogger().info("[Portals] RabbitMQ unavailable — cross-server portals disabled.");
        }

        var portalCmd = new fr.elias.oessentials.portals.internal.PortalsCommand(this.portals, wandListener);
        services(ConfigurationServices.class).getCommands().registerLegacy("portal", portalCmd, portalCmd);
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive() || portals == null) return;
        portals.initCrossServer(event.packets());
        plugin.getLogger().info("[Portals] Cross-server support enabled.");
    }

}
