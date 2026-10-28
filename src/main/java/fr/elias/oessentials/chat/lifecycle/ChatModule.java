package fr.elias.oessentials.chat.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.mute.lifecycle.MuteSystemServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.chat.internal.ChatSyncManager;
import fr.elias.oessentials.platform.config.CustomConfig;
import fr.elias.oessentials.chat.internal.FormatManager;
import fr.elias.oessentials.chat.internal.oreobot.ConversationListener;
import org.bukkit.event.Listener;

@PluginModule(value = "chat", dependencies = {"configuration::services", "mute-system::services", "storage::services"}, uses = {"discord-integration::services", "inventories::services", "mod-gui::services", "notes::services"}, after = {"shop"})
public final class ChatModule extends ManagedModule implements ChatServices {
    private fr.elias.oessentials.chat.internal.channels.ChatChannelManager channelManager;
    private CustomConfig chatConfig;
    private FormatManager chatFormatManager;
    private ChatSyncManager chatSyncManager;
    private org.bukkit.event.Listener activeChatListener;
    private org.bukkit.event.Listener activeJoinMessagesListener;
    private org.bukkit.event.Listener activeQuitMessagesListener;
    private org.bukkit.configuration.file.FileConfiguration joinQuitCfg;
    private fr.elias.oessentials.chat.internal.oreobot.ConversationListener activeConversationListener;
    private fr.elias.oessentials.chat.internal.tasks.AutoMessageScheduler autoMessageScheduler;

