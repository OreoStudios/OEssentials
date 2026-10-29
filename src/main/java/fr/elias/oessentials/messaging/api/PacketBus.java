package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.function.Supplier;

/**
 * Cross-server packets over RabbitMQ.
 *
 * <p>Obtain it from {@code MessagingServices.getPacketBus()} or the {@code MessagingReady}
 * event. Packets registered with {@link #registerPacket} get ids in registration order, so every
 * server must register the same packets in the same order; prefer a {@link PacketNamespace} with
 * fixed ids for new packets.</p>
 */
@ModuleApi("services")
public interface PacketBus {
    /** Whether the connection is set up and incoming packets are being dispatched. */
    boolean isInitialized();

    /** Sends to {@link PacketChannels#GLOBAL}. */
    void sendPacket(Packet packet);

    void sendPacket(PacketChannel target, Packet packet);

    <T extends Packet> void subscribe(Class<T> packetClass, PacketSubscriber<T> subscriber);

    /** Receives packets sent to {@code channel} in addition to the default ones. */
    void subscribeChannel(PacketChannel channel);

    <T extends Packet> void registerPacket(Class<T> packetClass, Supplier<T> constructor);
}
