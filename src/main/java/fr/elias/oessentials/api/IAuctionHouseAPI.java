package fr.elias.oessentials.api;

import fr.elias.oessentials.inventory.internal.auctionhouse.models.Auction;
import fr.elias.oessentials.inventory.internal.auctionhouse.models.AuctionCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API for the Auction House module.
 *
 * <p>Obtain via {@link OEssentialsAPI#auctionHouse()}. Returns {@code null} if the
 * auction house module is disabled.
 *
 * <h2>Example — list an item on the auction house programmatically</h2>
 * <pre>{@code
 * IAuctionHouseAPI ah = OEssentialsAPI.get().auctionHouse();
 * if (ah != null && ah.isEnabled()) {
 *     boolean listed = ah.createAuction(player, item, 500.0, 24, AuctionCategory.MISC, "euro");
 * }
 * }</pre>
 */
public interface IAuctionHouseAPI {

    /**
     * Returns {@code true} if the auction house module is active.
     */
    boolean isEnabled();

    /**
     * Lists an item on the auction house using Vault economy.
     *
     * @param seller        the selling player
     * @param item          the item to list
     * @param price         the buy-now price
     * @param durationHours listing duration in hours
     * @param category      the auction category
     * @return {@code true} if the listing was created
     */
    boolean createAuction(@NotNull Player seller,
                          @NotNull ItemStack item,
                          double price,
                          long durationHours,
                          @NotNull AuctionCategory category);

    /**
     * Lists an item on the auction house using a custom OES currency.
     *
     * @param seller        the selling player
     * @param item          the item to list
     * @param price         the buy-now price
     * @param durationHours listing duration in hours
     * @param category      the auction category
     * @param currencyId    OES currency ID (e.g. {@code "euro"}), or {@code null} for Vault
     * @return {@code true} if the listing was created
     */
    boolean createAuction(@NotNull Player seller,
                          @NotNull ItemStack item,
                          double price,
                          long durationHours,
                          @NotNull AuctionCategory category,
                          @Nullable String currencyId);

    /**
     * Processes a purchase of an active auction listing.
     *
     * @param buyer     the buying player
     * @param auctionId the ID of the listing to purchase
     * @return {@code true} if the purchase succeeded
     */
    boolean purchaseAuction(@NotNull Player buyer, @NotNull String auctionId);

    /**
     * Cancels an active listing owned by the given player.
     *
     * @param owner     the listing owner
     * @param auctionId the listing ID
     * @return {@code true} if cancelled successfully
     */
    boolean cancelAuction(@NotNull Player owner, @NotNull String auctionId);

    /**
     * Opens the auction house browse GUI for the given player.
     *
     * @param player the player to open the GUI for
     */
    void openBrowse(@NotNull Player player);

    /**
     * Returns a snapshot of all currently active auction listings.
     */
    @NotNull
    List<Auction> getAllActiveAuctions();

    /**
     * Returns all active listings in a specific category.
     *
     * @param category the auction category to filter by
     */
    @NotNull
    List<Auction> getAuctionsByCategory(@NotNull AuctionCategory category);

    /**
     * Returns all active listings created by the given player.
     *
     * @param playerId the player's UUID
     */
    @NotNull
    List<Auction> getPlayerActiveListings(@NotNull UUID playerId);

    /**
     * Returns the player's expired (unclaimed) listings.
     *
     * @param playerId the player's UUID
     */
    @NotNull
    List<Auction> getPlayerExpired(@NotNull UUID playerId);

    /**
     * Returns the player's sold listings.
     *
     * @param playerId the player's UUID
     */
    @NotNull
    List<Auction> getPlayerSold(@NotNull UUID playerId);

    /** Completed auction totals: net seller proceeds and gross buyer expenses. */
    record FinancialTotals(double profit, double expend) {}

    /** Totals by currency. Key "vault" represents the server's Vault currency. */
    default @NotNull Map<String, FinancialTotals> getPlayerFinancialTotals(@NotNull UUID playerId) {
        return Map.of();
    }
}
