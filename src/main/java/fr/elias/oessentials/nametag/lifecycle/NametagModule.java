package fr.elias.oessentials.nametag.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.nametag.internal.PlayerNametagManager;

@PluginModule(value = "nametag", dependencies = {"configuration::services"}, after = {"events"}, optional = true)
public final class NametagModule extends ManagedModule implements NametagServices {
    private PlayerNametagManager nametagManager;
    private fr.elias.oessentials.nametag.internal.ChatBubbleService chatBubbleService;
    private fr.elias.oessentials.nametag.internal.ActionBarService actionBarService;
    private fr.elias.oessentials.nametag.internal.MultiBossBarService multiBossBarService;
    private fr.elias.oessentials.nametag.internal.CustomNameplatesConfig customNameplatesConfig;
    private fr.elias.oessentials.nametag.internal.NametageToggleStore nametagToggleStore;

    @Override
    protected void start() {
        cleanup("nametagManager", () -> { if (nametagManager != null) nametagManager.shutdown(); });
        cleanup("chatBubbleService", () -> { if (chatBubbleService != null) chatBubbleService.shutdown(); });
        cleanup("actionBarService", () -> { if (actionBarService != null) actionBarService.shutdown(); });
        cleanup("multiBossBarService", () -> { if (multiBossBarService != null) multiBossBarService.shutdown(); });
        initNametag();
    }

    @Override public PlayerNametagManager getNametagManager() { return nametagManager; }
    @Override public fr.elias.oessentials.nametag.internal.ChatBubbleService getChatBubbleService() { return chatBubbleService; }
    @Override public fr.elias.oessentials.nametag.internal.ActionBarService getActionBarService() { return actionBarService; }
    @Override public fr.elias.oessentials.nametag.internal.MultiBossBarService getMultiBossBarService() { return multiBossBarService; }
    @Override public fr.elias.oessentials.nametag.internal.CustomNameplatesConfig getCustomNameplatesConfig() { return customNameplatesConfig; }
    @Override public fr.elias.oessentials.nametag.internal.NametageToggleStore getNametagToggleStore() { return nametagToggleStore; }

    private void initNametag() {
        this.customNameplatesConfig = new fr.elias.oessentials.nametag.internal.CustomNameplatesConfig(plugin);
        this.nametagToggleStore = new fr.elias.oessentials.nametag.internal.NametageToggleStore(plugin);
        org.bukkit.configuration.file.FileConfiguration npCfg = customNameplatesConfig.raw();

        if (npCfg.getBoolean("nametag.enabled", true) && BootstrapSupport.uiModuleAllowed(plugin, "nametag")) {
            try {
                this.nametagManager = new PlayerNametagManager(plugin, npCfg);
                this.nametagManager.setToggleStore(nametagToggleStore);
                plugin.getLogger().info("[Nametag] Custom nametags initialized");
            } catch (Exception e) {
                plugin.getLogger().severe("[Nametag] Failed to initialize: " + e.getMessage());
                e.printStackTrace();
                this.nametagManager = null;
            }
        } else {
            this.nametagManager = null;
            plugin.getLogger().info("[Nametag] Disabled in custom-nameplates/config.yml");
        }

        try {
            if (BootstrapSupport.uiModuleAllowed(plugin, "chat-bubbles")) {
                this.chatBubbleService = new fr.elias.oessentials.nametag.internal.ChatBubbleService(plugin, npCfg);
            } else {
                this.chatBubbleService = null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[ChatBubble] Failed to initialize: " + e.getMessage());
            this.chatBubbleService = null;
        }

        try {
            if (BootstrapSupport.uiModuleAllowed(plugin, "actionbar")) {
                this.actionBarService = new fr.elias.oessentials.nametag.internal.ActionBarService(plugin, npCfg);
            } else {
                this.actionBarService = null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[ActionBar] Failed to initialize: " + e.getMessage());
            this.actionBarService = null;
        }

        try {
            if (BootstrapSupport.uiModuleAllowed(plugin, "multi-bossbar")) {
                this.multiBossBarService = new fr.elias.oessentials.nametag.internal.MultiBossBarService(plugin, npCfg);
            } else {
                this.multiBossBarService = null;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MultiBossBar] Failed to initialize: " + e.getMessage());
            this.multiBossBarService = null;
        }

        // Register commands
        services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.nametag.internal.commands.NametageCommand(plugin, nametagToggleStore));
        services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.nametag.internal.commands.CustomNameplatesCommand(plugin));
        services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.nametag.internal.commands.BubbleColorCommand(plugin));
    }

    /** Reloads all custom-nameplates services from disk. Called by /cnp reload. */
    public void reloadCustomNameplates() {
        customNameplatesConfig.reload();
        org.bukkit.configuration.file.FileConfiguration npCfg = customNameplatesConfig.raw();

        if (!BootstrapSupport.uiModuleAllowed(plugin, "nametag")) {
            if (nametagManager != null) nametagManager.shutdown();
            nametagManager = null;
        } else if (nametagManager != null) {
            nametagManager.reload(npCfg);
        } else if (npCfg.getBoolean("nametag.enabled", true)) {
            nametagManager = new PlayerNametagManager(plugin, npCfg);
            nametagManager.setToggleStore(nametagToggleStore);
        }

        if (!BootstrapSupport.uiModuleAllowed(plugin, "chat-bubbles")) {
            if (chatBubbleService != null) chatBubbleService.shutdown();
            chatBubbleService = null;
        } else if (chatBubbleService != null) {
            chatBubbleService.reload(npCfg);
        } else {
            chatBubbleService = new fr.elias.oessentials.nametag.internal.ChatBubbleService(plugin, npCfg);
        }

        if (!BootstrapSupport.uiModuleAllowed(plugin, "actionbar")) {
            if (actionBarService != null) actionBarService.shutdown();
            actionBarService = null;
        } else if (actionBarService != null) {
            actionBarService.reload(npCfg);
        } else {
            actionBarService = new fr.elias.oessentials.nametag.internal.ActionBarService(plugin, npCfg);
        }

        if (!BootstrapSupport.uiModuleAllowed(plugin, "multi-bossbar")) {
            if (multiBossBarService != null) multiBossBarService.shutdown();
            multiBossBarService = null;
        } else if (multiBossBarService != null) {
            multiBossBarService.reload(npCfg);
        } else {
            multiBossBarService = new fr.elias.oessentials.nametag.internal.MultiBossBarService(plugin, npCfg);
        }
    }
}
