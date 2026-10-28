package fr.elias.oessentials.discord.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface DiscordIntegrationServices {
    fr.elias.oessentials.discord.internal.DiscordModerationNotifier getDiscordMod();
    fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager getDiscordLinkManager();

    /** Moderation announcements, or {@code null} while the Discord integration is not running. */
    default fr.elias.oessentials.discord.api.ModerationNotifier getModerationNotifier() {
        return getDiscordMod();
    }
}
