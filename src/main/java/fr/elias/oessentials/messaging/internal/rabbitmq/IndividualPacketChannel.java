package fr.elias.oessentials.messaging.internal.rabbitmq;


import fr.elias.oessentials.messaging.api.PacketChannel;

import java.util.Collections;

public final class IndividualPacketChannel implements PacketChannel {
    private final Iterable<String> channel;

    private IndividualPacketChannel(String channel) {
        this.channel = Collections.singletonList(channel);
    }

    public static PacketChannel create(String channel) { return new IndividualPacketChannel(channel); }

    @Override public Iterable<String> getChannels() { return channel; }
}