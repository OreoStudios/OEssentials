package fr.elias.oessentials.messaging.internal.rabbitmq.packet.event;


import fr.elias.oessentials.messaging.api.PacketChannel;

public interface IncomingPacketListener {

    void onReceive(PacketChannel channel, byte[] content);

}

