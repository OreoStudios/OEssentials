package fr.elias.oessentials.economy.internal.cross;

import fr.elias.oessentials.OEssentials;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataListener implements Listener {
    private final OEssentials plugin;

    public PlayerDataListener(OEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID playerUUID = event.getUniqueId();
        String playerName = event.getName();

        CompletableFuture.supplyAsync(() -> plugin.getRedis().getBalance(playerUUID)).thenAccept(cachedBalance -> {
            if (cachedBalance != null) {
                if (isDebugEnabled()) {
                    plugin.getLogger().info("[OEssentials] Loaded cached balance for " + playerName + ": $" + cachedBalance);
                }
                return;
            }

            CompletableFuture.supplyAsync(() -> plugin.getDatabase().getOrCreateBalance(playerUUID, playerName))
                    .thenAccept(balance -> {
                        plugin.getRedis().setBalance(playerUUID, balance);
                        if (isDebugEnabled()) {
                            plugin.getLogger().info("[OEssentials] Cached balance for " + playerName + ": $" + balance);
                        }
                    });
        });
    }

    private boolean isDebugEnabled() {
        return plugin.getServerConfig().isDebugEnabled();
    }
}
