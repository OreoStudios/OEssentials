package fr.elias.oessentials.economy.internal.cross.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;

/** Join/quit announcements that keep the cross-server offline player cache current. Ids are on the wire. */
public final class PlayerPresencePacketNamespace extends PacketNamespace {

    public PlayerPresencePacketNamespace() {
        super((short) 1);
    }

    @Override
    protected void registerPackets() {
        registerPacket(0, PlayerJoinPacket.class, PlayerJoinPacket::new);
        registerPacket(1, PlayerQuitPacket.class, PlayerQuitPacket::new);
    }
}
