package fr.elias.oessentials.economy.internal.cross;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerJoinPacket;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerQuitPacket;
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

        if (plugin.getPacketBus() != null && plugin.getPacketBus().isInitialized()) {
            plugin.getPacketBus().sendPacket(new PlayerJoinPacket(playerUUID, playerName));
            if (isDebugEnabled()) {
                plugin.getLogger().info("[CROSS] Sent PlayerJoinPacket: " + playerName + " (" + playerUUID + ")");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (plugin.getPacketBus() != null && plugin.getPacketBus().isInitialized()) {
            plugin.getPacketBus().sendPacket(new PlayerQuitPacket(playerUUID));
            if (isDebugEnabled()) {
                plugin.getLogger().info("[CROSS] Sent PlayerQuitPacket: " + player.getName() + " (" + playerUUID + ")");
            }
        }
    }

    private boolean isDebugEnabled() {
        return plugin.getServerConfig().isDebugEnabled();
    }
}
