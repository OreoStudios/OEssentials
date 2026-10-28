/**
 * mobs feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.mobs.lifecycle.MobsModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "mobs")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "mobs", lifecycle = fr.elias.oessentials.mobs.lifecycle.MobsModule.class)
package fr.elias.oessentials.mobs;
