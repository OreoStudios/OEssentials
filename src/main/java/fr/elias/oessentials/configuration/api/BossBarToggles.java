package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;

/**
 * The nametag module's multi-bossbars, as switched from the {@code /oesettings} menu.
 *
 * <p>Nametag starts after configuration, so it registers this through
 * {@link fr.elias.oessentials.configuration.lifecycle.ConfigurationServices#setBossBarToggles}.</p>
 */
@ModuleApi("services")
public interface BossBarToggles {
    boolean isBarEnabled(String barId);

    /** Flips {@code bossbars.bars.<barId>.enabled}, saves it and applies it immediately. */
    void toggleBar(String barId);
}
