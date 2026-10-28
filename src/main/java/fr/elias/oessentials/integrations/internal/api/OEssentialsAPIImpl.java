package fr.elias.oessentials.integrations.internal.api;

import fr.elias.oessentials.afk.internal.api.AfkAPIImpl;
import fr.elias.oessentials.commandcontrol.internal.api.AliasesAPIImpl;
import fr.elias.oessentials.inventory.internal.api.AuctionHouseAPIImpl;
import fr.elias.oessentials.player.internal.api.BackAPIImpl;
import fr.elias.oessentials.bossbar.internal.api.BossBarAPIImpl;
import fr.elias.oessentials.chat.internal.api.ChatAPIImpl;
import fr.elias.oessentials.clearlag.internal.api.ClearLagAPIImpl;
import fr.elias.oessentials.commandcontrol.internal.api.CommandControlAPIImpl;
import fr.elias.oessentials.commandtoggle.internal.api.CommandToggleAPIImpl;
import fr.elias.oessentials.modgui.internal.cross.CrossServerAPIImpl;
import fr.elias.oessentials.currency.internal.api.CurrencyAPIImpl;
import fr.elias.oessentials.crafting.internal.api.CustomCraftAPIImpl;
import fr.elias.oessentials.daily.internal.api.DailyAPIImpl;
import fr.elias.oessentials.player.internal.api.DeathBackAPIImpl;
import fr.elias.oessentials.economy.internal.api.EconomyAPIImpl;
import fr.elias.oessentials.enderchest.internal.api.EnderChestAPIImpl;
import fr.elias.oessentials.freeze.lifecycle.FreezeServices;
import fr.elias.oessentials.holograms.internal.api.HologramsAPIImpl;
import fr.elias.oessentials.ignore.internal.api.IgnoreAPIImpl;
import fr.elias.oessentials.commands.internal.api.InteractiveCommandsAPIImpl;
import fr.elias.oessentials.jails.internal.api.JailAPIImpl;
import fr.elias.oessentials.jumppads.internal.api.JumpPadsAPIImpl;
import fr.elias.oessentials.kits.internal.api.KitsAPIImpl;
import fr.elias.oessentials.mail.internal.api.MailAPIImpl;
import fr.elias.oessentials.maintenance.internal.api.MaintenanceAPIImpl;
import fr.elias.oessentials.nametag.internal.api.NametagAPIImpl;
import fr.elias.oessentials.inventory.internal.api.OrdersAPIImpl;
import fr.elias.oessentials.playervaults.internal.api.PlayerVaultsAPIImpl;
import fr.elias.oessentials.storage.internal.api.PlayerWarpsAPIImpl;
import fr.elias.oessentials.playtime.internal.api.PlaytimeAPIImpl;
import fr.elias.oessentials.portals.internal.api.PortalsAPIImpl;
import fr.elias.oessentials.punishment.internal.api.PunishmentAPIImpl;
import fr.elias.oessentials.scoreboard.internal.api.ScoreboardAPIImpl;
import fr.elias.oessentials.inventory.internal.api.SellGuiAPIImpl;
import fr.elias.oessentials.shards.internal.api.ShardsAPIImpl;
import fr.elias.oessentials.shop.internal.api.ShopAPIImpl;
import fr.elias.oessentials.storage.internal.api.SpawnAPIImpl;
import fr.elias.oessentials.player.internal.api.TeleportAPIImpl;
import fr.elias.oessentials.trade.internal.api.TradeAPIImpl;
import fr.elias.oessentials.player.internal.api.VanishAPIImpl;
import fr.elias.oessentials.warnings.internal.api.WarningsAPIImpl;
import fr.elias.oessentials.storage.internal.api.WarpsAPIImpl;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.api.IAfkAPI;
import fr.elias.oessentials.api.IAliasesAPI;
import fr.elias.oessentials.api.IAuctionHouseAPI;
import fr.elias.oessentials.api.IAutoRebootAPI;
import fr.elias.oessentials.api.IBackAPI;
import fr.elias.oessentials.api.IBossBarAPI;
import fr.elias.oessentials.api.IChatAPI;
import fr.elias.oessentials.api.IClearLagAPI;
import fr.elias.oessentials.api.ICommandControlAPI;
import fr.elias.oessentials.api.ICommandToggleAPI;
import fr.elias.oessentials.api.ICrossServerAPI;
import fr.elias.oessentials.api.ICurrencyAPI;
import fr.elias.oessentials.api.ICustomCraftAPI;
import fr.elias.oessentials.api.IDailyAPI;
import fr.elias.oessentials.api.IDeathBackAPI;
import fr.elias.oessentials.api.IEconomyAPI;
import fr.elias.oessentials.api.IEnderChestAPI;
import fr.elias.oessentials.api.IFreezeAPI;
import fr.elias.oessentials.api.IHologramsAPI;
import fr.elias.oessentials.api.IIgnoreAPI;
import fr.elias.oessentials.api.IInteractiveCommandsAPI;
import fr.elias.oessentials.api.IJailAPI;
import fr.elias.oessentials.api.IJumpPadsAPI;
import fr.elias.oessentials.api.IKitsAPI;
import fr.elias.oessentials.api.IMailAPI;
import fr.elias.oessentials.api.IMaintenanceAPI;
import fr.elias.oessentials.api.INametagAPI;
import fr.elias.oessentials.api.IOrdersAPI;
import fr.elias.oessentials.api.IPlayerVaultsAPI;
import fr.elias.oessentials.api.IPlayerWarpsAPI;
import fr.elias.oessentials.api.IPlaytimeAPI;
import fr.elias.oessentials.api.IPortalsAPI;
import fr.elias.oessentials.api.IPunishmentAPI;
import fr.elias.oessentials.api.IScoreboardAPI;
import fr.elias.oessentials.api.ISellGuiAPI;
import fr.elias.oessentials.api.IShardsAPI;
import fr.elias.oessentials.api.IShopAPI;
import fr.elias.oessentials.api.ISpawnAPI;
import fr.elias.oessentials.api.ITeleportAPI;
import fr.elias.oessentials.api.ITempFlyAPI;
import fr.elias.oessentials.api.ITradeAPI;
import fr.elias.oessentials.api.IVanishAPI;
import fr.elias.oessentials.api.IWarningsAPI;
import fr.elias.oessentials.api.IWarpsAPI;
import fr.elias.oessentials.api.OEssentialsAPI;
import fr.elias.oessentials.currency.internal.Currency;
import fr.elias.oessentials.currency.internal.CurrencyService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Internal implementation of {@link OEssentialsAPI}. Not part of the public API surface.
 *
 * <p>All module getters are lazy — they read from the plugin instance at call time,
 * so it is safe to register this before all modules have finished initialising.
 */
