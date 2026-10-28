package fr.elias.oessentials.trade.internal.cross;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketBus;
import fr.elias.oessentials.messaging.api.PacketChannels;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.platform.scheduling.OreTask;
import fr.elias.oessentials.shared.Lang;
import fr.elias.oessentials.trade.internal.cross.CrossTradeStore.CrossTrade;
import fr.elias.oessentials.trade.internal.cross.CrossTradeStore.Side;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Trades between players on different servers. The trade record in MongoDB holds the offered
 * items and decides every outcome ({@link CrossTradeStore}); this class turns player actions into
 * record updates, shows the record in each player's menu, and hands out what a player is owed.
 *
 * <p>Leaving or switching servers during a trade cancels it, and anything owed is handed out a few
 * seconds after the player next joins any server (after inventory sync has loaded).</p>
 */
public final class CrossTradeService implements Listener {
    private static final long INVITE_TTL_MS = 60_000L;
    /** Inventory sync applies the saved inventory ~10 ticks after join plus a database read. */
    private static final long CLAIM_DELAY_TICKS = 100L;

    private record Invite(UUID fromId, String fromName, long at) {
        boolean expired() { return System.currentTimeMillis() - at > INVITE_TTL_MS; }
    }

    private final OEssentials plugin;
    private final CrossTradeStore store;
    private final PacketBus bus;
    /** Busy check for the local (same-server) trade system. */
    private final Predicate<UUID> inLocalTrade;

    /** receiver -> invite from a player on another server */
    private final Map<UUID, Invite> incoming = new ConcurrentHashMap<>();
    /** requester -> target they invited (the requester's consent to the trade being opened) */
    private final Map<UUID, Invite> outgoing = new ConcurrentHashMap<>();
    /** viewer -> open menu */
    private final Map<UUID, CrossTradeMenu> menus = new ConcurrentHashMap<>();
    private final OreTask poller;

    public CrossTradeService(OEssentials plugin, CrossTradeStore store, PacketBus bus, Predicate<UUID> inLocalTrade) {
        this.plugin = plugin;
        this.store = store;
        this.bus = bus;
        this.inLocalTrade = inLocalTrade;
        // Signals can be lost; the record is the truth, so open menus re-read it every second.
        this.poller = OreScheduler.runTimer(plugin, this::refreshAll, 20L, 20L);
    }

    public void shutdown() {
        try { poller.cancel(); } catch (Throwable ignored) {}
        for (CrossTradeMenu menu : menus.values()) {
            Player p = Bukkit.getPlayer(menu.viewer);
            if (p != null) closeMenu(p, menu);
        }
        menus.clear();
    }

    // ------------------------------------------------------------------ /trade

    /**
     * Handles {@code /trade <name>} when {@code name} is not on this server: accepts their invite,
     * or invites them. Returns false when the player is not online anywhere.
     */
    public boolean handleCommand(Player player, String targetName) {
        Invite inv = incoming.get(player.getUniqueId());
        if (inv != null && !inv.expired() && inv.fromName().equalsIgnoreCase(targetName)) {
            incoming.remove(player.getUniqueId());
            accept(player, inv);
            return true;
        }
        UUID targetId = lookupOnline(targetName);
        if (targetId == null) return false;
        if (targetId.equals(player.getUniqueId())) {
            Lang.send(player, "trade.self", "<red>You cannot trade with yourself.</red>");
            return true;
        }
        if (busy(player.getUniqueId())) {
            Lang.send(player, "trade.cross.busy-self", "<red>Finish your current trade first.</red>");
            return true;
        }
        outgoing.put(player.getUniqueId(), new Invite(targetId, targetName, System.currentTimeMillis()));
        signal(CrossTradeSignalPacket.INVITE, null, player.getUniqueId(), player.getName(), targetId, targetName);
        Lang.send(player, "trade.cross.invite-sent",
                "<gray>Trade request sent to <aqua>%name%</aqua> on another server. Expires in <yellow>60s</yellow>.</gray>",
                Map.of("name", targetName));
        return true;
    }

