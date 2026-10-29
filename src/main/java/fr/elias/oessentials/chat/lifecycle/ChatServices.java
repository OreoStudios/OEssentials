package fr.elias.oessentials.chat.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.chat.internal.ChatSyncManager;
import fr.elias.oessentials.platform.config.CustomConfig;
import fr.elias.oessentials.chat.internal.FormatManager;

@ModuleApi("services")
public interface ChatServices {
    fr.elias.oessentials.chat.internal.channels.ChatChannelManager getChannelManager();
    CustomConfig getChatConfig();
    FormatManager getChatFormatManager();
    ChatSyncManager getChatSyncManager();
    org.bukkit.event.Listener getActiveChatListener();
    org.bukkit.event.Listener getActiveJoinMessagesListener();
    org.bukkit.event.Listener getActiveQuitMessagesListener();
    org.bukkit.configuration.file.FileConfiguration getJoinQuitCfg();
    fr.elias.oessentials.chat.internal.oreobot.ConversationListener getActiveConversationListener();
    void reloadChat();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IChatAPI getChatApi() {
        var s = getChatSyncManager();
        return s == null ? null : new fr.elias.oessentials.chat.internal.api.ChatAPIImpl(s);
    }

    /**
     * Sends a chat control (CLEAR_CHAT, GLOBAL_MUTE, SLOWMODE) to the other servers.
     * Does nothing while chat sync is not set up.
     */
    default void broadcastChatControl(String type, String value, String actor) {
        var sync = getChatSyncManager();
        if (sync != null) sync.broadcastChatControl(type, value, actor);
    }
}
