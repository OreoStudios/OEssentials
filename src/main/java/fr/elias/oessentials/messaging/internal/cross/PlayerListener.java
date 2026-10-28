package fr.elias.oessentials.messaging.internal.cross;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerJoinPacket;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerQuitPacket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final OEssentials plugin;

    public PlayerListener(OEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        String playerName = player.getName();

        plugin.getOfflinePlayerCache().add(playerName, playerUUID);

        if (playerName.startsWith(".") || playerName.startsWith("_")) {
            plugin.getLogger().warning("[CROSS] Bedrock player joined with prefixed name: " + playerName + " (" + playerUUID + ")");
        }

        if (plugin.getPacketManager() != null && plugin.getPacketManager().isInitialized()) {
            plugin.getPacketManager().sendPacket(new PlayerJoinPacket(playerUUID, playerName));
            if (isDebugEnabled()) {
                plugin.getLogger().info("[CROSS] Sent PlayerJoinPacket: " + playerName + " (" + playerUUID + ")");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.getPacketManager() != null && plugin.getPacketManager().isInitialized()) {
            plugin.getPacketManager().sendPacket(new PlayerQuitPacket(playerUUID));
            if (isDebugEnabled()) {
                plugin.getLogger().info("[CROSS] Sent PlayerQuitPacket: " + player.getName() + " (" + playerUUID + ")");
            }
        }
    }

    private boolean isDebugEnabled() {
        return plugin.getServerConfig().isDebugEnabled();
    }
}
