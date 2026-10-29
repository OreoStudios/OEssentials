package fr.elias.oessentials.currency.internal.rabbitmq;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class CurrencyPacketNamespace extends PacketNamespace {
    public CurrencyPacketNamespace() {
        super((short) 35);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27021, CurrencyTransferPacket.class, CurrencyTransferPacket::new);
        registerPacket(27022, CurrencyUpdatePacket.class, CurrencyUpdatePacket::new);
        registerPacket(27023, CurrencySyncPacket.class, CurrencySyncPacket::new);
    }
}
