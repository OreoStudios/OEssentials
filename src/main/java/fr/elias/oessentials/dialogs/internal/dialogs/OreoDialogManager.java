package fr.elias.oessentials.dialogs.internal.dialogs;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.ui.Dialogs;

/**
 * Whether Paper dialogs are available and enabled. Feature dialogs (homes, warps, kits, TPA,
 * channels, near, confirmations) now live in their features and use {@link Dialogs}.
 */
public final class OreoDialogManager {
    private final OEssentials plugin;

    public OreoDialogManager(OEssentials plugin) {
        this.plugin = plugin;
        if (Dialogs.apiAvailable()) plugin.getLogger().info("[Dialog] Paper Dialog API detected — dialog support enabled.");
        else plugin.getLogger().warning("[Dialog] Paper Dialog API not found (requires Paper 1.21.5+). Dialog UI disabled.");
    }

    public boolean isAvailable() { return Dialogs.apiAvailable(); }

    public boolean isEnabled() {
        var settings = plugin.getFeatureSettings();
        return Dialogs.apiAvailable() && settings != null && settings.isDialogEnabled();
    }

    public boolean isEnabledFor(String feature) {
        return Dialogs.enabledFor(plugin, feature);
    }
}
