package fr.elias.oessentials.shop.internal;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.shop.api.ShopSales;
import fr.elias.oessentials.shop.internal.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.OptionalDouble;

/** Publishes the shop's sell logic as {@link ShopSales} (moved from the web panel). */
public final class ShopSalesAdapter implements ShopSales {
    private final OEssentials plugin;
    private final ShopModule shop;

    public ShopSalesAdapter(OEssentials plugin, ShopModule shop) {
        this.plugin = plugin;
        this.shop = shop;
    }

    @Override public boolean isEnabled() { return shop.isEnabled(); }

    @Override
    public OptionalDouble sellPricePerItem(ItemStack item) {
        if (!shop.isEnabled()) return OptionalDouble.empty();
        ShopItem si = shop.getShopManager().findBestSellItem(item);
        if (si == null || !si.canSell()) return OptionalDouble.empty();
        return OptionalDouble.of(si.getSellPrice() / Math.max(1, si.getAmount()));
    }

    @Override
    public boolean sellFromInventory(Player player, Material mat, int amount, Runnable afterPayout) {
        if (!shop.isEnabled()) return false;

        ShopItem shopItem = shop.getShopManager().findBestSellItem(new ItemStack(mat, 1));
        if (shopItem == null || !shopItem.canSell()) return false;

        // Count how many of this material the player actually has (by type only, ignoring meta)
        int available = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot != null && slot.getType() == mat) available += slot.getAmount();
        }
        int toSell = Math.min(amount, available);
        if (toSell <= 0) return false;

        // Remove items from inventory by material type (use setItem to properly persist)
        int remaining = toSell;
        ItemStack[] sellContents = player.getInventory().getContents();
        for (int i = 0; i < sellContents.length && remaining > 0; i++) {
            ItemStack slot = sellContents[i];
            if (slot == null || slot.getType() != mat) continue;
            int take = Math.min(remaining, slot.getAmount());
            if (take >= slot.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                slot.setAmount(slot.getAmount() - take);
            }
            remaining -= take;
        }
        player.updateInventory();

        // Pay the player using the shop's sell price per item
        double pricePerItem = shopItem.getSellPrice() / Math.max(1, shopItem.getAmount());
        double total = pricePerItem * toSell;

        fr.elias.oessentials.shop.internal.models.Shop owningShop = shop.getShopManager().getShop(shopItem.getShopId());
        String currencyId = owningShop != null ? owningShop.getCurrencyId() : null;
        var cs = (currencyId != null) ? plugin.getCurrencyService() : null;

        if (cs != null) {
            cs.deposit(player.getUniqueId(), currencyId, total)
                    .thenRun(() -> OreScheduler.runForEntity(plugin, player, afterPayout))
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("[WebPanel] Failed custom-currency SELL payout: " + ex.getMessage());
                        return null;
                    });
        } else {
            shop.getEconomy().deposit(player, total);
            afterPayout.run();
        }

        shop.getTransactionLogger().logTransaction(
                player.getName(), "SOLD", toSell, mat.name(), total,
                cs != null ? currencyId : shop.getEconomy().getEconomyName());
        return true;
    }
}
