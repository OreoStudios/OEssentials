package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.api.IAutoRebootAPI;

import java.util.UUID;

/** Scheduled restarts configured under {@code auto-reboot} in {@code config/settings.yml}. */
@ModuleApi("services")
public interface AutoReboot extends IAutoRebootAPI {
    /**
     * A server that is rebooting sent a player here and asked for them to be placed in a
     * WorldGuard region. Ignored unless {@code targetServer} is this server; applied on join
     * when the player has not arrived yet.
     */
    void acceptSafeZoneEnter(UUID playerId, String targetServer, String worldName, String regionName);
}
