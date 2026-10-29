package fr.elias.oessentials.inventory.internal;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.inventory.internal.auctionhouse.rabbitmq.AuctionSyncPacket;
import fr.elias.oessentials.inventory.internal.orders.rabbitmq.OrderSyncPacket;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class InventoriesPacketNamespace extends PacketNamespace {
    public InventoriesPacketNamespace() {
        super((short) 36);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27024, AuctionSyncPacket.class, AuctionSyncPacket::new);
        registerPacket(27025, OrderSyncPacket.class, OrderSyncPacket::new);
    }
}
