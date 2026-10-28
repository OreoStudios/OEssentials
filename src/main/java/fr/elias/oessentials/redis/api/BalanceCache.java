package fr.elias.oessentials.redis.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Redis cache of economy balances. */
@ModuleApi("services")
public interface BalanceCache {
    /** {@code null} when not cached. */
    Double getBalance(UUID playerUUID);

    void setBalance(UUID playerUUID, double balance);

    void deleteBalance(UUID playerUUID);

    void clearCache();
}
