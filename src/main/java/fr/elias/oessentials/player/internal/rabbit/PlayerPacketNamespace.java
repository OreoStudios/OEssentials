package fr.elias.oessentials.player.internal.rabbit;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.player.internal.back.rabbit.packets.BackTeleportPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpJumpPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaAcceptPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaBringPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaRequestPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaSummonPacket;
import fr.elias.oessentials.player.internal.vanish.rabbit.VanishSyncPacket;

/**
 * Packet ids are on the wire. These were numbered by registration order at startup until
 * they got fixed ids; keep them unchanged.
 */
public final class PlayerPacketNamespace extends PacketNamespace {
    public PlayerPacketNamespace() {
        super((short) 31);
    }

    @Override
    protected void registerPackets() {
        registerPacket(27007, VanishSyncPacket.class, VanishSyncPacket::new);
        registerPacket(27015, TpJumpPacket.class, TpJumpPacket::new);
        registerPacket(27016, BackTeleportPacket.class, BackTeleportPacket::new);
        registerPacket(27017, TpaBringPacket.class, TpaBringPacket::new);
        registerPacket(27018, TpaRequestPacket.class, TpaRequestPacket::new);
        registerPacket(27019, TpaSummonPacket.class, TpaSummonPacket::new);
        registerPacket(27020, TpaAcceptPacket.class, TpaAcceptPacket::new);
    }
}
