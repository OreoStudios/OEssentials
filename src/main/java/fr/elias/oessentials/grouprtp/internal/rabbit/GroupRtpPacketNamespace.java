package fr.elias.oessentials.grouprtp.internal.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class GroupRtpPacketNamespace extends PacketNamespace {
    public GroupRtpPacketNamespace() {
        super((short) 39);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27028, GroupRtpSyncPacket.class, GroupRtpSyncPacket::new);
    }
}
