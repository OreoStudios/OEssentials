package fr.elias.oessentials.shop.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.OptionalDouble;

/** Selling items to the server shop outside the shop menus (e.g. from the web panel). */
@ModuleApi("services")
public interface ShopSales {
    boolean isEnabled();

    /** What the shop pays for one of these items, or empty when it does not buy them. */
    OptionalDouble sellPricePerItem(ItemStack item);

    /** The sell price listed in the best matching shop entry (for its whole bundle), or empty. */
    OptionalDouble listedSellPrice(ItemStack item);

    /**
     * Removes up to {@code amount} items of {@code material} from the player's inventory, pays the
     * player in the shop's currency and logs the sale. {@code afterPayout} runs on the player's
     * thread once the balance is updated. Returns {@code false} when nothing was sold.
     */
    boolean sellFromInventory(Player player, Material material, int amount, Runnable afterPayout);
}
