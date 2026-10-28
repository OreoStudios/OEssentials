package fr.elias.oessentials.messaging.internal.cross.rabbit.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.internal.cross.rabbit.packets.CrossInvPacket;

public final class CrossInvPacketNamespace extends PacketNamespace {


    public static final short NS_ID = 91;
    public static final int   CROSS_INV_PACKET_ID = 9101;

    public CrossInvPacketNamespace() {
        super(NS_ID);
    }

    @Override
    protected void registerPackets() {
        registerPacket(CROSS_INV_PACKET_ID, CrossInvPacket.class, CrossInvPacket::new);
    }
}
