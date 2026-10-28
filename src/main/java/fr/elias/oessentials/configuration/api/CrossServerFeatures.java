package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;

/** The {@code features.cross-server.*} switches of {@code config/settings.yml}, as of the last load. */
@ModuleApi("services")
public interface CrossServerFeatures {
    boolean homes();
    boolean warps();
    boolean spawn();
    boolean economy();
    boolean enderchest();
}
