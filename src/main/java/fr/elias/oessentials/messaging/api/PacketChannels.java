package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

import fr.elias.oessentials.messaging.internal.rabbitmq.IndividualPacketChannel;
import fr.elias.oessentials.messaging.internal.rabbitmq.MultiPacketChannel;

@ModuleApi("services")
public final class PacketChannels {
    private PacketChannels() {}

    public static final PacketChannel GLOBAL =
            IndividualPacketChannel.create("global");

    public static PacketChannel individual(String channel) {
        return IndividualPacketChannel.create(channel);
    }

    public static PacketChannel multiple(String... channels) {
        return MultiPacketChannel.create(channels);
    }

    public static PacketChannel multiple(PacketChannel... channels) {
        return MultiPacketChannel.create(channels);
    }
}

