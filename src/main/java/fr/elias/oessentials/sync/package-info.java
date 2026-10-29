/**
 * inventory-sync feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.sync.lifecycle.InventorySyncModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "inventory-sync")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "inventory-sync", lifecycle = fr.elias.oessentials.sync.lifecycle.InventorySyncModule.class)
package fr.elias.oessentials.sync;
