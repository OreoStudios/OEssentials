package fr.elias.oessentials.storage.internal.spawn.rabbit.namespace;

import fr.elias.oessentials.storage.internal.spawn.rabbit.packets.SpawnTeleportRequestPacket;
import fr.elias.oessentials.messaging.api.PacketNamespace;

public final class SpawnPacketNamespace extends PacketNamespace {

    public static final int SPAWN_TP_REQ_ID = 1003;

    public SpawnPacketNamespace() {
        super((short) 12);
    }

    @Override
    protected void registerPackets() {
        registerPacket(SPAWN_TP_REQ_ID, SpawnTeleportRequestPacket.class, SpawnTeleportRequestPacket::new);
    }
}
