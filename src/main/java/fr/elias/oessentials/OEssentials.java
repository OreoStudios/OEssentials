package fr.elias.oessentials;

import com.google.gson.Gson;
import dev.oreo.modulith.paper.PaperModulith;
import fr.elias.oessentials.platform.modularity.OreoModuleRegistry;
import fr.elias.oessentials.platform.modularity.OreoModules;
import fr.elias.oessentials.afk.lifecycle.AfkServices;
import fr.elias.oessentials.bossbar.lifecycle.BossBarServices;
import fr.elias.oessentials.chat.lifecycle.ChatServices;
import fr.elias.oessentials.clearlag.lifecycle.ClearLagServices;
import fr.elias.oessentials.commandcontrol.lifecycle.CommandControlServices;
import fr.elias.oessentials.commands.lifecycle.CommandsServices;
import fr.elias.oessentials.commandtoggle.lifecycle.CommandToggleServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.currency.lifecycle.CurrencySystemServices;
import fr.elias.oessentials.crafting.lifecycle.CustomCraftingServices;
import fr.elias.oessentials.daily.lifecycle.DailyServices;
import fr.elias.oessentials.dialogs.lifecycle.DialogsServices;
import fr.elias.oessentials.discord.lifecycle.DiscordIntegrationServices;
import fr.elias.oessentials.economy.lifecycle.EconomyServices;
import fr.elias.oessentials.enderchest.lifecycle.EnderChestServices;
import fr.elias.oessentials.freeze.lifecycle.FreezeServices;
import fr.elias.oessentials.grouprtp.lifecycle.GroupRtpServices;
import fr.elias.oessentials.ignore.lifecycle.IgnoreServices;
import fr.elias.oessentials.integrations.lifecycle.IntegrationsServices;
import fr.elias.oessentials.inventory.lifecycle.InventoriesServices;
import fr.elias.oessentials.jails.lifecycle.JailsServices;
import fr.elias.oessentials.jumppads.lifecycle.JumpPadsServices;
import fr.elias.oessentials.kits.lifecycle.KitsServices;
import fr.elias.oessentials.mail.lifecycle.MailServices;
import fr.elias.oessentials.maintenance.lifecycle.MaintenanceServices;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.mobs.lifecycle.MobsServices;
import fr.elias.oessentials.modgui.lifecycle.ModGuiServices;
import fr.elias.oessentials.mute.lifecycle.MuteSystemServices;
import fr.elias.oessentials.nametag.lifecycle.NametagServices;
import fr.elias.oessentials.notes.lifecycle.NotesServices;
import fr.elias.oessentials.guards.lifecycle.PerformanceGuardsServices;
import fr.elias.oessentials.player.lifecycle.PlayerServices;
import fr.elias.oessentials.playervaults.lifecycle.PlayerVaultsServices;
import fr.elias.oessentials.playtime.lifecycle.PlaytimeServices;
import fr.elias.oessentials.portals.lifecycle.PortalsServices;
import fr.elias.oessentials.proxy.lifecycle.ProxyMessagingServices;
import fr.elias.oessentials.punishment.lifecycle.PunishmentLoggerServices;
import fr.elias.oessentials.redis.lifecycle.RedisServices;
import fr.elias.oessentials.rtp.lifecycle.RtpServices;
import fr.elias.oessentials.scoreboard.lifecycle.ScoreboardServices;
import fr.elias.oessentials.shards.lifecycle.ShardingServices;
import fr.elias.oessentials.shop.lifecycle.ShopServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.tab.lifecycle.TabServices;
import fr.elias.oessentials.trade.lifecycle.TradeServices;
import fr.elias.oessentials.warnings.lifecycle.WarningsServices;
import fr.elias.oessentials.platform.commands.CommandManager;
import fr.elias.oessentials.configuration.internal.config.ConfigService;
import fr.elias.oessentials.configuration.internal.config.SettingsConfig;
import fr.elias.oessentials.economy.internal.persistence.database.PlayerEconomyDatabase;
import fr.elias.oessentials.redis.internal.persistence.database.RedisManager;
import fr.elias.oessentials.freeze.internal.freeze.FreezeManager;
import fr.elias.oessentials.modgui.internal.ip.IpTracker;
import fr.elias.oessentials.notes.internal.notes.NotesChatListener;
import fr.elias.oessentials.notes.internal.notes.PlayerNotesManager;
import fr.elias.oessentials.afk.internal.AfkPoolService;
import fr.elias.oessentials.afk.internal.AfkService;
import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.configuration.internal.autoreboot.AutoRebootService;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.rabbit.BackBroker;
import fr.elias.oessentials.player.internal.back.service.BackService;
import fr.elias.oessentials.chat.internal.ChatSyncManager;
import fr.elias.oessentials.chat.internal.chatservices.MuteService;
import fr.elias.oessentials.clearlag.internal.ClearLagManager;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleService;
import fr.elias.oessentials.modgui.internal.cross.ModBridge;
import fr.elias.oessentials.currency.internal.CurrencyConfig;
import fr.elias.oessentials.currency.internal.CurrencyService;
import fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion;
import fr.elias.oessentials.crafting.internal.customcraft.CraftActionsConfig;
import fr.elias.oessentials.crafting.internal.customcraft.CustomCraftingService;
import fr.elias.oessentials.player.internal.deathback.DeathBackService;
import fr.elias.oessentials.economy.internal.EconomyBootstrap;
import fr.elias.oessentials.storage.internal.homes.TeleportBroker;
import fr.elias.oessentials.storage.internal.homes.home.HomeService;
import fr.elias.oessentials.sync.internal.invlook.manager.InvlookManager;
import fr.elias.oessentials.sync.internal.invsee.InvseeService;
import fr.elias.oessentials.nametag.internal.PlayerNametagManager;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpDirectory;
import fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpService;
import fr.elias.oessentials.rtp.internal.RtpConfig;
import fr.elias.oessentials.rtp.internal.RtpPendingService;
import fr.elias.oessentials.scoreboard.internal.ScoreboardService;
import fr.elias.oessentials.inventory.internal.sellgui.manager.SellGuiManager;
import fr.elias.oessentials.shop.internal.ShopModule;
import fr.elias.oessentials.storage.internal.spawn.SpawnDirectory;
import fr.elias.oessentials.storage.internal.spawn.SpawnService;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.service.TeleportService;
import fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker;
import fr.elias.oessentials.trade.internal.service.TradeService;
import fr.elias.oessentials.storage.internal.warps.WarpService;
import fr.elias.oessentials.storage.internal.warps.rabbit.WarpDirectory;
import fr.elias.oessentials.economy.internal.offline.OfflinePlayerCache;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.PacketManager;
import fr.elias.oessentials.freeze.internal.state.FreezeService;
import fr.elias.oessentials.player.internal.services.GodService;
import fr.elias.oessentials.guards.internal.services.JoinFloodGuardService;
import fr.elias.oessentials.player.internal.services.MessageService;
import fr.elias.oessentials.storage.internal.services.StorageApi;
import fr.elias.oessentials.player.internal.services.VanishService;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.proxy.internal.transport.ProxyMessenger;
import fr.minuskube.inv.InventoryManager;
import java.util.Map;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class OEssentials extends JavaPlugin {
    private static OEssentials instance;
    private final OreoModuleRegistry moduleRegistry = new OreoModuleRegistry();
    private PaperModulith modulith;

    public OreoModuleRegistry getModuleRegistry() { return moduleRegistry; }
    public static OEssentials get() { return instance; }

    @Override
    public void onLoad() { loadExternalLibraries(); }

    // Editable configuration lives under config/, plugin-written state under data/.
    private static final String[] CONFIG_ENTRIES = {
            "settings.yml", "kits.yml", "rtp.yml", "sellgui.yml", "group-rtp.yml",
            "discord-integration.yml", "featureFlags.yml", "currency-config.yml", "events.yml",
            "jumpads.yml", "player-sync.yml", "recipes.yml", "worlds.yml", "lang.yml",
            "afk", "auctionhouse", "chat-messaging", "commandsmodule", "custom-currencies",
            "custom-nameplates", "dailyrewards", "lang", "oreopanel", "playtime-rewards",
            "scoreboard-tab", "server", "shop"
    };
    private static final String[] DATA_ENTRIES = {
            "balances.json", "balances.yml", "channel-data.yml", "currencies.json",
            "currency_balances.json", "daily_players.yml", "economy_last_type.yml", "economy.json",
            "essentials.json", "enderchests.yml", "ignore.yml", "ips.yml", "jails.yml",
            "kitsdata.yml", "mail.yml", "mutes.yml", "notes.yml", "playerwarps.yml",
            "playtime_data.yml", "punishment_history.yml", "warnings.yml", "vanish-state.yml",
            "player-sync-prefs.yml", "prewards_data.yml",
            "enderchests", "orders", "playervaults", "playerwarps", "portals", "trades",
            "vaults", "OHolograms", "players"
    };

    /**
     * Groups the data folder into config/ and data/. Creates the folder skeleton and, for
     * servers upgrading from the flat layout, moves any existing root-level file or folder
     * into its new home. Runs before modules load so all file paths resolve correctly.
     */
    private void reorganizeDataFolder() {
        java.io.File root = getDataFolder();
        if (!root.exists() && !root.mkdirs()) return;
        migrateInto(root, new java.io.File(root, "config"), CONFIG_ENTRIES);
        migrateInto(root, new java.io.File(root, "data"), DATA_ENTRIES);
    }

    private void migrateInto(java.io.File root, java.io.File target, String[] names) {
        target.mkdirs();
        for (String name : names) {
            java.io.File src = new java.io.File(root, name);
            java.io.File dest = new java.io.File(target, name);
            if (src.exists() && !dest.exists()) {
                java.io.File parent = dest.getParentFile();
                if (parent != null) parent.mkdirs();
                try {
                    java.nio.file.Files.move(src.toPath(), dest.toPath());
                    getLogger().info("[data-layout] moved " + name + " -> " + target.getName() + "/" + name);
                } catch (java.io.IOException e) {
                    getLogger().warning("[data-layout] could not move " + name + ": " + e.getMessage());
                }
            }
            // Ensure sub-folders exist on fresh installs so writes never fail.
            if (!name.contains(".")) dest.mkdirs();
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        reorganizeDataFolder();
        try {
            modulith = OreoModules.start(this);
            java.util.List<String> moduleIds = modulith.runtime().modules().stream()
                    .map(dev.oreo.modulith.core.ModuleDescriptor::id)
                    .collect(java.util.stream.Collectors.toList());
            java.util.Map<String, String> failures = modulith.runtime().diagnostics().failures();
            fr.elias.oessentials.platform.StartupBanner.print(this, moduleIds, failures.keySet());
            failures.forEach((id, reason) -> getLogger().warning("Module '" + id + "' is unavailable: " + reason));
        } catch (RuntimeException failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "OEssentials module startup failed", failure);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        OreScheduler.cancelAll(this);
        try {
            if (modulith != null) modulith.close();
        } catch (RuntimeException failure) {
            getLogger().log(java.util.logging.Level.SEVERE, "OEssentials module shutdown failed", failure);
        } finally {
            modulith = null;
            org.bukkit.event.HandlerList.unregisterAll(this);
            getServer().getServicesManager().unregisterAll(this);
            moduleRegistry.clear();
            instance = null;
        }
        getLogger().info("OEssentials disabled.");
    }

    public dev.oreo.modulith.core.RuntimeDiagnostics getModuleDiagnostics() {
        return modulith == null ? null : modulith.runtime().diagnostics();
    }

    public MuteService getMuteService() { return moduleRegistry.read(MuteSystemServices.class, MuteSystemServices::getMuteService); }

    public fr.elias.oessentials.ignore.internal.IgnoreService getIgnoreService() { return moduleRegistry.read(IgnoreServices.class, IgnoreServices::getIgnoreService); }

    public fr.elias.oessentials.warnings.internal.WarnService getWarnService() { return moduleRegistry.read(WarningsServices.class, WarningsServices::getWarnService); }

    public fr.elias.oessentials.mail.internal.MailService getMailService() { return moduleRegistry.read(MailServices.class, MailServices::getMailService); }

    public com.mongodb.client.MongoClient getHomesMongoClient() { return moduleRegistry.read(StorageServices.class, StorageServices::getHomesMongoClient); }

    public fr.elias.oessentials.punishment.internal.PunishmentLogger getPunishmentLogger() { return moduleRegistry.read(PunishmentLoggerServices.class, PunishmentLoggerServices::getPunishmentLogger); }

    /** Released API. Feature modules read {@link #getFeatureSettings()} instead. */
    public fr.elias.oessentials.configuration.internal.config.SettingsConfig getSettingsConfig() { return (SettingsConfig) getFeatureSettings(); }

    public fr.elias.oessentials.configuration.api.FeatureSettings getFeatureSettings() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getFeatureSettings); }

    public fr.elias.oessentials.configuration.api.PerformanceGuardSettings getPerformanceGuardSettings() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getPerformanceGuardSettings); }

    public JoinFloodGuardService getJoinFloodGuardService() { return moduleRegistry.read(PerformanceGuardsServices.class, PerformanceGuardsServices::getJoinFloodGuardService); }

    public ShopModule getShopModule() { return moduleRegistry.read(ShopServices.class, ShopServices::getShopModule); }

    public fr.elias.oessentials.integrations.internal.integration.DiscordModerationNotifier getDiscordMod() { return moduleRegistry.read(DiscordIntegrationServices.class, DiscordIntegrationServices::getDiscordMod); }

    public TpCrossServerBroker getTpBroker() { return moduleRegistry.read(fr.elias.oessentials.player.lifecycle.PlayerServices.class, fr.elias.oessentials.player.lifecycle.PlayerServices::getTpBroker); }

    public fr.elias.oessentials.portals.internal.PortalsManager getPortalsManager() { return moduleRegistry.read(PortalsServices.class, PortalsServices::getPortals); }

    public fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsManager getJumpPadsManager() { return moduleRegistry.read(JumpPadsServices.class, JumpPadsServices::getJumpPads); }

    public fr.elias.oessentials.modgui.internal.ModGuiService getModGuiService() { return moduleRegistry.read(ModGuiServices.class, ModGuiServices::getModGuiService); }

    public AuctionHouseModule getAuctionHouseModule() { return moduleRegistry.read(InventoriesServices.class, InventoriesServices::getAuctionHouse); }

    public fr.elias.oessentials.inventory.internal.orders.OrdersModule getOrdersModule() { return moduleRegistry.read(InventoriesServices.class, InventoriesServices::getOrdersModule); }

    public fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager getDiscordLinkManager() { return moduleRegistry.read(DiscordIntegrationServices.class, DiscordIntegrationServices::getDiscordLinkManager); }

    public CurrencyService getCurrencyService() { return moduleRegistry.read(CurrencySystemServices.class, CurrencySystemServices::getCurrencyService); }

    public CurrencyConfig getCurrencyConfig() { return moduleRegistry.read(CurrencySystemServices.class, CurrencySystemServices::getCurrencyConfig); }

    public fr.elias.oessentials.chat.internal.channels.ChatChannelManager getChannelManager() { return moduleRegistry.read(ChatServices.class, ChatServices::getChannelManager); }

    public fr.elias.oessentials.configuration.internal.tempfly.TempFlyService getTempFlyService() { return (fr.elias.oessentials.configuration.internal.tempfly.TempFlyService) moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getTempFly); }

    public EconomyBootstrap getEconomy() { return moduleRegistry.read(EconomyServices.class, EconomyServices::getEcoBootstrap); }

    public EconomyBootstrap getEcoBootstrap() { return moduleRegistry.read(EconomyServices.class, EconomyServices::getEcoBootstrap); }

    public InventoryManager getInvManager() { return moduleRegistry.read(InventoriesServices.class, InventoriesServices::getInvManager); }

    public InventoryManager getInventoryManager() { return moduleRegistry.read(InventoriesServices.class, InventoriesServices::getInvManager); }

    public AutoRebootService getAutoRebootService() { return (AutoRebootService) moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getAutoReboot); }

    public CraftActionsConfig getCraftActionsConfig() { return moduleRegistry.read(CustomCraftingServices.class, CustomCraftingServices::getCraftActionsConfig); }

    public SellGuiManager getSellGuiManager() { return moduleRegistry.read(InventoriesServices.class, InventoriesServices::getSellGuiManager); }

    public fr.elias.oessentials.maintenance.internal.MaintenanceService getMaintenanceService() { return moduleRegistry.read(MaintenanceServices.class, MaintenanceServices::getMaintenanceService); }

    public ProxyMessenger getProxyMessenger() { return moduleRegistry.read(ProxyMessagingServices.class, ProxyMessagingServices::getProxyMessenger); }

    public  fr.elias.oessentials.grouprtp.internal.GroupRtpModule getGroupRtpModule() { return moduleRegistry.read(GroupRtpServices.class, GroupRtpServices::getGroupRtpModule); }

    public TpaCrossServerBroker getTpaBroker() { return moduleRegistry.read(fr.elias.oessentials.player.lifecycle.PlayerServices.class, fr.elias.oessentials.player.lifecycle.PlayerServices::getTpaBroker); }

    public fr.elias.oessentials.kits.internal.KitsManager getKitsManager() { return moduleRegistry.read(KitsServices.class, KitsServices::getKitsManager); }

    public fr.elias.oessentials.tab.internal.TabListManager getTabListManager() { return moduleRegistry.read(TabServices.class, TabServices::getTabListManager); }

    public fr.elias.oessentials.bossbar.internal.BossBarService getBossBarService() { return moduleRegistry.read(BossBarServices.class, BossBarServices::getBossBarService); }

    public fr.elias.oessentials.playervaults.internal.PlayerVaultsService getPlayervaultsService() { return moduleRegistry.read(PlayerVaultsServices.class, PlayerVaultsServices::getPlayervaultsService); }

    public InvseeService getInvseeService() { return moduleRegistry.read(fr.elias.oessentials.sync.lifecycle.InventorySyncServices.class, fr.elias.oessentials.sync.lifecycle.InventorySyncServices::getInvseeService); }

    public fr.elias.oessentials.commands.internal.ic.ICManager getIcManager() { return moduleRegistry.read(CommandsServices.class, CommandsServices::getIcManager); }

    public fr.elias.oessentials.playtime.internal.PlaytimeRewardsService getPlaytimeRewards() { return moduleRegistry.read(PlaytimeServices.class, PlaytimeServices::getPlaytimeRewards); }

    public TradeCrossServerBroker getTradeBroker() { return moduleRegistry.read(fr.elias.oessentials.trade.lifecycle.TradeServices.class, fr.elias.oessentials.trade.lifecycle.TradeServices::getTradeBroker); }

    public TradeService getTradeService() { return moduleRegistry.read(TradeServices.class, TradeServices::getTradeService); }

    public AfkService getAfkService() { return moduleRegistry.read(AfkServices.class, AfkServices::getAfkService); }

    public AfkPoolService getAfkPoolService() { return moduleRegistry.read(AfkServices.class, AfkServices::getAfkPoolService); }

    /** Released API. Feature modules read {@link #getCrossServerFeatures()} instead. */
    public fr.elias.oessentials.configuration.internal.config.CrossServerSettings getCrossServerSettings() { return (fr.elias.oessentials.configuration.internal.config.CrossServerSettings) getCrossServerFeatures(); }

    public fr.elias.oessentials.configuration.api.CrossServerFeatures getCrossServerFeatures() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getCrossServerFeatures); }

    public fr.elias.oessentials.enderchest.internal.EnderChestService getEnderChestService() { return moduleRegistry.read(EnderChestServices.class, EnderChestServices::getEcService); }

    public RtpConfig getRtpConfig() { return moduleRegistry.read(RtpServices.class, RtpServices::getRtpConfig); }

    public ClearLagManager getClearLagManager() { return moduleRegistry.read(ClearLagServices.class, ClearLagServices::getClearLag); }

    public fr.elias.oessentials.commandcontrol.internal.CommandControlService getCommandControlService() { return moduleRegistry.read(CommandControlServices.class, CommandControlServices::getCommandControlService); }

    public fr.elias.oessentials.daily.internal.DailyService getDailyService() { return moduleRegistry.read(DailyServices.class, DailyServices::getDailyService); }

    public fr.elias.oessentials.commandcontrol.internal.aliases.AliasService getAliasService() { return moduleRegistry.read(ProxyMessagingServices.class, ProxyMessagingServices::getAliasService); }

    public fr.elias.oessentials.jails.internal.jail.JailService getJailService() { return moduleRegistry.read(JailsServices.class, JailsServices::getJailService); }

    public CustomCraftingService getCustomCraftingService() { return moduleRegistry.read(CustomCraftingServices.class, CustomCraftingServices::getCustomCraftingService); }

    public fr.elias.oessentials.nametag.internal.MultiBossBarService getMultiBossBarService() { return moduleRegistry.read(NametagServices.class, NametagServices::getMultiBossBarService); }

    public fr.elias.oessentials.nametag.internal.CustomNameplatesConfig getCustomNameplatesConfig() { return moduleRegistry.read(NametagServices.class, NametagServices::getCustomNameplatesConfig); }

    public BackBroker getBackBroker() { return moduleRegistry.read(fr.elias.oessentials.player.lifecycle.PlayerServices.class, fr.elias.oessentials.player.lifecycle.PlayerServices::getBackBroker); }

    public Gson getGson() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getGson); }

    public Map<UUID, BackLocation> getPendingBackTeleports() { return moduleRegistry.read(fr.elias.oessentials.player.lifecycle.PlayerServices.class, fr.elias.oessentials.player.lifecycle.PlayerServices::getPendingBackTeleports); }

    public VanishService getVanishService() {
        return moduleRegistry.read(PlayerServices.class, PlayerServices::getVanishService);
    }
    public void reloadCustomNameplates() { moduleRegistry.require(NametagServices.class).reloadCustomNameplates(); }

    private void loadExternalLibraries() {
        try {
            net.byteflux.libby.BukkitLibraryManager libraryManager = new net.byteflux.libby.BukkitLibraryManager(this);
            libraryManager.addMavenCentral();
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.mongodb").artifactId("bson").version("5.1.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.mongodb").artifactId("mongodb-driver-core").version("5.1.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.mongodb").artifactId("mongodb-driver-sync").version("5.1.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.apache.commons").artifactId("commons-pool2").version("2.12.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("redis.clients").artifactId("jedis").version("5.1.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.postgresql").artifactId("postgresql").version("42.7.4").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("com.rabbitmq").artifactId("amqp-client").version("5.15.0").build());
            libraryManager.loadLibrary(net.byteflux.libby.Library.builder().groupId("org.xerial").artifactId("sqlite-jdbc").version("3.46.1.3").build());
            getLogger().info("[LibraryLoader] All dependencies loaded successfully!");
        } catch (Exception e) {
            getLogger().severe("[LibraryLoader] FAILED to load dependencies: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    public boolean isMessagingAvailable() { return moduleRegistry.read(MessagingServices.class, MessagingServices::getPacketBus) != null && moduleRegistry.read(MessagingServices.class, MessagingServices::getPacketBus).isInitialized(); }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public IpTracker getIpTracker() { return moduleRegistry.read(NotesServices.class, NotesServices::getIpTracker); }

    public FreezeManager getFreezeManager() { return moduleRegistry.read(FreezeServices.class, FreezeServices::getFreezeManager); }

    public PlayerNotesManager getNotesManager() { return moduleRegistry.read(NotesServices.class, NotesServices::getNotesManager); }

    public NotesChatListener getNotesChat() { return moduleRegistry.read(NotesServices.class, NotesServices::getNotesChat); }

    /** Released API. Feature modules read {@link #getServerConfig()} instead. */
    public ConfigService getConfigService() { return (ConfigService) getServerConfig(); }

    public fr.elias.oessentials.configuration.api.ServerConfig getServerConfig() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getServerConfig); }

    public StorageApi getStorage() { return moduleRegistry.read(StorageServices.class, StorageServices::getStorage); }

    public SpawnService getSpawnService() { return moduleRegistry.read(StorageServices.class, StorageServices::getSpawnService); }

    public WarpService getWarpService() { return moduleRegistry.read(StorageServices.class, StorageServices::getWarpService); }

    public HomeService getHomeService() { return moduleRegistry.read(StorageServices.class, StorageServices::getHomeService); }

    public PlayerWarpService getPlayerWarpService() { return moduleRegistry.read(StorageServices.class, StorageServices::getPlayerWarpService); }

    public PlayerWarpDirectory getPlayerWarpDirectory() { return moduleRegistry.read(StorageServices.class, StorageServices::getPlayerWarpDirectory); }

    public InvlookManager getInvlookManager() { return moduleRegistry.read(fr.elias.oessentials.sync.lifecycle.InventorySyncServices.class, fr.elias.oessentials.sync.lifecycle.InventorySyncServices::getInvlookManager); }

    public TeleportService getTeleportService() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getTeleportService); }

    public BackService getBackService() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getBackService); }

    public MessageService getMessageService() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getMessageService); }

    public DeathBackService getDeathBackService() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getDeathBackService); }

    public GodService getGodService() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getGodService); }

    public CommandManager getCommands() { return moduleRegistry.read(ConfigurationServices.class, ConfigurationServices::getCommands); }

    public ChatSyncManager getChatSyncManager() { return moduleRegistry.read(ChatServices.class, ChatServices::getChatSyncManager); }

    public fr.elias.oessentials.mobs.internal.HealthBarListener getHealthBarListener() { return moduleRegistry.read(MobsServices.class, MobsServices::getHealthBarListener); }

    public FreezeService getFreezeService() { return moduleRegistry.read(FreezeServices.class, FreezeServices::getFreezeService); }

    public fr.elias.oessentials.chat.internal.CustomConfig getChatConfig() { return moduleRegistry.read(ChatServices.class, ChatServices::getChatConfig); }

    public org.bukkit.configuration.file.FileConfiguration getPlayerWarpsConfig() { return moduleRegistry.read(StorageServices.class, StorageServices::getPlayerWarpsConfig); }
    public void reloadPlayerWarpsConfig() { moduleRegistry.require(StorageServices.class).reloadPlayerWarpsConfig(); }

    public fr.elias.oessentials.daily.internal.DailyConfig getDailyConfig() { return moduleRegistry.read(DailyServices.class, DailyServices::getDailyConfig); }

    public fr.elias.oessentials.daily.internal.RewardsConfig getDailyRewardsConfig() { return moduleRegistry.read(DailyServices.class, DailyServices::getDailyRewardsConfig); }
    public void reloadChat() { moduleRegistry.require(ChatServices.class).reloadChat(); }

    public WarpDirectory getWarpDirectory() { return moduleRegistry.read(StorageServices.class, StorageServices::getWarpDirectory); }

    public SpawnDirectory getSpawnDirectory() { return moduleRegistry.read(StorageServices.class, StorageServices::getSpawnDirectory); }

    public fr.elias.oessentials.storage.internal.directory.PlayerDirectory getPlayerDirectory() { return moduleRegistry.read(StorageServices.class, StorageServices::getPlayerDirectory); }

    public TeleportBroker getTeleportBroker() { return moduleRegistry.read(PlayerServices.class, PlayerServices::getTeleportBroker); }

    public RedisManager getRedis() { return moduleRegistry.read(RedisServices.class, RedisServices::getRedis); }
    public OfflinePlayerCache getOfflinePlayerCache() { return moduleRegistry.require(EconomyServices.class).getOfflinePlayerCache(); }

    public PlayerEconomyDatabase getDatabase() { return moduleRegistry.read(EconomyServices.class, EconomyServices::getDatabase); }

    /** Released API. Feature modules use {@link #getPacketBus()} instead. */
    public PacketManager getPacketManager() { return (PacketManager) getPacketBus(); }

    public fr.elias.oessentials.messaging.api.PacketBus getPacketBus() { return moduleRegistry.read(MessagingServices.class, MessagingServices::getPacketBus); }

    public ScoreboardService getScoreboardService() { return moduleRegistry.read(ScoreboardServices.class, ScoreboardServices::getScoreboardService); }

    public fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTeleportBroker() { return moduleRegistry.read(fr.elias.oessentials.storage.lifecycle.StorageServices.class, fr.elias.oessentials.storage.lifecycle.StorageServices::getHomeTpBroker); }

    public fr.elias.oessentials.playervaults.internal.PlayerVaultsService getPlayerVaultsService() { return moduleRegistry.read(PlayerVaultsServices.class, PlayerVaultsServices::getPlayervaultsService); }

    public fr.elias.oessentials.playtime.internal.PlaytimeRewardsService getPlaytimeRewardsService() { return moduleRegistry.read(PlaytimeServices.class, PlaytimeServices::getPlaytimeRewards); }

    public ModBridge getModBridge() { return moduleRegistry.read(fr.elias.oessentials.modgui.lifecycle.ModGuiServices.class, fr.elias.oessentials.modgui.lifecycle.ModGuiServices::getModBridge); }

    public java.util.Map<java.util.UUID, Long> getRtpCooldownCache() { return moduleRegistry.read(RtpServices.class, RtpServices::getRtpCooldownCache); }

    public fr.elias.oessentials.playtime.internal.PlaytimeTracker getPlaytimeTracker() { return moduleRegistry.read(PlaytimeServices.class, PlaytimeServices::getPlaytimeTracker); }

    public PlayerNametagManager getNametagManager() { return moduleRegistry.read(NametagServices.class, NametagServices::getNametagManager); }

    public fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager getDialogManager() { return moduleRegistry.read(DialogsServices.class, DialogsServices::getDialogManager); }

    public fr.elias.oessentials.nametag.internal.ChatBubbleService getChatBubbleService() { return moduleRegistry.read(NametagServices.class, NametagServices::getChatBubbleService); }

    /** Released API; same object as {@link #getSettingsConfig()}. */
    public SettingsConfig getSettings() { return getSettingsConfig(); }

    public boolean isJoinUiReady(Player player) {
        return moduleRegistry.read(PerformanceGuardsServices.class, PerformanceGuardsServices::getJoinFloodGuardService) == null || moduleRegistry.read(PerformanceGuardsServices.class, PerformanceGuardsServices::getJoinFloodGuardService).isUiReady(player);
    }

    public long getJoinUiDelayTicks(Player player, long minimumDelayTicks) {
        return moduleRegistry.read(PerformanceGuardsServices.class, PerformanceGuardsServices::getJoinFloodGuardService) == null
                ? Math.max(0L, minimumDelayTicks)
                : moduleRegistry.read(PerformanceGuardsServices.class, PerformanceGuardsServices::getJoinFloodGuardService).getDeferredJoinDelayTicks(player, minimumDelayTicks);
    }

    public RtpPendingService getRtpPendingService() { return moduleRegistry.read(RtpServices.class, RtpServices::getRtpPendingService); }

    public fr.elias.oessentials.rtp.internal.RtpCrossServerBridge getRtpBridge() { return moduleRegistry.read(RtpServices.class, RtpServices::getRtpBridge); }

    public fr.elias.oessentials.shards.internal.OreoShardsModule getShardsModule() { return moduleRegistry.read(ShardingServices.class, ShardingServices::getShardsModule); }

    public CommandToggleService getCommandToggleService() { return moduleRegistry.read(CommandToggleServices.class, CommandToggleServices::getCommandToggleService); }

    public Economy getVaultEconomy() { return moduleRegistry.read(EconomyServices.class, EconomyServices::getVaultEconomy); }

    public CurrencyPlaceholderExpansion getCurrencyPlaceholders() { return moduleRegistry.read(IntegrationsServices.class, IntegrationsServices::getCurrencyPlaceholders); }

    public String getServerNameSafe() {
        try {
            if (getServerConfig() != null) { String name = getServerConfig().serverName(); if (name != null && !name.isBlank()) return name; }
        } catch (Throwable ignored) {}
        try { String name = getServer().getName(); if (name != null && !name.isBlank()) return name; } catch (Throwable ignored) {}
        return "UNKNOWN";
    }

    public final class PapiUtil {
        private PapiUtil() {}
        public static String apply(Player p, String text) {
            if (text == null) return "";
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
            try { return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, text); }
            catch (Throwable t) { return text; }
        }
    }
}
