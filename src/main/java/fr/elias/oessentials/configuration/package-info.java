/**
 * configuration feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code api} is the contract other modules may use. {@code lifecycle} exposes the
 * MinecraftModulith service interface and startup entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.configuration.lifecycle.ConfigurationModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "configuration")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "configuration", lifecycle = fr.elias.oessentials.configuration.lifecycle.ConfigurationModule.class)
package fr.elias.oessentials.configuration;
