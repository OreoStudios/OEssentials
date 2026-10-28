/**
 * messaging feature: lifecycle wiring and the implementation it owns.
 *
 * <p>{@code lifecycle} exposes the MinecraftModulith service contract and startup
 * entry point. {@code internal} contains gameplay, commands, listeners and persistence
 * belonging to this feature. Existing cross-feature implementation references are
 * tracked separately from lifecycle dependencies; these packages are not hot-unloadable.
 *
 * @see fr.elias.oessentials.messaging.lifecycle.MessagingModule
 */
@dev.oreo.modulith.core.ApplicationModule(id = "messaging")
@fr.elias.oessentials.platform.modularity.FeaturePackage(
        id = "messaging", lifecycle = fr.elias.oessentials.messaging.lifecycle.MessagingModule.class)
package fr.elias.oessentials.messaging;
