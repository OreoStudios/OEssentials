package fr.elias.oessentials.chat.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.chat.internal.ChatSyncManager;
import fr.elias.oessentials.chat.internal.CustomConfig;
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
}
