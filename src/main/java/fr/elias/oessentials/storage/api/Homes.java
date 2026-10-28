package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Player homes across the network. */
@ModuleApi("services")
public interface Homes {
    boolean setHome(Player player, String name, Location location);

    boolean delHome(UUID owner, String name);

    /** The home on this server, or {@code null}. */
    Location getHome(UUID owner, String name);

    Set<String> homes(UUID owner);

    /** Every home of the owner on any server. */
    Set<String> allHomeNames(UUID owner);

    Map<String, ? extends HomeLocation> listHomes(UUID owner);

    String homeServer(UUID owner, String name);

    Map<String, String> homeServers(UUID owner);

    String localServer();

    /**
     * Asks {@code targetServer} to teleport {@code requester} to another player's home when they
     * arrive. Returns {@code false} when cross-server messaging is unavailable.
     */
    boolean requestRemoteHomeTeleport(UUID requester, UUID owner, String homeName, String targetServer);
}
