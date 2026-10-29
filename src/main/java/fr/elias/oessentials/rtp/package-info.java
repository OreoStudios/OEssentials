/**
 * rtp feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.rtp.lifecycle.RtpModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "rtp")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "rtp", lifecycle = fr.elias.oessentials.rtp.lifecycle.RtpModule.class)
package fr.elias.oessentials.rtp;
