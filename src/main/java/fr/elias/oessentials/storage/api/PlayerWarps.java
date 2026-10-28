package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.entity.Player;

import java.util.List;

/** Player warps, for menus outside storage. */
@ModuleApi("services")
public interface PlayerWarps {
    List<? extends PlayerWarpView> listAll();

    /** Whether the player may use a warp returned by {@link #listAll()}. */
    boolean canUse(Player player, PlayerWarpView warp);
}
