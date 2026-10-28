package fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.handlers;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerJoinPacket;
import fr.elias.oessentials.economy.internal.offline.OfflinePlayerCache;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;

import java.util.UUID;

public class PlayerJoinPacketHandler implements PacketSubscriber<PlayerJoinPacket> {

    private final OEssentials plugin;

    public PlayerJoinPacketHandler(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReceive(PacketChannel channel, PlayerJoinPacket packet) {
        if (packet == null) return;

        UUID uuid = packet.getPlayerId();
        String name = packet.getPlayerName();

        if (uuid == null || name == null || name.isEmpty()) {
            plugin.getLogger().warning("[Rabbit] PlayerJoinPacket missing data (uuid=" + uuid + ", name=" + name + ")");
            return;
        }

        plugin.getLogger().fine("[Rabbit] Join @" + channel + " -> " + name + " (" + uuid + ")");

        OfflinePlayerCache cache = plugin.getOfflinePlayerCache();
        if (cache != null) {
            try {
                cache.add(name, uuid);
            } catch (Throwable t) {
                plugin.getLogger().warning("[Rabbit] Failed to add player to OfflinePlayerCache: " + t.getMessage());
            }
        }
    }
}
