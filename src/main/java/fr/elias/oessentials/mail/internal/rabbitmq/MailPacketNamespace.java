package fr.elias.oessentials.mail.internal.rabbitmq;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class MailPacketNamespace extends PacketNamespace {
    public MailPacketNamespace() {
        super((short) 38);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27027, MailDeliveryPacket.class, MailDeliveryPacket::new);
    }
}
