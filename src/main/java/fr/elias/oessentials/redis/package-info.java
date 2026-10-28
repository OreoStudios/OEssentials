/**
 * redis feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.redis.lifecycle.RedisModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "redis")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "redis", lifecycle = fr.elias.oessentials.redis.lifecycle.RedisModule.class)
package fr.elias.oessentials.redis;
