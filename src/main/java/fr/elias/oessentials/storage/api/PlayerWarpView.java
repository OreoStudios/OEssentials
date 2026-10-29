package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Read-only view of a player warp. */
@ModuleApi("services")
public interface PlayerWarpView {
    UUID getOwner();
    String getName();
    String getDescription();
    boolean isLocked();
}
