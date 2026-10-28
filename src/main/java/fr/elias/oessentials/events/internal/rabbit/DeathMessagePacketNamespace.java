package fr.elias.oessentials.events.internal.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.events.internal.rabbit.DeathMessagePacket;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class DeathMessagePacketNamespace extends PacketNamespace {
    public DeathMessagePacketNamespace() {
        super((short) 34);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27013, DeathMessagePacket.class, DeathMessagePacket::new);
    }
}
