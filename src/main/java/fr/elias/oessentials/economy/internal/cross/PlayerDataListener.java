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

        CompletableFuture.supplyAsync(() -> redisCache().getBalance(playerUUID)).thenAccept(cachedBalance -> {
            if (cachedBalance != null) {
                if (isDebugEnabled()) {
                    plugin.getLogger().info("[OEssentials] Loaded cached balance for " + playerName + ": $" + cachedBalance);
                }
                return;
            }

            CompletableFuture.supplyAsync(() -> plugin.getDatabase().getOrCreateBalance(playerUUID, playerName))
                    .thenAccept(balance -> {
                        redisCache().setBalance(playerUUID, balance);
                        if (isDebugEnabled()) {
                            plugin.getLogger().info("[OEssentials] Cached balance for " + playerName + ": $" + balance);
                        }
                    });
        });
    }

    private boolean isDebugEnabled() {
        return plugin.getServerConfig().isDebugEnabled();
    }

    private fr.elias.oessentials.redis.api.BalanceCache redisCache() {
        return plugin.getModuleRegistry().read(fr.elias.oessentials.redis.lifecycle.RedisServices.class,
                fr.elias.oessentials.redis.lifecycle.RedisServices::getRedis);
    }
}
