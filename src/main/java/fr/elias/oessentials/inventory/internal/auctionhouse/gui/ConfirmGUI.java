package fr.elias.oessentials.inventory.internal.auctionhouse.gui;

import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.inventory.internal.auctionhouse.models.Auction;
import fr.elias.oessentials.inventory.internal.auctionhouse.utils.TimeFormatter;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

import static fr.elias.oessentials.inventory.internal.auctionhouse.gui.BrowseGUI.*;

public class ConfirmGUI implements InventoryProvider {
    private java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> onConfirm;
    private volatile boolean purchased;

    private final AuctionHouseModule module;
    private final Auction auction;

    public ConfirmGUI(AuctionHouseModule module, Auction auction) {
        this.module = module;
        this.auction = auction;
    }

    public static SmartInventory getInventory(AuctionHouseModule module, Auction auction) {
        return SmartInventory.builder()
                .id("oe_ah_confirm_" + auction.getId())
                .provider(new ConfirmGUI(module, auction))
                .manager(module.getPlugin().getInvManager())
                .size(3, 9)
                .title(c(module.getConfig().getGui().getString("confirm.title", "&e&lConfirm Purchase")))
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        var cfg = module.getConfig();
        ItemStack pane = glass(cfg.guiBorder("confirm", Material.GRAY_STAINED_GLASS_PANE));
        for (int i = 0; i < 27; i++) contents.set(i / 9, i % 9, ClickableItem.empty(pane));

        ItemStack display = auction.getItem().clone();
        ItemMeta dm = display.getItemMeta();
        // BUG-09: null guard for ItemMeta
        if (dm != null) {
            dm.setLore(List.of("",
                    c("&7Seller: &e" + auction.getSellerName()),
                    c("&7Price: &a" + module.formatMoney(auction.getPrice())),
                    c("&7Time Left: &e" + TimeFormatter.format(auction.getTimeRemaining())),
                    "", c("&6&lPurchase Confirmation"), ""));
            display.setItemMeta(dm);
        }
        contents.set(1, 4, ClickableItem.empty(display));

        // The handler is kept in a field so update() can refresh the button without losing it.
        // (update() used to replace it with ClickableItem.empty every tick, so the confirm button
        // stopped working one tick after the menu opened and purchases never went through.)
        int confirmSlot = cfg.guiSlot("confirm", "confirm", 11);
        this.onConfirm = e -> {
            if (purchased) return; // one purchase per menu, however fast the clicks
            purchased = true;
            click(player); player.closeInventory();
            module.purchaseAuction(player, auction.getId());
        };
        contents.set(confirmSlot / 9, confirmSlot % 9, ClickableItem.of(confirmBtn(player, cfg), onConfirm));

        int cancelSlot = cfg.guiSlot("confirm", "cancel", 15);
        contents.set(cancelSlot / 9, cancelSlot % 9, ClickableItem.of(
                named(cfg.guiMaterial("confirm", "cancel", Material.RED_WOOL),
                        cfg.guiNameRaw("confirm", "cancel", "&c&l✗ CANCEL")),
                e -> { click(player); BrowseGUI.getInventory(module, auction.getCategory()).open(player); }));

        int infoSlot = cfg.guiSlot("confirm", "info", 22);
        ItemStack info = named(cfg.guiMaterial("confirm", "info", Material.PAPER),
                cfg.guiNameRaw("confirm", "info", "&e&lPurchase Information"));
        ItemMeta im = info.getItemMeta();
        // BUG-09: null guard for ItemMeta
        if (im != null) {
            im.setLore(List.of("",
                    c("&7• Item will be added to your inventory"),
                    c("&7• Money will be deducted from your balance"),
                    c("&7• Seller will receive payment"),
                    c("&7• Transaction cannot be reversed"), ""));
            info.setItemMeta(im);
        }
        contents.set(infoSlot / 9, infoSlot % 9, ClickableItem.empty(info));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // Refresh the balance shown on the button; setting the slot replaces the previous item and
        // handler, so reusing onConfirm does not stack handlers.
        var cfg = module.getConfig();
        int confirmSlot = cfg.guiSlot("confirm", "confirm", 11);
        contents.set(confirmSlot / 9, confirmSlot % 9,
                onConfirm == null ? ClickableItem.empty(confirmBtn(player, cfg)) : ClickableItem.of(confirmBtn(player, cfg), onConfirm));
    }

    private ItemStack confirmBtn(Player p, fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseConfig cfg) {
        ItemStack btn = new ItemStack(cfg.guiMaterial("confirm", "confirm", Material.GREEN_WOOL));
        ItemMeta m = btn.getItemMeta();
        // BUG-09: null guard for ItemMeta
        if (m == null) return btn;
        m.setDisplayName(c(cfg.guiNameRaw("confirm", "confirm", "&a&l✔ CONFIRM PURCHASE")));
        // BUG-08: null check for economy before calling getBalance()
        net.milkbowl.vault.economy.Economy eco = module.getEconomy();
        String balance = eco != null
                ? module.formatMoney(eco.getBalance(p))
                : "§cUnavailable";
        m.setLore(List.of("",
                c("&7You will pay: &a" + module.formatMoney(auction.getPrice())),
                c("&7Your balance: &e" + balance),
                "", c("&a&lClick to confirm!"), ""));
        btn.setItemMeta(m);
        return btn;
    }
}
