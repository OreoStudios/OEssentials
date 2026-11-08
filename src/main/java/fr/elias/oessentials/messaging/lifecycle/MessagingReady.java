package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.messaging.api.PacketBus;

/** Transient local readiness notification, delivered through global Folia routes.
 * PacketBus is a live connection; this event must not be serialized into a persistent event store. */
@ModuleApi("events")
public record MessagingReady(PacketBus packets) {}
