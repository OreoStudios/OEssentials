/**
 * kits feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.kits.lifecycle.KitsModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "kits")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "kits", lifecycle = fr.elias.oessentials.kits.lifecycle.KitsModule.class)
package fr.elias.oessentials.kits;
