package fr.elias.oessentials.modgui.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Network chat state set from the moderation GUI: global mute, slow mode and staff chat. */
@ModuleApi("services")
public interface ChatControls {
    boolean chatMuted();
    void setChatMuted(boolean muted);
    int getSlowmodeSeconds();
    void setSlowmodeSeconds(int seconds);
    boolean isStaffChatEnabled(UUID playerId);
    boolean canSendMessage(UUID playerId);
    long getRemainingSlowmode(UUID playerId);
    void recordMessage(UUID playerId);
}
