package fr.elias.oessentials.sync.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Inventory snapshots for the staff inventory viewer, local or fetched from another server. */
@ModuleApi("services")
public interface InvseeSnapshots {
    /** The best snapshot available for {@code targetId}, or {@code null}. */
    InventoryService.Snapshot requestSnapshot(UUID targetId);

    /** Applies an edit made in the viewer GUI; {@code false} when it could not be applied. */
    boolean applySnapshotFromGui(UUID viewerId, UUID targetId, InventoryService.Snapshot snapshot);
}
