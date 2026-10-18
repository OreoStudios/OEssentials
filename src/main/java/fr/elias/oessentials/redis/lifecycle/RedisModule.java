package fr.elias.oessentials.redis.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.redis.internal.persistence.database.RedisManager;

@PluginModule(value = "redis", dependencies = {"configuration::services"}, after = {"storage"})
public final class RedisModule extends ManagedModule implements RedisServices {
    private RedisManager redis;

    @Override
    protected void start() {
        cleanup("redis", () -> { if (redis != null) redis.shutdown(); });
        initRedis();
    }

    @Override public RedisManager getRedis() { return redis; }

    private void initRedis() {
        if (services(ConfigurationServices.class).getRedisEnabled()) {
            this.redis = new RedisManager(
                    plugin.getConfig().getString("redis.host", "localhost"),
                    plugin.getConfig().getInt("redis.port", 6379),
                    plugin.getConfig().getString("redis.password", "")
            );
            if (!redis.connect()) {
                plugin.getLogger().warning("[REDIS] Enabled but failed to connect. Continuing without cache.");
            } else {
                plugin.getLogger().info("[REDIS] Connected.");
            }
        } else {
            this.redis = new RedisManager("", 6379, "");
            plugin.getLogger().info("[REDIS] Disabled.");
        }
    }
}
