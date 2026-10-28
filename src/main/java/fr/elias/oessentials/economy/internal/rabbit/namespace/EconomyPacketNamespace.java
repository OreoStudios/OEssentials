package fr.elias.oessentials.economy.internal.rabbit.namespace;


import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerJoinPacket;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerQuitPacket;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.SendRemoteMessagePacket;

public class EconomyPacketNamespace extends PacketNamespace {

    public EconomyPacketNamespace() {
        super((short) 1);
    }

    @Override
    protected void registerPackets() {
        registerPacket(0, PlayerJoinPacket.class, PlayerJoinPacket::new);
        registerPacket(1, PlayerQuitPacket.class, PlayerQuitPacket::new);
        registerPacket(2, SendRemoteMessagePacket.class, SendRemoteMessagePacket::new);
    }
}
