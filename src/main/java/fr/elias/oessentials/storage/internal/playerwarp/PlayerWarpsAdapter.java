package fr.elias.oessentials.storage.internal.playerwarp;

import fr.elias.oessentials.storage.api.PlayerWarpView;
import fr.elias.oessentials.storage.api.PlayerWarps;
import org.bukkit.entity.Player;

import java.util.List;

/** Publishes {@link PlayerWarpService} as the {@link PlayerWarps} contract. */
public final class PlayerWarpsAdapter implements PlayerWarps {
    private final PlayerWarpService warps;

    public PlayerWarpsAdapter(PlayerWarpService warps) {
        this.warps = warps;
    }

    @Override public List<? extends PlayerWarpView> listAll() { return warps.listAll(); }

    @Override
    public boolean canUse(Player player, PlayerWarpView warp) {
        return warp instanceof PlayerWarp stored && warps.canUse(player, stored);
    }
}
