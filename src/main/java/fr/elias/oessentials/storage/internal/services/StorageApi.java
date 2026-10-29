package fr.elias.oessentials.storage.internal.services;

import fr.elias.oessentials.storage.internal.homes.home.HomeService;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface StorageApi extends fr.elias.oessentials.storage.api.BackDataStore {


    void setSpawn(String server, Location loc);
    Location getSpawn(String server);

    default void setSpawn(Location loc) {
        setSpawn(Bukkit.getServer().getName(), loc);
    }

    default Location getSpawn() {
        return getSpawn(Bukkit.getServer().getName());
    }


    void setWarp(String name, Location loc);
    boolean delWarp(String name);
    Location getWarp(String name);
    Set<String> listWarps();



    boolean setHome(UUID uuid, String name, Location loc);
    boolean delHome(UUID uuid, String name);
    Location getHome(UUID uuid, String name);
    Set<String> homes(UUID uuid);
    Map<String, HomeService.StoredHome> listHomes(UUID owner);


    void setBackData(UUID uuid, Map<String, Object> data);
    Map<String, Object> getBackData(UUID uuid);

    default void setLast(UUID uuid, Location loc) {
        if (loc == null) {
            setBackData(uuid, null);
            return;
        }

        String server = Bukkit.getServer().getName();

        Map<String, Object> data = Map.of(
                "server", server,
                "world", loc.getWorld() != null ? loc.getWorld().getName() : "",
                "x", loc.getX(),
                "y", loc.getY(),
                "z", loc.getZ(),
                "yaw", loc.getYaw(),
                "pitch", loc.getPitch()
        );
        setBackData(uuid, data);
    }

    default Location getLast(UUID uuid) {
        Map<String, Object> map = getBackData(uuid);
        if (map == null) return null;

        // Same layout as setLast() and player-services' BackLocation.toMap().
        if (!(map.get("world") instanceof String worldName)
                || !(map.get("x") instanceof Number x) || !(map.get("y") instanceof Number y) || !(map.get("z") instanceof Number z)
                || !(map.get("yaw") instanceof Number yaw) || !(map.get("pitch") instanceof Number pitch)) {
            return null;
        }

        String current = Bukkit.getServer().getName();
        String lastServer = map.get("server") instanceof String server ? server : null;
        if (lastServer == null || !lastServer.equalsIgnoreCase(current)) {
            return null;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x.doubleValue(), y.doubleValue(), z.doubleValue(), yaw.floatValue(), pitch.floatValue());
    }

    void flush();
    void close();
}
