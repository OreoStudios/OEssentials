package fr.elias.oessentials.storage.internal.dialogs;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.ui.ConfirmDialogHandler;
import fr.elias.oessentials.platform.ui.Dialogs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/** Dialogs of storage's commands (moved from the dialogs module). */
public final class StorageDialogs {
    private StorageDialogs() {}

    public static boolean openHomes(Player player) {
        OEssentials plugin = OEssentials.get();
        var homes = plugin.getHomes();
        return homes != null && Dialogs.show(plugin, "homes", "Home dialog", () -> HomeDialogHandler.open(plugin, player, homes));
    }

    public static boolean openWarps(Player player) {
        OEssentials plugin = OEssentials.get();
        var warps = plugin.getWarpsApi();
        return warps != null && Dialogs.show(plugin, "warps", "Warp dialog", () -> WarpDialogHandler.open(plugin, player, warps));
    }

    /** Confirm runs /delhome <name> --confirmed. */
    public static boolean confirmDelHome(Player player, String homeName) {
        String safeHome = ConfirmDialogHandler.sanitizeCommandArg(homeName);
        return Dialogs.confirm(OEssentials.get(), player, "confirm-delhome", "DelHome confirm dialog",
                Component.text("Delete Home", NamedTextColor.RED),
                Component.text("Delete home \u201c" + homeName + "\u201d? This cannot be undone.", NamedTextColor.GRAY),
                "delhome " + safeHome + " --confirmed");
    }

    /** Confirm runs /delwarp <name> --confirmed. */
    public static boolean confirmDelWarp(Player player, String warpName) {
        String safeWarp = ConfirmDialogHandler.sanitizeCommandArg(warpName);
        return Dialogs.confirm(OEssentials.get(), player, "confirm-delwarp", "DelWarp confirm dialog",
                Component.text("Delete Warp", NamedTextColor.RED),
                Component.text("Delete warp \u201c" + warpName + "\u201d? This cannot be undone.", NamedTextColor.GRAY),
                "delwarp " + safeWarp + " --confirmed");
    }

    /** Confirm runs /pw remove <name> --confirmed. */
    public static boolean confirmPlayerWarpRemove(Player player, String warpName) {
        String safeWarp = ConfirmDialogHandler.sanitizeCommandArg(warpName);
        return Dialogs.confirm(OEssentials.get(), player, "confirm-pwremove", "PlayerWarp remove confirm",
                Component.text("Remove Player Warp", NamedTextColor.RED),
                Component.text("Remove your player warp \u201c" + warpName + "\u201d?", NamedTextColor.GRAY),
                "pw remove " + safeWarp + " --confirmed");
    }
}
