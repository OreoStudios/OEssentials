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
}
