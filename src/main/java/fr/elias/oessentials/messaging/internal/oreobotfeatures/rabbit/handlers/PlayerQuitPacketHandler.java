package fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.handlers;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerQuitPacket;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;

import java.util.UUID;

public class PlayerQuitPacketHandler implements PacketSubscriber<PlayerQuitPacket> {

    private final OEssentials plugin;

    public PlayerQuitPacketHandler(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReceive(PacketChannel channel, PlayerQuitPacket packet) {
        UUID playerId = packet.getPlayerId();

        if (playerId == null) {
            plugin.getLogger().warning("[OEssentials] Received PlayerQuitPacket with null UUID; skipping removal.");
            return;
        }

        if (plugin.getOfflinePlayerCache().contains(playerId)) {
            plugin.getOfflinePlayerCache().remove(playerId);
            if (isDebugEnabled()) {
                plugin.getLogger().info("[CROSS] Received PlayerQuitPacket: " + playerId + " (removed from cache)");
            }
        } else if (isDebugEnabled()) {
            plugin.getLogger().info("[CROSS] Received PlayerQuitPacket for unknown UUID: " + playerId + " (not in cache)");
        }
    }

    private boolean isDebugEnabled() {
        return plugin.getServerConfig().isDebugEnabled();
    }
}
