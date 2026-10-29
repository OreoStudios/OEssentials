package fr.elias.oessentials.messaging.api;

import dev.oreo.modulith.core.ModuleApi;

/** Player counts reported by the proxy; 0 until a count has been received. */
@ModuleApi("services")
public interface NetworkPlayerCounts {
    int networkTotal();

    int serverTotal(String serverName);
}
