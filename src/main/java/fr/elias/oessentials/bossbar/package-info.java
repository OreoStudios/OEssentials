/**
 * boss-bar feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.bossbar.lifecycle.BossBarModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "boss-bar")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "boss-bar", lifecycle = fr.elias.oessentials.bossbar.lifecycle.BossBarModule.class)
package fr.elias.oessentials.bossbar;
