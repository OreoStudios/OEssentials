package fr.elias.oessentials.storage.internal.playerwarp.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.storage.internal.warps.rabbit.packets.PlayerWarpTeleportRequestPacket;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class PlayerWarpPacketNamespace extends PacketNamespace {
    public PlayerWarpPacketNamespace() {
        super((short) 13);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27008, PlayerWarpTeleportRequestPacket.class, PlayerWarpTeleportRequestPacket::new);
    }
}
