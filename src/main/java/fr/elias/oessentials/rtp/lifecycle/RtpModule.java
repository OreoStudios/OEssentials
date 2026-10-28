package fr.elias.oessentials.rtp.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.rtp.internal.RtpCommand;
import fr.elias.oessentials.rtp.internal.RtpConfig;
import fr.elias.oessentials.rtp.internal.RtpPendingService;
import fr.elias.oessentials.rtp.internal.listeners.RtpJoinListener;

@PluginModule(value = "rtp", dependencies = {"messaging::events", "configuration::services", "messaging::services"}, after = {"player-vaults"}, optional = true)
public final class RtpModule extends ManagedModule implements RtpServices {
    private fr.elias.oessentials.rtp.internal.RtpCrossServerBridge rtpBridge;
    @Override public fr.elias.oessentials.rtp.internal.RtpCrossServerBridge getRtpBridge() { return rtpBridge; }

    private RtpPendingService rtpPendingService;
    private RtpConfig rtpConfig;
    private final java.util.Map<java.util.UUID, Long> rtpCooldownCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected void start() {
        services(MessagingServices.class).registerNamespace(new fr.elias.oessentials.rtp.internal.RtpPacketNamespace());
        initRtp();
        listenForModuleEvents(this);
    }

    @Override public RtpPendingService getRtpPendingService() { return rtpPendingService; }
    @Override public RtpConfig getRtpConfig() { return rtpConfig; }
    @Override public java.util.Map<java.util.UUID, Long> getRtpCooldownCache() { return rtpCooldownCache; }

    private void initRtp() {
        this.rtpPendingService = new RtpPendingService();
        this.rtpConfig         = new RtpConfig(plugin);
        if (services(ConfigurationServices.class).getFeatureSettings().rtpEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(new RtpJoinListener(plugin), plugin);
        }
        if (!services(ConfigurationServices.class).getFeatureSettings().rtpEnabled()) {
            BootstrapSupport.unregisterCommandHard(plugin, "rtp"); BootstrapSupport.unregisterCommandHard(plugin, "wild");
            plugin.getLogger().info("[RTP] Disabled by settings.yml.");
        } else if (!this.rtpConfig.isEnabled()) {
            BootstrapSupport.unregisterCommandHard(plugin, "rtp"); BootstrapSupport.unregisterCommandHard(plugin, "wild");
            plugin.getLogger().info("[RTP] Disabled by rtp.yml.");
        } else {
            services(ConfigurationServices.class).getCommands().register(new RtpCommand());
            plugin.getLogger().info("[RTP] Enabled.");
        }

        // Bridge requires packetManager which is created async in initRabbitMQ().
        // It is initialized in initBrokers() once the connection is established.

    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive() || rtpConfig == null || !rtpConfig.isCrossServerEnabled()) return;
        rtpBridge = new fr.elias.oessentials.rtp.internal.RtpCrossServerBridge(
                plugin, event.packets(), services(ConfigurationServices.class).getServerConfig().serverName());
        plugin.getLogger().info("[RTP] Cross-server RTP bridge ready.");
    }

}
