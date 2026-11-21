package fr.elias.oessentials.inventory.internal.auctionhouse.hooks;

import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuctionPlaceholders extends PlaceholderExpansion {

    private final AuctionHouseModule module;

    public AuctionPlaceholders(AuctionHouseModule module) { this.module = module; }

    @Override public @NotNull String getIdentifier() { return "oreoah"; }
    @Override public @NotNull String getAuthor()     { return "OEssentials"; }
    @Override public @NotNull String getVersion()    { return "1.0.0"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String id) {
        if (player == null || !module.enabled()) return "";
        String key = id.toLowerCase(java.util.Locale.ROOT);
        return switch (key) {
            case "active_listings"  -> String.valueOf(module.getAllActiveAuctions().size());
            case "player_listings"  -> String.valueOf(module.getPlayerActiveListings(player.getUniqueId()).size());
            case "player_sales"     -> String.valueOf(module.getPlayerSold(player.getUniqueId()).size());
            case "player_expired"   -> String.valueOf(module.getPlayerExpired(player.getUniqueId()).size());
            case "player_profit"    -> amount(player, "vault", true);
            case "player_expend"    -> amount(player, "vault", false);
            default -> {
                if (key.startsWith("player_profit_")) yield amount(player, key.substring("player_profit_".length()), true);
                if (key.startsWith("player_expend_")) yield amount(player, key.substring("player_expend_".length()), false);
                yield null;
            }
        };
    }

    private String amount(Player player, String currency, boolean profit) {
        var stats = module.getPlayerFinancialTotals(player.getUniqueId()).get(currency);
        double value = stats == null ? 0.0 : (profit ? stats.profit() : stats.expend());
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}