package fr.elias.oessentials.nametag.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.nametag.internal.PlayerNametagManager;

@ModuleApi("services")
public interface NametagServices {
    PlayerNametagManager getNametagManager();
    fr.elias.oessentials.nametag.internal.ChatBubbleService getChatBubbleService();
    fr.elias.oessentials.nametag.internal.ActionBarService getActionBarService();
    fr.elias.oessentials.nametag.internal.MultiBossBarService getMultiBossBarService();
    fr.elias.oessentials.nametag.internal.CustomNameplatesConfig getCustomNameplatesConfig();
    fr.elias.oessentials.nametag.internal.NametageToggleStore getNametagToggleStore();
    void reloadCustomNameplates();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.INametagAPI getNametagApi() {
        var s = getNametagManager();
        return s == null ? null : new fr.elias.oessentials.nametag.internal.api.NametagAPIImpl(s);
    }

    /** Re-evaluates who can see {@code owner}'s nametag (e.g. after a vanish toggle). */
    default void refreshOwnerVisibility(org.bukkit.entity.Player owner) {
        var manager = getNametagManager();
        if (manager != null) manager.refreshOwnerVisibility(owner);
    }

    /** Marks an offline owner's nametag for re-evaluation on the next sweep. */
    default void markOwnerDirty(java.util.UUID ownerId) {
        var manager = getNametagManager();
        if (manager != null) manager.markOwnerDirty(ownerId);
    }

    /** Rebuilds {@code player}'s nametag now. */
    default void forceUpdate(org.bukkit.entity.Player player) {
        var manager = getNametagManager();
        if (manager != null) manager.forceUpdate(player);
    }
}
