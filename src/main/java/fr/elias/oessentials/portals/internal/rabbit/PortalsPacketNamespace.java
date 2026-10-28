package fr.elias.oessentials.portals.internal.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class PortalsPacketNamespace extends PacketNamespace {
    public PortalsPacketNamespace() {
        super((short) 37);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27026, PortalTeleportPacket.class, PortalTeleportPacket::new);
    }
}