    private UUID lookupOnline(String name) {
        try {
            var dir = plugin.getPlayerLocator();
            if (dir == null) return null;
            UUID id = dir.lookupUuidByName(name);
            if (id == null) return null;
            String server = dir.lookupCurrentServer(id);
            return server == null || server.isBlank() ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean busy(UUID player) {
        return inLocalTrade.test(player) || menus.containsKey(player) || store.openTradeOf(player) != null;
    }

    private void accept(Player accepter, Invite inv) {
        if (busy(accepter.getUniqueId())) {
            Lang.send(accepter, "trade.cross.busy-self", "<red>Finish your current trade first.</red>");
            return;
        }
        if (store.openTradeOf(inv.fromId()) != null || lookupOnline(inv.fromName()) == null) {
            Lang.send(accepter, "trade.cross.unavailable", "<red>%name% is no longer available to trade.</red>",
                    Map.of("name", inv.fromName()));
            return;
        }
        UUID tradeId = store.create(inv.fromId(), inv.fromName(), accepter.getUniqueId(), accepter.getName());
        openMenu(accepter, tradeId);
        signal(CrossTradeSignalPacket.OPEN, tradeId, inv.fromId(), inv.fromName(), accepter.getUniqueId(), accepter.getName());
    }

    // ------------------------------------------------------------------ signals

    public void onSignal(CrossTradeSignalPacket pkt) {
        OreScheduler.run(plugin, () -> {
            switch (pkt.kind()) {
                case CrossTradeSignalPacket.INVITE -> {
                    Player target = Bukkit.getPlayer(pkt.toId());
                    if (target == null || !target.isOnline()) return;
                    incoming.put(target.getUniqueId(), new Invite(pkt.fromId(), pkt.fromName(), System.currentTimeMillis()));
                    Lang.send(target, "trade.cross.invite-received",
                            "<yellow>%name%</yellow> <gray>(on another server) wants to trade. Type</gray> <aqua>/trade %name%</aqua> <gray>to accept (60s).</gray>",
                            Map.of("name", pkt.fromName()));
                }
                case CrossTradeSignalPacket.OPEN -> {
                    Player requester = Bukkit.getPlayer(pkt.fromId());
                    if (requester == null || !requester.isOnline()) return;
                    Invite sent = outgoing.get(requester.getUniqueId());
                    // Only open a trade the requester actually asked for.
                    if (sent == null || sent.expired() || !sent.fromId().equals(pkt.toId())) return;
                    outgoing.remove(requester.getUniqueId());
                    openMenu(requester, pkt.tradeId());
                }
                case CrossTradeSignalPacket.CHANGED -> refreshTrade(pkt.tradeId());
                default -> { }
            }
        });
    }

    private void signal(byte kind, UUID tradeId, UUID fromId, String fromName, UUID toId, String toName) {
        try {
            bus.sendPacket(PacketChannels.GLOBAL, new CrossTradeSignalPacket(kind, tradeId, fromId, fromName, toId, toName));
        } catch (Throwable t) {
            plugin.getLogger().warning("[TRADE] Cross-server signal failed (menus will catch up by polling): " + t.getMessage());
        }
    }

    private void changed(UUID tradeId) {
        signal(CrossTradeSignalPacket.CHANGED, tradeId, null, null, null, null);
        refreshTrade(tradeId);
    }

    // ------------------------------------------------------------------ menus

    private void openMenu(Player player, UUID tradeId) {
        CrossTrade trade = store.get(tradeId);
        if (trade == null || !trade.open()) return;
        Side side = trade.sideOf(player.getUniqueId());
        if (side == null) return;
        CrossTradeMenu menu = new CrossTradeMenu(tradeId, player.getUniqueId(), side, trade.nameOf(side.other()));
        menu.render(trade);
        menus.put(player.getUniqueId(), menu);
        player.openInventory(menu.getInventory());
        Lang.send(player, "trade.opening", "<gray>Opening trade GUI with <aqua>%partner%</aqua>…</gray>",
                Map.of("partner", trade.nameOf(side.other()), "name", trade.nameOf(side.other())));
    }

    private void refreshAll() {
        for (CrossTradeMenu menu : List.copyOf(menus.values())) refreshTrade(menu.tradeId);
    }

    /** Re-reads the record: repaints open menus, or finishes the trade for local players once it ended. */
    private void refreshTrade(UUID tradeId) {
        if (tradeId == null) return;
        List<CrossTradeMenu> local = menus.values().stream().filter(m -> m.tradeId.equals(tradeId)).toList();
        if (local.isEmpty()) return;
        CrossTrade trade = store.get(tradeId);
        for (CrossTradeMenu menu : local) {
            Player p = Bukkit.getPlayer(menu.viewer);
            if (p == null) { menus.remove(menu.viewer, menu); continue; }
            if (trade != null && trade.open()) {
                menu.render(trade);
            } else {
                closeMenu(p, menu);
                if (trade != null) {
                    if ("COMMITTED".equals(trade.state())) {
                        Lang.send(p, "trade.complete", "<green>Trade complete! Items received.</green>");
                    } else {
                        Lang.send(p, "trade.cancelled", "<red>Trade cancelled.</red>");
                    }
                }
                claimOwed(p);
            }
        }
    }

    private void closeMenu(Player p, CrossTradeMenu menu) {
        menus.remove(p.getUniqueId(), menu);
        if (p.getOpenInventory().getTopInventory().getHolder() == menu) {
            menu.closingByPlugin = true;
            p.closeInventory();
        }
    }

    // ------------------------------------------------------------------ actions

    private void offer(Player p, CrossTradeMenu menu, int invSlot) {
        ItemStack item = p.getInventory().getItem(invSlot);
        if (item == null || item.getType().isAir()) return;
        CrossTrade trade = store.get(menu.tradeId);
        if (trade == null || !trade.open()) return;
        List<String> mine = trade.offer(menu.side);
        int free = -1;
        for (int i = 0; i < CrossTradeStore.SLOTS; i++) if (mine.get(i) == null) { free = i; break; }
        if (free < 0) {
            Lang.send(p, "trade.cross.full", "<red>Your side of the trade is full.</red>");
            return;
        }
        // Remove first, then record; on failure the item goes straight back.
        ItemStack taken = item.clone();
        p.getInventory().setItem(invSlot, null);
        if (!store.putItem(menu.tradeId, menu.side, free, CrossTradeMenu.encode(taken))) {
            give(p, taken);
        }
        changed(menu.tradeId);
    }

    private void takeBack(Player p, CrossTradeMenu menu, int ownIndex) {
        String enc = store.takeItem(menu.tradeId, menu.side, ownIndex);
        if (enc != null) give(p, CrossTradeMenu.decode(enc));
        changed(menu.tradeId);
    }

    private void toggleConfirm(Player p, CrossTradeMenu menu) {
        CrossTrade trade = store.get(menu.tradeId);
        if (trade == null || !trade.open()) return;
        boolean nowReady = !trade.ready(menu.side);
        if (!store.setReady(menu.tradeId, menu.side, nowReady, trade.ver())) {
            Lang.send(p, "trade.cross.changed", "<yellow>The offers just changed — check them and confirm again.</yellow>");
        } else if (nowReady && store.tryCommit(menu.tradeId)) {
            plugin.getLogger().info("[TRADE] Cross-server trade " + menu.tradeId + " completed.");
        }
        changed(menu.tradeId);
    }

    private void cancel(UUID tradeId, String reason) {
        if (store.tryCancel(tradeId, reason)) changed(tradeId);
    }

    /**
     * Hands out everything {@code p} is owed (deliveries and returns), each exactly once.
     * Never for a player who is not online: a disconnect closes the menu first, and items given
     * to a player who is leaving were lost. They stay owed until the player's next join.
     */
    private void claimOwed(Player p) {
        if (p == null || !p.isOnline()) return;
        for (CrossTrade t : store.owedTo(p.getUniqueId())) {
            if (!p.isOnline()) return;
            Side side = t.sideOf(p.getUniqueId());
            List<String> items = store.claim(t.id(), side);
            if (items == null) continue;
            for (String enc : items) {
                ItemStack it = CrossTradeMenu.decode(enc);
                if (it != null) give(p, it);
            }
        }
    }

    private static void give(Player p, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
    }

    // ------------------------------------------------------------------ events

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof CrossTradeMenu menu)) return;
        e.setCancelled(true); // nothing moves through this menu by itself
        if (!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(menu.viewer)) return;
        int raw = e.getRawSlot();
        if (raw < 0) return;
        if (raw >= e.getView().getTopInventory().getSize()) {
            offer(p, menu, e.getSlot());
        } else if (raw == CrossTradeMenu.CONFIRM_SLOT) {
            toggleConfirm(p, menu);
        } else if (raw == CrossTradeMenu.CANCEL_SLOT) {
            cancel(menu.tradeId, "cancelled");
        } else {
            int own = menu.ownIndexOf(raw);
            if (own >= 0) takeBack(p, menu, own);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof CrossTradeMenu) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof CrossTradeMenu menu) || menu.closingByPlugin) return;
        menus.remove(menu.viewer, menu);
        cancel(menu.tradeId, "closed");
        // Runs a tick later: if the close was part of a disconnect the player is gone by then and
        // keeps the items owed (handed out on their next join).
        if (e.getPlayer() instanceof Player p) OreScheduler.run(plugin, () -> claimOwed(p));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        incoming.remove(id);
        outgoing.remove(id);
        CrossTradeMenu menu = menus.remove(id);
        if (menu != null) cancel(menu.tradeId, "left");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        // After inventory sync has loaded, so handed-out items are not overwritten.
        OreScheduler.runLater(plugin, () -> {
            if (!p.isOnline()) return;
            CrossTrade open = store.openTradeOf(p.getUniqueId());
            if (open != null && !menus.containsKey(p.getUniqueId())) {
                // They left (or switched servers) mid-trade: that trade is over.
                cancel(open.id(), "left");
            }
            claimOwed(p);
        }, CLAIM_DELAY_TICKS);
    }
}
