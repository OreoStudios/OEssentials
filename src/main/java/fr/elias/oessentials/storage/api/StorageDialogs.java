package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.entity.Player;

/**
 * Paper dialogs for storage's commands. Each method returns {@code false} when no dialog was
 * shown (dialogs disabled or unsupported), and the command falls back to its chat output.
 *
 * <p>The dialogs module starts after storage, so it registers this through
 * {@link fr.elias.oessentials.storage.lifecycle.StorageServices#setDialogs}.</p>
 */
@ModuleApi("services")
public interface StorageDialogs {
    boolean openHomeDialog(Player player);
    boolean openWarpDialog(Player player);
    boolean confirmDelHome(Player player, String homeName);
    boolean confirmDelWarp(Player player, String warpName);
    boolean confirmPlayerWarpRemove(Player player, String warpName);
}
