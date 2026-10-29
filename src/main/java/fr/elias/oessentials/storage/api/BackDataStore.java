package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.Map;
import java.util.UUID;

/** Persists each player's /back location. */
@ModuleApi("services")
public interface BackDataStore {
    Map<String, Object> getBackData(UUID uuid);

    /** {@code null} clears it. */
    void setBackData(UUID uuid, Map<String, Object> data);
}
