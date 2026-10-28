package fr.elias.oessentials.sync.internal.invsee.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.sync.internal.invsee.rabbit.packets.InvseeEditPacket;
import fr.elias.oessentials.sync.internal.invsee.rabbit.packets.InvseeOpenRequestPacket;
import fr.elias.oessentials.sync.internal.invsee.rabbit.packets.InvseeStatePacket;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class InvseePacketNamespace extends PacketNamespace {
    public InvseePacketNamespace() {
        super((short) 33);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27010, InvseeOpenRequestPacket.class, InvseeOpenRequestPacket::new);
        registerPacket(27011, InvseeStatePacket.class, InvseeStatePacket::new);
        registerPacket(27012, InvseeEditPacket.class, InvseeEditPacket::new);
    }
}
