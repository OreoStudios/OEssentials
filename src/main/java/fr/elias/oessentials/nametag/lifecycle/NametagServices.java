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
}
