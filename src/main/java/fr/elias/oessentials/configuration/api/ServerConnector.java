package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.entity.Player;

/**
 * Sends a player to another server through the proxy.
 *
 * <p>Registered by proxy-messaging through
 * {@link fr.elias.oessentials.configuration.lifecycle.ConfigurationServices#setServerConnector}.</p>
 */
@ModuleApi("services")
public interface ServerConnector {
    void connect(Player player, String targetServer);
}
