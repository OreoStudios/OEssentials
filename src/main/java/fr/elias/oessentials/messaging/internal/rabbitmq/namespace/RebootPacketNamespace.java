package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;

import fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.tp.SafeZoneEnterPacket;

/** Auto-reboot safe-zone handoff; ids are on the wire, keep them stable. */
public final class RebootPacketNamespace extends PacketNamespace {

    public static final int SAFE_ZONE_ENTER_ID = 2101;

    public RebootPacketNamespace() {
        super((short) 21);
    }

    @Override
    protected void registerPackets() {
        registerPacket(SAFE_ZONE_ENTER_ID, SafeZoneEnterPacket.class, SafeZoneEnterPacket::new);
    }
}
