package fr.elias.oessentials.modgui.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface ModGuiServices {
    fr.elias.oessentials.modgui.internal.ModGuiService getModGuiService();
    /** Cross-server moderation actions, or {@code null} until RabbitMQ connects. */
    fr.elias.oessentials.modgui.internal.cross.ModBridge getModBridge();
}
