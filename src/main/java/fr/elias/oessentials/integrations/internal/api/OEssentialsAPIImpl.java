package fr.elias.oessentials.integrations.internal.api;


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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
        return plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, s -> s.getCurrencyApi(currencyId));
    }

    @Override
    public @NotNull List<ICurrencyAPI> currencies() {
        List<ICurrencyAPI> list = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencyApis);
        return list == null ? List.of() : list;
    }

    @Override
    public boolean hasCurrency(@NotNull String currencyId) {
        return currency(currencyId) != null;
    }

    // -------------------------------------------------------------------------
    // Modules
    // -------------------------------------------------------------------------

    @Override
    public @Nullable IOrdersAPI orders() {
        return publicApi(fr.elias.oessentials.api.IOrdersAPI.class);
    }

    @Override
    public @Nullable INametagAPI nametag() {
        return publicApi(fr.elias.oessentials.api.INametagAPI.class);
    }

    @Override
    public @Nullable IChatAPI chat() {
        return publicApi(fr.elias.oessentials.api.IChatAPI.class);
    }

    @Override
    public @Nullable IAuctionHouseAPI auctionHouse() {
        return publicApi(fr.elias.oessentials.api.IAuctionHouseAPI.class);
    }

    @Override
    public @Nullable ITradeAPI trade() {
        return publicApi(fr.elias.oessentials.api.ITradeAPI.class);
    }

    @Override
    public @Nullable IShopAPI shop() {
        return publicApi(fr.elias.oessentials.api.IShopAPI.class);
    }

    @Override public @Nullable IAfkAPI afk() { return publicApi(fr.elias.oessentials.api.IAfkAPI.class); }
    @Override public @Nullable IBossBarAPI bossBar() { return publicApi(fr.elias.oessentials.api.IBossBarAPI.class); }
    @Override public @Nullable IClearLagAPI clearLag() { return publicApi(fr.elias.oessentials.api.IClearLagAPI.class); }
    @Override public @Nullable ICommandControlAPI commandControl() { return publicApi(fr.elias.oessentials.api.ICommandControlAPI.class); }
    @Override public @Nullable ICrossServerAPI crossServer() { return publicApi(fr.elias.oessentials.api.ICrossServerAPI.class); }
    @Override public @Nullable ICustomCraftAPI customCraft() { return publicApi(fr.elias.oessentials.api.ICustomCraftAPI.class); }
    @Override public @Nullable IDailyAPI daily() { return publicApi(fr.elias.oessentials.api.IDailyAPI.class); }
    @Override public @Nullable IEconomyAPI economy() { return publicApi(fr.elias.oessentials.api.IEconomyAPI.class); }
    @Override public @Nullable IEnderChestAPI enderChest() { return publicApi(fr.elias.oessentials.api.IEnderChestAPI.class); }
    @Override public @Nullable IHologramsAPI holograms() { return publicApi(fr.elias.oessentials.api.IHologramsAPI.class); }
    @Override public @Nullable IKitsAPI kits() { return publicApi(fr.elias.oessentials.api.IKitsAPI.class); }
    @Override public @Nullable IJumpPadsAPI jumpPads() { return publicApi(fr.elias.oessentials.api.IJumpPadsAPI.class); }
    @Override public @Nullable IJailAPI jail() { return publicApi(fr.elias.oessentials.api.IJailAPI.class); }
    @Override public @Nullable IMaintenanceAPI maintenance() { return publicApi(fr.elias.oessentials.api.IMaintenanceAPI.class); }
    @Override public @Nullable ISpawnAPI spawn() { return publicApi(fr.elias.oessentials.api.ISpawnAPI.class); }
    @Override public @Nullable ITeleportAPI teleport() { return publicApi(fr.elias.oessentials.api.ITeleportAPI.class); }
    @Override public @Nullable IWarpsAPI warps() { return publicApi(fr.elias.oessentials.api.IWarpsAPI.class); }
    @Override public @Nullable ISellGuiAPI sellGui() { return publicApi(fr.elias.oessentials.api.ISellGuiAPI.class); }
    @Override public @Nullable IPunishmentAPI punishment() { return publicApi(fr.elias.oessentials.api.IPunishmentAPI.class); }
    @Override public @Nullable IPortalsAPI portals() { return publicApi(fr.elias.oessentials.api.IPortalsAPI.class); }
    @Override public @Nullable IPlayerWarpsAPI playerWarps() { return publicApi(fr.elias.oessentials.api.IPlayerWarpsAPI.class); }
    @Override public @Nullable IPlayerVaultsAPI playerVaults() { return publicApi(fr.elias.oessentials.api.IPlayerVaultsAPI.class); }
    @Override public @Nullable ICommandToggleAPI commandToggle() { return publicApi(fr.elias.oessentials.api.ICommandToggleAPI.class); }
    @Override public @Nullable IInteractiveCommandsAPI interactiveCommands() { return publicApi(fr.elias.oessentials.api.IInteractiveCommandsAPI.class); }
    @Override public @Nullable IBackAPI back() { return publicApi(fr.elias.oessentials.api.IBackAPI.class); }
    @Override public @Nullable IDeathBackAPI deathBack() { return publicApi(fr.elias.oessentials.api.IDeathBackAPI.class); }
    @Override public @Nullable IIgnoreAPI ignore() { return publicApi(fr.elias.oessentials.api.IIgnoreAPI.class); }
    @Override public @Nullable IMailAPI mail() { return publicApi(fr.elias.oessentials.api.IMailAPI.class); }
    @Override public @Nullable IPlaytimeAPI playtime() { return publicApi(fr.elias.oessentials.api.IPlaytimeAPI.class); }
    @Override public @Nullable IScoreboardAPI scoreboard() { return publicApi(fr.elias.oessentials.api.IScoreboardAPI.class); }
    @Override public @Nullable IShardsAPI shards() { return publicApi(fr.elias.oessentials.api.IShardsAPI.class); }
    @Override public @Nullable ITempFlyAPI tempFly() { return publicApi(fr.elias.oessentials.api.ITempFlyAPI.class); }
    @Override public @Nullable IWarningsAPI warnings() { return publicApi(fr.elias.oessentials.api.IWarningsAPI.class); }
    @Override public @Nullable IAutoRebootAPI autoReboot() { return publicApi(fr.elias.oessentials.api.IAutoRebootAPI.class); }
    @Override public @Nullable IAliasesAPI aliases() { return publicApi(fr.elias.oessentials.api.IAliasesAPI.class); }
    @Override public @Nullable IVanishAPI vanish() { return publicApi(fr.elias.oessentials.api.IVanishAPI.class); }
    @Override public @Nullable IFreezeAPI freeze() { return publicApi(fr.elias.oessentials.api.IFreezeAPI.class); }

    /** Each module publishes its part of the API; null while that module is not running. */
    private <T> T publicApi(Class<T> api) {
        var apis = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getPublicApis);
        return apis == null ? null : apis.get(api);
    }
}
