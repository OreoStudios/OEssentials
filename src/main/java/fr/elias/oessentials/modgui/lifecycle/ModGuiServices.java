package fr.elias.oessentials.modgui.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface ModGuiServices {
    fr.elias.oessentials.modgui.internal.ModGuiService getModGuiService();
    /** Cross-server moderation actions, or {@code null} until RabbitMQ connects. */
    fr.elias.oessentials.modgui.internal.cross.ModBridge getModBridge();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ICrossServerAPI getCrossServerApi() {
        var s = getModBridge();
        return s == null ? null : new fr.elias.oessentials.modgui.internal.cross.CrossServerAPIImpl(s);
    }

    /** IP history of joining players. */
    fr.elias.oessentials.modgui.internal.ip.IpTracker getIpTracker();

    /** Chat mute / slow mode / staff chat state, or {@code null} while the GUI service is not running. */
    default fr.elias.oessentials.modgui.api.ChatControls getChatControls() {
        return getModGuiService();
    }

    /** Opens the gamerule editor for {@code world}; {@code false} when the GUI service is not running. */
    default boolean openWorldGamerules(org.bukkit.entity.Player viewer, org.bukkit.World world) {
        var svc = getModGuiService();
        if (svc == null) return false;
        var plugin = fr.elias.oessentials.OEssentials.get();
        fr.minuskube.inv.SmartInventory.builder()
                .manager(plugin.getInvManager())
                .provider(new fr.elias.oessentials.modgui.internal.menu.WorldGamerulesMenu(plugin, svc, world))
                .title(fr.elias.oessentials.shared.Lang.color("&8Gamerules: " + world.getName()))
                .size(6, 9)
                .build()
                .open(viewer);
        return true;
    }
}
