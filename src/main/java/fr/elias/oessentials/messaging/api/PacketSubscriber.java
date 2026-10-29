package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface PacketSubscriber<T extends Packet> {

    void onReceive(PacketChannel channel, T packet);
}

