package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.api.SendRemoteMessagePacket;

/** Packets messaging handles itself. Ids are on the wire. */
public final class MessagingPacketNamespace extends PacketNamespace {

    public MessagingPacketNamespace() {
        super((short) 2);
    }

    @Override
    protected void registerPackets() {
        registerPacket(2, SendRemoteMessagePacket.class, SendRemoteMessagePacket::new);
    }
}
