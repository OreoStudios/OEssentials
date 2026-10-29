package fr.elias.oessentials.messaging.internal.rabbitmq.packet.event;


import fr.elias.oessentials.messaging.api.PacketChannel;

public interface PacketSender {

    void sendPacket(PacketChannel channel, byte[] content);
    void registerChannel(PacketChannel channel);
    void registerListener(IncomingPacketListener listener);

    void close();
}
