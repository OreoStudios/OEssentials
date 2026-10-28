package fr.elias.oessentials.rtp.internal;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class RtpPacketNamespace extends PacketNamespace {
    public RtpPacketNamespace() {
        super((short) 32);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27009, RtpTeleportRequestPacket.class, RtpTeleportRequestPacket::new);
    }
}
