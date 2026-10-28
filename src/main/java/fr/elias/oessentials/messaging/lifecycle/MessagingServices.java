package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.rabbit.BackBroker;
import fr.elias.oessentials.messaging.internal.cross.ModBridge;
import fr.elias.oessentials.messaging.internal.invsee.InvseeService;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.InvseeCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker;
import fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker;
import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.PacketBus;
import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import java.util.Map;
import java.util.UUID;

@ModuleApi("services")
public interface MessagingServices {
    TpCrossServerBroker getTpBroker();
    BackBroker getBackBroker();
    fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver getNetworkCountReceiver();
    TpaCrossServerBroker getTpaBroker();
    ModBridge getModBridge();
    InvseeService getInvseeService();
    fr.elias.oessentials.messaging.internal.invlook.manager.InvlookManager getInvlookManager();
    InvseeCrossServerBroker getInvseeBroker();
    TradeCrossServerBroker getTradeBroker();
    /** Cross-server packets, or {@code null} when RabbitMQ is disabled. */
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
    fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker();
    Map<UUID, BackLocation> getPendingBackTeleports();
}
