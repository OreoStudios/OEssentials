package fr.elias.oessentials.chat.internal.msg;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class ChatPacketNamespace extends PacketNamespace {
    public ChatPacketNamespace() {
        super((short) 30);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27006, CrossServerMsgPacket.class, CrossServerMsgPacket::new);
    }
}
