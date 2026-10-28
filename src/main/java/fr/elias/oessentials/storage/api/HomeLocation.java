package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;

/** Where a home is, including the server that owns it. */
@ModuleApi("services")
public interface HomeLocation {
    String getWorld();
    double getX();
    double getY();
    double getZ();
    String getServer();
}
