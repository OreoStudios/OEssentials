package fr.elias.oessentials.storage.internal.homes;

import fr.elias.oessentials.messaging.api.PacketBus;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.storage.api.HomeLocation;
import fr.elias.oessentials.storage.api.Homes;
import fr.elias.oessentials.storage.internal.homes.home.HomeService;
import fr.elias.oessentials.storage.internal.homes.rabbit.packet.OtherHomeTeleportRequestPacket;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Publishes {@link HomeService} as the {@link Homes} contract. */
public final class HomesAdapter implements Homes {
    private final HomeService homes;
    private final Supplier<PacketBus> packets;

    public HomesAdapter(HomeService homes, Supplier<PacketBus> packets) {
        this.homes = homes;
        this.packets = packets;
    }

    @Override public boolean setHome(Player player, String name, Location location) { return homes.setHome(player, name, location); }
    @Override public boolean delHome(UUID owner, String name) { return homes.delHome(owner, name); }
    @Override public Location getHome(UUID owner, String name) { return homes.getHome(owner, name); }
    @Override public Set<String> homes(UUID owner) { return homes.homes(owner); }
    @Override public Set<String> allHomeNames(UUID owner) { return homes.allHomeNames(owner); }
    @Override public Map<String, ? extends HomeLocation> listHomes(UUID owner) { return homes.listHomes(owner); }
    @Override public String homeServer(UUID owner, String name) { return homes.homeServer(owner, name); }
    @Override public Map<String, String> homeServers(UUID owner) { return homes.homeServers(owner); }
    @Override public String localServer() { return homes.localServer(); }

    @Override
    public boolean requestRemoteHomeTeleport(UUID requester, UUID owner, String homeName, String targetServer) {
        PacketBus bus = packets.get();
        if (bus == null || !bus.isInitialized()) return false;
        String requestId = UUID.randomUUID().toString();
        bus.sendPacket(PacketChannel.individual(targetServer),
                new OtherHomeTeleportRequestPacket(requester, owner, homeName, targetServer, requestId));
        return true;
    }
}
