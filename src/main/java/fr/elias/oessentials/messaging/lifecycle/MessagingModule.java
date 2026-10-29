package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.configuration.api.SafeZoneRelay;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketChannels;
import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.messaging.api.SendRemoteMessagePacket;
import fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver;
import fr.elias.oessentials.messaging.internal.rabbitmq.handler.RemoteMessagePacketHandler;
import fr.elias.oessentials.messaging.internal.rabbitmq.namespace.MessagingPacketNamespace;
import fr.elias.oessentials.messaging.internal.rabbitmq.namespace.RebootPacketNamespace;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.PacketManager;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.tp.SafeZoneEnterPacket;
import fr.elias.oessentials.messaging.internal.rabbitmq.sender.RabbitMQSender;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.platform.scheduling.OreScheduler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The RabbitMQ transport. Feature modules depend on it and plug in their own packets
 * ({@link #registerNamespace}), handlers ({@link #subscribe}) and brokers ({@link MessagingReady});
 * messaging itself depends only on configuration, so it starts right after it.
 */
@PluginModule(value = "messaging", dependencies = {"configuration::services"})
public final class MessagingModule extends ManagedModule implements MessagingServices {
    private volatile NetworkCountReceiver networkCountReceiver;
    private volatile PacketManager packetManager;
    private volatile RabbitMQSender connectingSender;
    private final List<PacketNamespace> namespaces = new CopyOnWriteArrayList<>();
    private final List<Consumer<PacketManager>> pendingSubscriptions = new CopyOnWriteArrayList<>();

    @Override
    protected void start() {
        cleanup("connectingSender", () -> { if (connectingSender != null) connectingSender.close(); });
        cleanup("packetManager", () -> { if (packetManager != null) packetManager.close(); });
        cleanup("networkCountReceiver", () -> { if (networkCountReceiver != null) networkCountReceiver.stop(); });
        registerNamespaces();
        initSafeZoneRelay();
        initRabbitMQ();
    }

    @Override public fr.elias.oessentials.messaging.api.NetworkPlayerCounts getNetworkPlayerCounts() {
        return new fr.elias.oessentials.messaging.api.NetworkPlayerCounts() {
            @Override public int networkTotal() { return NetworkCountReceiver.getNetworkTotal(); }
            @Override public int serverTotal(String serverName) { return NetworkCountReceiver.getServerTotal(serverName); }
        };
    }
    @Override public PacketManager getPacketBus() { return packetManager; }

    @Override
    public void registerNamespace(PacketNamespace namespace) {
        namespaces.add(namespace);
        PacketManager live = packetManager;
        if (live != null) live.getPacketRegistry().register(namespace);
    }

    @Override
    public <T extends Packet> void subscribe(Class<T> packetClass, PacketSubscriber<T> subscriber) {
        pendingSubscriptions.add(bus -> bus.subscribe(packetClass, subscriber));
        PacketManager live = packetManager;
        if (live != null) live.subscribe(packetClass, subscriber);
    }

    /** Packets messaging handles itself; every other module registers its own. */
    private void registerNamespaces() {
        registerNamespace(new MessagingPacketNamespace());
        registerNamespace(new RebootPacketNamespace());
    }

    /** Auto-reboot (configuration) asks the safe-zone server to place arriving players in a region. */
    private void initSafeZoneRelay() {
        var configuration = services(ConfigurationServices.class);
        configuration.setSafeZoneRelay(new SafeZoneRelay() {
            @Override public boolean packetsReady() { return packetManager != null && packetManager.isInitialized(); }
            @Override public void requestSafeZone(UUID playerId, String targetServer, String worldName, String regionName) {
                packetManager.sendPacket(new SafeZoneEnterPacket(playerId, targetServer, worldName, regionName));
            }
        });
        cleanup("safeZoneRelay", () -> configuration.setSafeZoneRelay(null));
    }

    private void initRabbitMQ() {
        if (!services(ConfigurationServices.class).getRabbitEnabled()) {
            plugin.getLogger().info("[RABBIT] Disabled.");
            return;
        }

        // The connection attempt runs async so the server thread is never blocked; modules work
        // locally until MessagingReady is published on the main thread.
        plugin.getLogger().info("[RABBIT] Connecting async (non-blocking)...");

        final String uri        = plugin.getConfig().getString("rabbitmq.uri");
        final String serverName = services(ConfigurationServices.class).getServerConfig().serverName();

        OreScheduler.runAsync(plugin, () -> {
            if (!isActive()) return;
            RabbitMQSender rabbit = new RabbitMQSender(uri, serverName, services(ConfigurationServices.class).getServerConfig()::isDebugEnabled);
            connectingSender = rabbit;
            if (!isActive()) { rabbit.close(); return; }
            boolean connected = rabbit.connect();
            if (!isActive()) { rabbit.close(); return; }

            try {
                OreScheduler.run(plugin, () -> {
                    if (!isActive()) { rabbit.close(); return; }
                    if (!connected) {
                        rabbit.close();
                        connectingSender = null;
                        plugin.getLogger().severe("[RABBIT] Connect failed; continuing without messaging.");
                        return;
                    }
                    onConnected(rabbit, uri, serverName);
                });
            } catch (RuntimeException schedulingFailure) {
                rabbit.close();
                if (isActive()) throw schedulingFailure;
            }
        });
    }

    private void onConnected(RabbitMQSender rabbit, String uri, String serverName) {
        PacketManager bus = new PacketManager(plugin, rabbit, namespaces);
        // Handlers registered by modules are in place before any packet is dispatched.
        pendingSubscriptions.forEach(subscription -> subscription.accept(bus));
        this.packetManager = bus;

        try {
            this.networkCountReceiver = new NetworkCountReceiver(plugin, uri);
            if (this.networkCountReceiver.start()) { plugin.getLogger().info("[NetworkCountReceiver] Listening for proxy player counts."); }
            else { plugin.getLogger().warning("[NetworkCountReceiver] Failed to start."); this.networkCountReceiver = null; }
        } catch (Throwable t) { plugin.getLogger().warning("[NetworkCountReceiver] Error: " + t.getMessage()); this.networkCountReceiver = null; }

        bus.subscribeChannel(PacketChannels.GLOBAL);
        bus.subscribeChannel(PacketChannel.individual(serverName));
        plugin.getLogger().info("[RABBIT] Subscribed channels: global + individual(" + serverName + ")");

        bus.subscribe(SendRemoteMessagePacket.class, new RemoteMessagePacketHandler());
        bus.subscribe(SafeZoneEnterPacket.class, (channel, pkt) -> {
            var autoReboot = services(ConfigurationServices.class).getAutoReboot();
            if (pkt != null && autoReboot != null) {
                autoReboot.acceptSafeZoneEnter(pkt.getPlayerId(), pkt.getTargetServer(), pkt.getWorldName(), pkt.getRegionName());
            }
        });

        bus.init();

        try { plugin.getLogger().info("[RABBIT] Packet registry checksum=" + bus.registryChecksum()); }
        catch (Throwable ignored) {}

        plugin.getLogger().info("[RABBIT] Connected and subscriptions active.");
        publishEvent(new MessagingReady(bus));
    }
}
