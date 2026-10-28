package fr.elias.oessentials.trade.internal.cross;

import fr.elias.oessentials.messaging.api.ItemStacksCodec;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

/**
 * The menu one player sees during a cross-server trade: their own offer on the left, the
 * partner's on the right. It only displays the trade record; every click is cancelled and
 * handled by {@link CrossTradeService}, so no item ever moves through the menu itself.
 */
public final class CrossTradeMenu implements InventoryHolder {
    static final int[] OWN_SLOTS = {19, 20, 21, 28, 29, 30, 37, 38, 39};
    static final int[] PARTNER_SLOTS = {23, 24, 25, 32, 33, 34, 41, 42, 43};
    static final int CONFIRM_SLOT = 47;
    static final int CANCEL_SLOT = 49;
    static final int PARTNER_STATUS_SLOT = 51;

    final UUID tradeId;
    final UUID viewer;
    final CrossTradeStore.Side side;
    private final Inventory inventory;
    /** Set while the plugin closes the menu itself, so the close does not cancel the trade. */
    boolean closingByPlugin;

    CrossTradeMenu(UUID tradeId, UUID viewer, CrossTradeStore.Side side, String partnerName) {
        this.tradeId = tradeId;
        this.viewer = viewer;
        this.side = side;
        String title = Lang.get("trade.cross.title", "Trade with %name%").replace("%name%", partnerName);
        this.inventory = Bukkit.createInventory(this, 54, title.length() > 32 ? title.substring(0, 32) : title);
        drawFrame(partnerName);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    int ownIndexOf(int rawSlot) {
        for (int i = 0; i < OWN_SLOTS.length; i++) if (OWN_SLOTS[i] == rawSlot) return i;
        return -1;
    }

    private void drawFrame(String partnerName) {
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inventory.setItem(i, pane);
        for (int s : OWN_SLOTS) inventory.setItem(s, null);
        for (int s : PARTNER_SLOTS) inventory.setItem(s, null);
        ItemStack divider = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        inventory.setItem(22, divider);
        inventory.setItem(31, divider);
        inventory.setItem(40, divider);
        inventory.setItem(4, named(Material.BOOK,
                Lang.get("trade.cross.help", "&eClick items in your inventory to offer them. Click your offer to take it back.")));
        inventory.setItem(1, head(viewer, Lang.get("trade.cross.you", "&aYour offer")));
        inventory.setItem(7, named(Material.PLAYER_HEAD,
                Lang.get("trade.cross.partner", "&b%name%'s offer").replace("%name%", partnerName)));
        inventory.setItem(CANCEL_SLOT, named(Material.BARRIER, Lang.get("trade.cross.cancel", "&cCancel trade")));
    }

    /** Repaints both offers and the confirmation state from the trade record. */
    void render(CrossTradeStore.CrossTrade trade) {
        paint(OWN_SLOTS, trade.offer(side));
        paint(PARTNER_SLOTS, trade.offer(side.other()));
        boolean mine = trade.ready(side);
        boolean theirs = trade.ready(side.other());
        inventory.setItem(CONFIRM_SLOT, named(mine ? Material.LIME_CONCRETE : Material.EMERALD_BLOCK, mine
                ? Lang.get("trade.cross.confirmed", "&aConfirmed &7(click to undo)")
                : Lang.get("trade.cross.confirm", "&aClick to confirm")));
        inventory.setItem(PARTNER_STATUS_SLOT, named(theirs ? Material.LIME_DYE : Material.RED_DYE, theirs
                ? Lang.get("trade.cross.partner-ready", "&aPartner confirmed")
                : Lang.get("trade.cross.partner-not-ready", "&cPartner has not confirmed")));
    }

    private void paint(int[] slots, List<String> offer) {
        for (int i = 0; i < slots.length; i++) {
            String enc = offer == null || i >= offer.size() ? null : offer.get(i);
            inventory.setItem(slots[i], decode(enc));
        }
    }

    static ItemStack decode(String enc) {
        if (enc == null) return null;
        ItemStack[] items = ItemStacksCodec.decodeBase64(enc);
        return items == null || items.length == 0 ? null : items[0];
    }

    static String encode(ItemStack item) {
        return ItemStacksCodec.encodeBase64(new ItemStack[]{item});
    }

    private static ItemStack named(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.color(name));
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack head(UUID owner, String name) {
        ItemStack it = named(Material.PLAYER_HEAD, name);
        if (it.getItemMeta() instanceof SkullMeta sm) {
            Player p = Bukkit.getPlayer(owner);
            if (p != null) sm.setOwningPlayer(p);
            it.setItemMeta(sm);
        }
        return it;
    }
}
