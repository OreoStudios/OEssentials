package fr.elias.oessentials.freeze.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.api.IFreezeAPI;
import fr.elias.oessentials.freeze.internal.freeze.FreezeManager;
import fr.elias.oessentials.freeze.internal.state.FreezeService;

@ModuleApi("services")
public interface FreezeServices {
    FreezeManager getFreezeManager();

    /** Released facade type; other modules use {@link #getFreezeApi()}. */
    FreezeService getFreezeService();

    /** Who is frozen, as exposed by the public OEssentials API. */
    IFreezeAPI getFreezeApi();

    default boolean isFrozen(java.util.UUID playerId) {
        var manager = getFreezeManager();
        return manager != null && manager.isFrozen(playerId);
    }

    /** Freezes the player for {@code seconds} (0 = until unfrozen). */
    default void freeze(org.bukkit.entity.Player target, long seconds) {
        var manager = getFreezeManager();
        if (manager != null) manager.freeze(target, null, seconds);
    }

    default void unfreeze(org.bukkit.entity.Player target) {
        var manager = getFreezeManager();
        if (manager != null) manager.unfreeze(target);
    }
}
