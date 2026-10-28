package fr.elias.oessentials.mute.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.mute.api.MuteService;

@ModuleApi("services")
public interface MuteSystemServices {
    MuteService getMuteService();
}
