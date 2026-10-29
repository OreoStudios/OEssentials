package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

import fr.elias.oessentials.messaging.internal.rabbitmq.IndividualPacketChannel;
import fr.elias.oessentials.messaging.internal.rabbitmq.MultiPacketChannel;

import java.util.Iterator;

@ModuleApi("services")
public interface PacketChannel extends Iterable<String> {

    static PacketChannel individual(String name) {
        return IndividualPacketChannel.create(name);
    }

    static PacketChannel multiple(String... names) {
        return MultiPacketChannel.create(names);
    }

    static PacketChannel multiple(PacketChannel... channels) {
        return MultiPacketChannel.create(channels);
    }

    Iterable<String> getChannels();

    @Override
    default Iterator<String> iterator() {
        return getChannels().iterator();
    }
}
