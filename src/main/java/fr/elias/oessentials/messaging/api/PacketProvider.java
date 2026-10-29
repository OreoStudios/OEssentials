package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

@FunctionalInterface
@ModuleApi("services")
public interface PacketProvider<T extends Packet> {
    T createPacket();
}
