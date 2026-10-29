package fr.elias.oessentials.platform.ui;

import fr.elias.oessentials.OEssentials;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/** Paper dialogs (1.21.5+), shown when enabled for a feature in settings.yml. */
public final class Dialogs {
    private static volatile Boolean apiAvailable;

    private Dialogs() {}

    public static boolean apiAvailable() {
        Boolean available = apiAvailable;
        if (available == null) {
            try {
                Class.forName("io.papermc.paper.dialog.Dialog");
                available = true;
            } catch (ClassNotFoundException e) {
                available = false;
            }
            apiAvailable = available;
        }
        return available;
    }

    public static boolean enabledFor(OEssentials plugin, String feature) {
        var settings = plugin.getFeatureSettings();
        return apiAvailable() && settings != null && settings.isDialogEnabledFor(feature);
    }

    /**
     * Runs {@code open} when dialogs are enabled for {@code feature}. Returns {@code false} when they
     * are not, or when opening fails (logged), so the caller can fall back to its menu or chat output.
     */
    public static boolean show(OEssentials plugin, String feature, String label, Runnable open) {
        if (!enabledFor(plugin, feature)) return false;
        try {
            open.run();
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[Dialog] " + label + " failed: " + t.getMessage());
            return false;
        }
    }

    /** A confirm dialog whose confirm button runs {@code command} as the player. */
    public static boolean confirm(OEssentials plugin, Player player, String feature, String label,
                                  Component title, Component body, String command) {
        return show(plugin, feature, label, () -> ConfirmDialogHandler.open(plugin, player, title, body, command));
    }
}
