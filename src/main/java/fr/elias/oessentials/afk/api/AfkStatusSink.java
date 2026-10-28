package fr.elias.oessentials.afk.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/**
 * Receives AFK changes (e.g. the web panel dashboard). Registered through
 * {@link fr.elias.oessentials.afk.lifecycle.AfkServices#setStatusSink}.
 */
@ModuleApi("services")
public interface AfkStatusSink {
    void publishAfkStatus(UUID uuid, String playerName, String serverName,
                          String world, double x, double y, double z,
                          long afkSinceMs, boolean entering);
}
