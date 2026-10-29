package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.messaging.api.PacketBus;

/** Published on the server scheduler after messaging subscriptions have been initialized. */
@ModuleApi("events")
public record MessagingReady(PacketBus packets) {}
