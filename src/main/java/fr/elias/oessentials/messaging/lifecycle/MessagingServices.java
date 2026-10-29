package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.PacketBus;
import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.messaging.api.NetworkPlayerCounts;

@ModuleApi("services")
public interface MessagingServices {
    NetworkPlayerCounts getNetworkPlayerCounts();

    /** Cross-server packets, or {@code null} until RabbitMQ connects and when it is disabled. */
    PacketBus getPacketBus();

    /**
     * Adds packets with fixed ids. Call it while your module starts, so the packets can be decoded
     * as soon as RabbitMQ connects. A packet or namespace id that is already taken fails startup.
     */
    void registerNamespace(PacketNamespace namespace);

    /**
     * Handles a packet type once RabbitMQ connects, before any packet is dispatched. Does nothing
     * when RabbitMQ is disabled or cannot connect.
     */
    <T extends Packet> void subscribe(Class<T> packetClass, PacketSubscriber<T> subscriber);
}
