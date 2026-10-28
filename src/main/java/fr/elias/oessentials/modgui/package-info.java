/**
 * mod-gui feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.modgui.lifecycle.ModGuiModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "mod-gui")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "mod-gui", lifecycle = fr.elias.oessentials.modgui.lifecycle.ModGuiModule.class)
package fr.elias.oessentials.modgui;
