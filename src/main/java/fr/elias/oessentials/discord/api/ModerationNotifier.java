package fr.elias.oessentials.discord.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Announces moderation actions to Discord. Each call is a no-op while the integration is disabled. */
@ModuleApi("services")
public interface ModerationNotifier {
    boolean isEnabled();
    void notifyKick(String targetName, UUID targetId, String reason, String by);
    void notifyBan(String targetName, UUID targetId, String reason, String by, Long untilEpochMillis);
    void notifyUnban(String targetName, UUID targetId, String by);
    void notifyMute(String targetName, UUID targetId, String reason, String by, Long untilEpochMillis);
    void notifyUnmute(String targetName, UUID targetId, String by);
    void notifyJail(String targetName, UUID targetId, String jailName, String cellId, String reason, String by, Long untilEpochMillis);
    void notifyUnjail(String targetName, UUID targetId, String by);
}
