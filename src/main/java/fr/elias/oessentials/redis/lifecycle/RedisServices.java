package fr.elias.oessentials.redis.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.redis.internal.persistence.database.RedisManager;

@ModuleApi("services")
public interface RedisServices {
    /** The balance cache, or {@code null} when Redis is disabled. */
    fr.elias.oessentials.redis.api.BalanceCache getRedis();
}
