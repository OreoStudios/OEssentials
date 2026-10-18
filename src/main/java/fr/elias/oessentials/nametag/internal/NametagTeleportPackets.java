package fr.elias.oessentials.nametag.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * PacketEvents-only teleport path, kept out of {@link PlayerNametagManager} so that class
 * still loads when PacketEvents is not installed.
 */
final class NametagTeleportPackets {
    private NametagTeleportPackets() {}

    static void send(Collection<UUID> viewers, int entityId, double x, double y, double z) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                entityId,
                new Vector3d(x, y, z),
                0f, 0f, false);
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
            }
        }
    }
}
