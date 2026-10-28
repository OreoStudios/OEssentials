package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * How auto-reboot moves players to the safe-zone server.
 *
 * <p>Messaging starts after configuration, so it registers this through
 * {@link fr.elias.oessentials.configuration.lifecycle.ConfigurationServices#setSafeZoneRelay}
 * instead of configuration depending on messaging.</p>
 */
@ModuleApi("services")
public interface SafeZoneRelay {
    /** Whether {@link #requestSafeZone} can reach other servers right now. */
    boolean packetsReady();

    /** Asks {@code targetServer} to place the player in the region when they arrive. */
    void requestSafeZone(UUID playerId, String targetServer, String worldName, String regionName);

    /** Sends the player to another server through the proxy. */
    void connect(Player player, String targetServer);
}