public class OEssentialsAPIImpl implements OEssentialsAPI {

    private static final String API_VERSION = "1.0";

    private final OEssentials plugin;

    public OEssentialsAPIImpl(OEssentials plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Meta
    // -------------------------------------------------------------------------

    @Override
    public @NotNull String version() {
        return API_VERSION;
    }

    // -------------------------------------------------------------------------
    // Currency
    // -------------------------------------------------------------------------

    @Override
    public @Nullable ICurrencyAPI currency(@NotNull String currencyId) {
        CurrencyService svc = plugin.getCurrencyService();
        if (svc == null) return null;
        Currency c = svc.getCurrency(currencyId.toLowerCase(Locale.ROOT).trim());
        if (c == null) return null;
        return new CurrencyAPIImpl(svc, c);
    }

    @Override
    public @NotNull List<ICurrencyAPI> currencies() {
        CurrencyService svc = plugin.getCurrencyService();
        if (svc == null) return List.of();
        return svc.getAllCurrencies().stream()
                .map(c -> (ICurrencyAPI) new CurrencyAPIImpl(svc, c))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasCurrency(@NotNull String currencyId) {
        CurrencyService svc = plugin.getCurrencyService();
        if (svc == null) return false;
        return svc.getCurrency(currencyId.toLowerCase(Locale.ROOT).trim()) != null;
    }

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    @Override
    public @Nullable IOrdersAPI orders() {
        var module = plugin.getOrdersModule();
        if (module == null) return null;
        var svc = module.getService();
        if (svc == null) return null;
        return new OrdersAPIImpl(svc);
    }

    @Override
    public @Nullable INametagAPI nametag() {
        var mgr = plugin.getNametagManager();
        if (mgr == null) return null;
        return new NametagAPIImpl(mgr);
    }

    @Override
    public @Nullable IChatAPI chat() {
        var mgr = plugin.getChatSyncManager();
        if (mgr == null) return null;
        return new ChatAPIImpl(mgr);
    }

    @Override
    public @Nullable IAuctionHouseAPI auctionHouse() {
        var module = plugin.getAuctionHouseModule();
        if (module == null) return null;
        return new AuctionHouseAPIImpl(module);
    }

    @Override
    public @Nullable ITradeAPI trade() {
        var svc = plugin.getTradeService();
        if (svc == null) return null;
        return new TradeAPIImpl(svc);
    }

    @Override
    public @Nullable IShopAPI shop() {
        var module = plugin.getShopModule();
        if (module == null) return null;
        return new ShopAPIImpl(module);
    }

    @Override public @Nullable IAfkAPI afk() { var s = plugin.getAfkService(); return s == null ? null : new AfkAPIImpl(s); }
    @Override public @Nullable IBossBarAPI bossBar() { var s = plugin.getBossBarService(); return s == null ? null : new BossBarAPIImpl(s); }
    @Override public @Nullable IClearLagAPI clearLag() { var s = plugin.getClearLagManager(); return s == null ? null : new ClearLagAPIImpl(s); }
    @Override public @Nullable ICommandControlAPI commandControl() { var s = plugin.getCommandControlService(); return s == null ? null : new CommandControlAPIImpl(s); }
    @Override public @Nullable ICrossServerAPI crossServer() { var s = plugin.getModBridge(); return s == null ? null : new CrossServerAPIImpl(s); }
    @Override public @Nullable ICustomCraftAPI customCraft() { var s = plugin.getCustomCraftingService(); return s == null ? null : new CustomCraftAPIImpl(s); }
    @Override public @Nullable IDailyAPI daily() { var s = plugin.getDailyService(); return s == null ? null : new DailyAPIImpl(s); }
    @Override public @Nullable IEconomyAPI economy() {
        var eco = plugin.getEconomy();
        if (eco == null) return null;
        var svc = eco.api();
        return svc == null ? null : new EconomyAPIImpl(svc);
    }
    @Override public @Nullable IEnderChestAPI enderChest() { var s = plugin.getEnderChestService(); return s == null ? null : new EnderChestAPIImpl(s); }
    @Override public @Nullable IHologramsAPI holograms() {
        var mgr = fr.elias.oessentials.holograms.internal.OHolograms.canGet()
                ? fr.elias.oessentials.holograms.internal.OHolograms.get() : null;
        if (mgr == null) return null;
        var holMgr = mgr.getHologramManager();
        return holMgr == null ? null : new HologramsAPIImpl(holMgr);
    }
    @Override public @Nullable IKitsAPI kits() { var s = plugin.getKitsManager(); return s == null ? null : new KitsAPIImpl(s); }
    @Override public @Nullable IJumpPadsAPI jumpPads() { var s = plugin.getJumpPadsManager(); return s == null ? null : new JumpPadsAPIImpl(s); }
    @Override public @Nullable IJailAPI jail() { var s = plugin.getJailService(); return s == null ? null : new JailAPIImpl(s); }
    @Override public @Nullable IMaintenanceAPI maintenance() { var s = plugin.getMaintenanceService(); return s == null ? null : new MaintenanceAPIImpl(s); }
    @Override public @Nullable ISpawnAPI spawn() { var s = plugin.getSpawnService(); return s == null ? null : new SpawnAPIImpl(s); }
    @Override public @Nullable ITeleportAPI teleport() { var s = plugin.getTeleportService(); return s == null ? null : new TeleportAPIImpl(s); }
    @Override public @Nullable IWarpsAPI warps() { var s = plugin.getWarpService(); return s == null ? null : new WarpsAPIImpl(s); }
    @Override public @Nullable ISellGuiAPI sellGui() { var s = plugin.getSellGuiManager(); return s == null ? null : new SellGuiAPIImpl(s); }
    @Override public @Nullable IPunishmentAPI punishment() { var s = plugin.getPunishmentLogger(); return s == null ? null : new PunishmentAPIImpl(s); }
    @Override public @Nullable IPortalsAPI portals() { var s = plugin.getPortalsManager(); return s == null ? null : new PortalsAPIImpl(s); }
    @Override public @Nullable IPlayerWarpsAPI playerWarps() { var s = plugin.getPlayerWarpService(); return s == null ? null : new PlayerWarpsAPIImpl(s); }
    @Override public @Nullable IPlayerVaultsAPI playerVaults() { var s = plugin.getPlayervaultsService(); return s == null ? null : new PlayerVaultsAPIImpl(s); }
    @Override public @Nullable ICommandToggleAPI commandToggle() { var s = plugin.getCommandToggleService(); return s == null ? null : new CommandToggleAPIImpl(s); }
    @Override public @Nullable IInteractiveCommandsAPI interactiveCommands() { var s = plugin.getIcManager(); return s == null ? null : new InteractiveCommandsAPIImpl(s); }
    @Override public @Nullable IBackAPI back() { var s = plugin.getBackService(); return s == null ? null : new BackAPIImpl(s); }
    @Override public @Nullable IDeathBackAPI deathBack() { var s = plugin.getDeathBackService(); return s == null ? null : new DeathBackAPIImpl(s); }
    @Override public @Nullable IIgnoreAPI ignore() { var s = plugin.getIgnoreService(); return s == null ? null : new IgnoreAPIImpl(s); }
    @Override public @Nullable IMailAPI mail() { var s = plugin.getMailService(); return s == null ? null : new MailAPIImpl(s); }
    @Override public @Nullable IPlaytimeAPI playtime() {
        var tracker = plugin.getPlaytimeTracker();
        var rewards = plugin.getPlaytimeRewardsService();
        return (tracker == null || rewards == null) ? null : new PlaytimeAPIImpl(tracker, rewards);
    }
    @Override public @Nullable IScoreboardAPI scoreboard() { var s = plugin.getScoreboardService(); return s == null ? null : new ScoreboardAPIImpl(s); }
    @Override public @Nullable IShardsAPI shards() { var s = plugin.getShardsModule(); return s == null ? null : new ShardsAPIImpl(s); }
    @Override public @Nullable ITempFlyAPI tempFly() { return plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getTempFly); }
    @Override public @Nullable IWarningsAPI warnings() { var s = plugin.getWarnService(); return s == null ? null : new WarningsAPIImpl(s); }
    @Override public @Nullable IAutoRebootAPI autoReboot() { return plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getAutoReboot); }
    @Override public @Nullable IAliasesAPI aliases() { var s = plugin.getAliasService(); return s == null ? null : new AliasesAPIImpl(s); }
    @Override public @Nullable IVanishAPI vanish() { var s = plugin.getVanishService(); return s == null ? null : new VanishAPIImpl(s); }
    @Override public @Nullable IFreezeAPI freeze() { return plugin.getModuleRegistry().read(FreezeServices.class, FreezeServices::getFreezeApi); }
}
