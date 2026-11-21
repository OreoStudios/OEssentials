package fr.elias.oessentials.inventory.internal.api;

import fr.elias.oessentials.api.IAuctionHouseAPI;
import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.inventory.internal.auctionhouse.models.Auction;
import fr.elias.oessentials.inventory.internal.auctionhouse.models.AuctionCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionHouseAPIImpl implements IAuctionHouseAPI {

    private final AuctionHouseModule module;

    public AuctionHouseAPIImpl(AuctionHouseModule module) {
        this.module = module;
    }

    @Override
    public boolean isEnabled() {
        return module.enabled();
    }

    @Override
    public boolean createAuction(@NotNull Player seller, @NotNull ItemStack item,
                                 double price, long durationHours,
                                 @NotNull AuctionCategory category) {
        return module.createAuction(seller, item, price, durationHours, category);
    }

    @Override
    public boolean createAuction(@NotNull Player seller, @NotNull ItemStack item,
                                 double price, long durationHours,
                                 @NotNull AuctionCategory category,
                                 @Nullable String currencyId) {
        return module.createAuction(seller, item, price, durationHours, category, currencyId);
    }

    @Override
    public boolean purchaseAuction(@NotNull Player buyer, @NotNull String auctionId) {
        return module.purchaseAuction(buyer, auctionId);
    }

    @Override
    public boolean cancelAuction(@NotNull Player owner, @NotNull String auctionId) {
        return module.cancelAuction(owner, auctionId);
    }

    @Override
    public void openBrowse(@NotNull Player player) {
        module.openBrowse(player);
    }

    @Override
    public @NotNull List<Auction> getAllActiveAuctions() {
        return module.getAllActiveAuctions();
    }

    @Override
    public @NotNull List<Auction> getAuctionsByCategory(@NotNull AuctionCategory category) {
        return module.getAuctionsByCategory(category);
    }

    @Override
    public @NotNull List<Auction> getPlayerActiveListings(@NotNull UUID playerId) {
        return module.getPlayerActiveListings(playerId);
    }

    @Override
    public @NotNull List<Auction> getPlayerExpired(@NotNull UUID playerId) {
        return module.getPlayerExpired(playerId);
    }

    @Override
    public @NotNull List<Auction> getPlayerSold(@NotNull UUID playerId) {
        return module.getPlayerSold(playerId);
    }

    @Override
    public @NotNull Map<String, FinancialTotals> getPlayerFinancialTotals(@NotNull UUID playerId) {
        return module.getPlayerFinancialTotals(playerId);
    }

