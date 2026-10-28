package fr.elias.oessentials.currency.internal.rabbitmq;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * RabbitMQ packet namespace for currency system
 */
public class CurrencyPacketNamespace extends PacketNamespace {

    public CurrencyPacketNamespace() {
        super((short) 7);
    }

    @Override
    protected void registerPackets() {
        registerPacket(1, CurrencyTransferPacket.class, CurrencyTransferPacket::new);

        registerPacket(2, CurrencyUpdatePacket.class, CurrencyUpdatePacket::new);

        registerPacket(3, CurrencySyncPacket.class, CurrencySyncPacket::new);
    }
}