    @Override
    protected void start() {
        cleanup("chatSyncManager", () -> { if (chatSyncManager != null) chatSyncManager.close(); });
        cleanup("channelManager", () -> { if (channelManager != null) channelManager.savePlayerData(); });
        initChat();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.IChatAPI.class, this::getChatApi);
        registerReloadStep(3, "chat", this::reloadChat);
        registerConfigFile("chat-format.yml", plugin::reloadChat);
        registerConfigFile("join-quit-messages.yml", plugin::reloadChat);
    }

    @Override public fr.elias.oessentials.chat.internal.channels.ChatChannelManager getChannelManager() { return channelManager; }
    @Override public CustomConfig getChatConfig() { return chatConfig; }
    @Override public FormatManager getChatFormatManager() { return chatFormatManager; }
    @Override public ChatSyncManager getChatSyncManager() { return chatSyncManager; }
    @Override public org.bukkit.event.Listener getActiveChatListener() { return activeChatListener; }
    @Override public org.bukkit.event.Listener getActiveJoinMessagesListener() { return activeJoinMessagesListener; }
    @Override public org.bukkit.event.Listener getActiveQuitMessagesListener() { return activeQuitMessagesListener; }
    @Override public org.bukkit.configuration.file.FileConfiguration getJoinQuitCfg() { return joinQuitCfg; }
    @Override public fr.elias.oessentials.chat.internal.oreobot.ConversationListener getActiveConversationListener() { return activeConversationListener; }

    private void initChat() {
        this.chatConfig       = new fr.elias.oessentials.platform.config.CustomConfig(plugin, "config/chat-messaging/chat-format.yml");
        this.chatFormatManager = new fr.elias.oessentials.chat.internal.FormatManager(chatConfig);

        boolean discordEnabled = false;
        String discordWebhookUrl = "";
        try {
            if (plugin.getFeatureSettings().chatDiscordBridgeEnabled()) {
                var chatRoot = chatConfig.getCustomConfig().getConfigurationSection("chat.discord");
                if (chatRoot != null) {
                    discordEnabled    = chatRoot.getBoolean("enabled", false);
                    discordWebhookUrl = chatRoot.getString("webhook_url", "");
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Chat] Failed reading Discord config: " + t.getMessage());
        }

        this.channelManager = new fr.elias.oessentials.chat.internal.channels.ChatChannelManager(plugin, this.chatConfig, services(StorageServices.class).getHomesMongoClient());
        this.channelManager.reload();

        boolean chatSyncEnabled = chatConfig.getCustomConfig().getBoolean("MongoDB_rabbitmq.enabled", false);
        String chatRabbitUri    = chatConfig.getCustomConfig().getString("MongoDB_rabbitmq.rabbitmq.uri", "");
        try {
            this.chatSyncManager = new ChatSyncManager(chatSyncEnabled, chatRabbitUri, services(MuteSystemServices.class).getMuteService(), channelManager);
            if (chatSyncEnabled) this.chatSyncManager.subscribeMessages();
            plugin.getLogger().info("[CHAT] ChatSync enabled=" + chatSyncEnabled);
        } catch (Exception e) {
            plugin.getLogger().severe("[CHAT] ChatSync init failed: " + e.getMessage());
            this.chatSyncManager = new ChatSyncManager(false, "", services(MuteSystemServices.class).getMuteService(), null);
        }

        Listener chatListener;
        if (channelManager != null && channelManager.isEnabled()) {
            chatListener = new fr.elias.oessentials.chat.internal.AsyncChatListenerWithChannels(plugin, chatFormatManager, chatConfig, chatSyncManager, discordEnabled, discordWebhookUrl, services(MuteSystemServices.class).getMuteService(), channelManager);
            plugin.getLogger().info("[Chat] Initialized with channel support (discord=" + discordEnabled + ")");
        } else {
            chatListener = new fr.elias.oessentials.chat.internal.AsyncChatListener(chatFormatManager, chatConfig, chatSyncManager, discordEnabled, discordWebhookUrl, services(MuteSystemServices.class).getMuteService());
            plugin.getLogger().info("[Chat] Initialized without channels (discord=" + discordEnabled + ")");
        }
        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
        this.activeChatListener = chatListener;

        if (channelManager.isEnabled() && plugin.getFeatureSettings().chatEnabled()) {
            var channelsCmd  = new fr.elias.oessentials.chat.internal.channels.commands.OeChannelsCommand(plugin, channelManager);
            var channelCmd   = new fr.elias.oessentials.chat.internal.channels.commands.OeChannelCommand(plugin, channelManager);
            services(ConfigurationServices.class).getCommands().register(channelsCmd);
            services(ConfigurationServices.class).getCommands().register(channelCmd);
            services(ConfigurationServices.class).getCommands().rewireTab("oechannel", channelCmd);
            services(ConfigurationServices.class).getCommands().rewireTab("channel",   channelCmd);
            plugin.getLogger().info("[Channels] Enabled with " + channelManager.getAllChannels().size() + " channels");

            var announceCmd = new fr.elias.oessentials.chat.internal.channels.commands.ChannelAnnounceCommand(plugin, channelManager, chatSyncManager);
            services(ConfigurationServices.class).getCommands().registerLegacy("channelannounce", announceCmd, announceCmd);
            plugin.getLogger().info("[Channels] Announcement command registered.");
        } else {
            plugin.getLogger().info("[Channels] Disabled by config.");
        }

        this.joinQuitCfg = loadJoinQuitConfig();

        if (activeConversationListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(activeConversationListener);
        }
        activeConversationListener = new ConversationListener(plugin, joinQuitCfg);
        plugin.getServer().getPluginManager().registerEvents(activeConversationListener, plugin);

        if (activeJoinMessagesListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(activeJoinMessagesListener);
        }
        activeJoinMessagesListener = new fr.elias.oessentials.chat.internal.oreobot.JoinMessagesListener(plugin, joinQuitCfg);
        plugin.getServer().getPluginManager().registerEvents(activeJoinMessagesListener, plugin);

        if (activeQuitMessagesListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(activeQuitMessagesListener);
        }
        activeQuitMessagesListener = new fr.elias.oessentials.chat.internal.oreobot.QuitMessagesListener(plugin, joinQuitCfg);
        plugin.getServer().getPluginManager().registerEvents(activeQuitMessagesListener, plugin);
        startAutoMessages();
    }

    private void startAutoMessages() {
        if (autoMessageScheduler != null) autoMessageScheduler.stop();
        autoMessageScheduler = new fr.elias.oessentials.chat.internal.tasks.AutoMessageScheduler(plugin, joinQuitCfg);
        autoMessageScheduler.start();
    }

    private org.bukkit.configuration.file.FileConfiguration loadJoinQuitConfig() {
        java.io.File f = new java.io.File(plugin.getDataFolder(), "config/chat-messaging/join-quit-messages.yml");
        if (!f.exists()) plugin.saveResource("config/chat-messaging/join-quit-messages.yml", false);
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
    }

    public void reloadChat() {
        // Unregister old chat listener
        if (activeChatListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(activeChatListener);
            activeChatListener = null;
        }

        // Reload chat-format.yml + channels (channelManager.reload() calls chatConfig.reloadCustomConfig() internally)
        channelManager.reload();

        // Re-read discord settings from freshly reloaded config
        boolean discordEnabled = false;
        String discordWebhookUrl = "";
        try {
            if (plugin.getFeatureSettings().chatDiscordBridgeEnabled()) {
                var chatRoot = chatConfig.getCustomConfig().getConfigurationSection("chat.discord");
                if (chatRoot != null) {
                    discordEnabled    = chatRoot.getBoolean("enabled", false);
                    discordWebhookUrl = chatRoot.getString("webhook_url", "");
                }
            }
        } catch (Throwable ignored) {}

        // Register the correct listener based on new channel state
        org.bukkit.event.Listener newListener;
        if (channelManager != null && channelManager.isEnabled()) {
            newListener = new fr.elias.oessentials.chat.internal.AsyncChatListenerWithChannels(
                    plugin, chatFormatManager, chatConfig, chatSyncManager,
                    discordEnabled, discordWebhookUrl, services(MuteSystemServices.class).getMuteService(), channelManager);
            plugin.getLogger().info("[Chat] Reloaded with channel support (channels=true)");
        } else {
            newListener = new fr.elias.oessentials.chat.internal.AsyncChatListener(
                    chatFormatManager, chatConfig, chatSyncManager,
                    discordEnabled, discordWebhookUrl, services(MuteSystemServices.class).getMuteService());
            plugin.getLogger().info("[Chat] Reloaded without channels (channels=false)");
        }
        plugin.getServer().getPluginManager().registerEvents(newListener, plugin);
        this.activeChatListener = newListener;

        // Reload join/quit/automessage/conversation config
        this.joinQuitCfg = loadJoinQuitConfig();
        if (activeJoinMessagesListener instanceof fr.elias.oessentials.chat.internal.oreobot.JoinMessagesListener jml) {
            jml.setChatMessagingCfg(joinQuitCfg);
        }
        if (activeQuitMessagesListener instanceof fr.elias.oessentials.chat.internal.oreobot.QuitMessagesListener qml) {
            qml.setChatMessagingCfg(joinQuitCfg);
        }
        if (activeConversationListener != null) {
            activeConversationListener.setChatMessagingCfg(joinQuitCfg);
        }
        startAutoMessages();
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        var mute = services(fr.elias.oessentials.mute.lifecycle.MuteSystemServices.class).getMuteService();
        var muteCmd = new fr.elias.oessentials.chat.internal.MuteCommand(mute, chatSyncManager);
        var unmuteCmd = new fr.elias.oessentials.chat.internal.UnmuteCommand(mute, chatSyncManager);
        var nickCmd = new fr.elias.oessentials.chat.internal.nick.NickCommand();
        commands.register(nickCmd)
                .register(new fr.elias.oessentials.chat.internal.BroadcastCommand())
                .register(new fr.elias.oessentials.chat.internal.AfeliusReloadCommand(plugin, chatConfig))
                .register(muteCmd)
                .register(unmuteCmd)
                .register(new fr.elias.oessentials.chat.internal.nick.RealNameCommand());
        commands.rewireTab("mute", muteCmd);
        commands.rewireTab("unmute", unmuteCmd);
        commands.rewireTab("nick", nickCmd);
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadChat(org.bukkit.command.CommandSender sender) {
        try {
            plugin.reloadChat();
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.chat",
                    "<green>✔ Reloaded <white>chat-format.yml</white> <gray>(channels=" + (channelManager != null && channelManager.isEnabled()) + ")</gray></green>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.chat-skip",
                    "<yellow>• Skipped chat: <gray>%error%</gray></yellow>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        }
    }
}
