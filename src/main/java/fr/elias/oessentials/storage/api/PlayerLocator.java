package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.Collection;
import java.util.UUID;

/**
 * The network-wide player directory: names, UUIDs and which server a player is on.
 * Only available with MongoDB storage; {@code null} otherwise.
 */
@ModuleApi("services")
public interface PlayerLocator {
    UUID lookupUuidByName(String name);

    String lookupNameByUuid(UUID uuid);

    /** The server the player is on now, or {@code null} when offline everywhere. */
    String lookupCurrentServer(UUID uuid);

    /** The server the player is on, or the last one they were seen on. */
    String getCurrentOrLastServer(UUID uuid);

    /** Online player names on the network starting with {@code prefix}, for tab completion. */
    Collection<String> suggestOnlineNames(String prefix, int limit);

    Collection<UUID> onlinePlayers();

    void saveMapping(UUID uuid, String name);
}
