package fr.elias.oessentials.trade.internal.service;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.trade.internal.config.TradeConfig;
import fr.elias.oessentials.trade.internal.TradeIds;
import fr.elias.oessentials.trade.internal.TradeSession;
import fr.elias.oessentials.trade.internal.pending.InMemoryPendingGrantsDao;
import fr.elias.oessentials.trade.internal.pending.PendingGrantsDao;
import fr.elias.oessentials.trade.internal.ui.TradeMenuRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TradeService implements Listener {

    public static final long INVITE_TTL_SECONDS = 60L;

    private final OEssentials plugin;
    private final TradeConfig cfg;

    private static final class Invite {
        final UUID fromId;
        final String fromName;
        final Instant createdAt = Instant.now();

        Invite(UUID fromId, String fromName) {
            this.fromId = fromId;
            this.fromName = (fromName == null ? "Player" : fromName);
        }

        boolean expired(long ttl) {
            return Instant.now().isAfter(createdAt.plusSeconds(ttl));
        }

        @Override
        public String toString() {
            return "Invite{fromId=" + fromId + ", fromName='" + fromName + "', at=" + createdAt + "}";
        }
    }

    private final Map<UUID, Invite> invites = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TradeSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToSession = new ConcurrentHashMap<>();
    private final TradeMenuRegistry menuRegistry = new TradeMenuRegistry();
    private final PendingGrantsDao pendingGrantsDao;
    private final Set<String> deliveredGrants = ConcurrentHashMap.newKeySet();

    public TradeService(OEssentials plugin, TradeConfig cfg) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.cfg = Objects.requireNonNull(cfg, "cfg");

        PendingGrantsDao dao = null;
        try {
            Method m = plugin.getClass().getMethod("getPendingGrantsDao");
            Object obj = m.invoke(plugin);
            if (obj instanceof PendingGrantsDao) dao = (PendingGrantsDao) obj;
        } catch (Throwable ignored) {}
        if (dao == null) dao = new InMemoryPendingGrantsDao();
        this.pendingGrantsDao = dao;

        OreScheduler.runTimer(
                plugin,
                () -> { try { purgeExpiredInvites(); } catch (Throwable ignored) {} },
                40L, 40L
        );

        log("[TRADE] TradeService init; tradedebug=" + cfg.debugDeep);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public TradeService() {
        this(OEssentials.get(), new TradeConfig(OEssentials.get()));
    }

    private boolean dbg() {
        try {
            return cfg != null && cfg.debugDeep;
        } catch (Throwable t) {
            return false;
        }
    }

    private void log(String s) {
        if (dbg()) plugin.getLogger().info(s);
    }

    public TradeConfig getConfig() {
        return cfg;
    }

    public TradeMenuRegistry getMenuRegistry() {
        return menuRegistry;
    }



    public void sendInvite(Player requester, Player target) {
        log("[TRADE] sendInvite requester=" + safe(requester) + " -> target=" + safe(target));
        if (requester == null || target == null || !target.isOnline()) {
            if (requester != null) requester.sendMessage("§cThat player is not available.");
            log("[TRADE] sendInvite abort (null/offline target).");
            return;
        }

        invites.put(target.getUniqueId(), new Invite(requester.getUniqueId(), requester.getName()));
        log("[TRADE] invite stored receiver=" + target.getUniqueId() + " -> " + invites.get(target.getUniqueId()));

        requester.sendMessage("§7Trade request sent to §b" + target.getName()
                + "§7. Expires in §e" + INVITE_TTL_SECONDS + "s§7.");
        target.sendMessage("§e" + requester.getName() + " §7wants to trade. Type §b/trade "
                + requester.getName() + " §7to accept (expires in §e" + INVITE_TTL_SECONDS + "s§7).");
    }

    public boolean tryAcceptInvite(Player acceptor, String requesterName) {
        log("[TRADE] tryAcceptInvite acceptor=" + safe(acceptor) + " requesterName=" + requesterName);
        if (acceptor == null || requesterName == null || requesterName.isBlank()) return false;

        Invite inv = invites.get(acceptor.getUniqueId());
        log("[TRADE] invite lookup -> " + inv);
        if (inv == null) return false;

        if (inv.expired(INVITE_TTL_SECONDS)) {
            invites.remove(acceptor.getUniqueId());
            acceptor.sendMessage("§cThat trade invite has expired.");
            log("[TRADE] invite expired -> removed");
            return true;
        }

        if (!inv.fromName.equalsIgnoreCase(requesterName)) {
            log("[TRADE] invite exists but name mismatch (have='" + inv.fromName + "')");
            return false;
        }

        Player requesterLocal = Bukkit.getPlayer(inv.fromId);
        if (requesterLocal != null && requesterLocal.isOnline()) {
            invites.remove(acceptor.getUniqueId());
            log("[TRADE] starting LOCAL trade " + requesterLocal.getName() + " <-> " + acceptor.getName());
            startLocalTrade(requesterLocal, acceptor);
            return true;
        }


        invites.remove(acceptor.getUniqueId());
        acceptor.sendMessage("§cThe requester is no longer available.");
        log("[TRADE] accept failed: no broker/messaging or requester unavailable.");
        return true;
    }

    public boolean tryAcceptInviteAny(Player acceptor) {
        Invite inv = invites.get(acceptor.getUniqueId());
        if (inv == null) return false;
        return tryAcceptInvite(acceptor, inv.fromName);
    }

    public void addIncomingInvite(Player receiver, UUID fromId, String fromName) {
        log("[TRADE] addIncomingInvite receiver=" + safe(receiver) + " from=" + fromName + "/" + fromId);
        if (receiver == null || !receiver.isOnline() || fromId == null) return;

        Invite existing = invites.get(receiver.getUniqueId());
        if (existing != null && existing.fromId.equals(fromId)) {
            log("[TRADE] duplicate invite ignored for " + receiver.getName() + " from " + fromName);
            return;
        }

        invites.put(receiver.getUniqueId(), new Invite(fromId, fromName));
        receiver.sendMessage("§e" + (fromName == null ? "Someone" : fromName)
                + " §7wants to trade §8(cross-server)§7. Type §b/trade "
                + (fromName == null ? "player" : fromName) + " §7to accept (expires in §e"
                + INVITE_TTL_SECONDS + "s§7).");
    }

    public void addIncomingInvite(UUID receiverId, UUID fromId, String fromName) {
        if (receiverId == null || fromId == null) return;

        Invite existing = invites.get(receiverId);
        if (existing != null && existing.fromId.equals(fromId)) {
            log("[TRADE] duplicate invite ignored for " + receiverId + " from " + fromName);
            return;
        }

        invites.put(receiverId, new Invite(fromId, fromName));
        Player receiver = Bukkit.getPlayer(receiverId);
        if (receiver != null && receiver.isOnline()) {
            receiver.sendMessage("§e" + (fromName == null ? "Someone" : fromName)
                    + " §7wants to trade §8(cross-server)§7. Type §b/trade "
                    + (fromName == null ? "player" : fromName)
                    + " §7to accept (expires in §e" + INVITE_TTL_SECONDS + "s§7).");
        }
    }

    private void startLocalTrade(Player a, Player b) {
        UUID sid = TradeIds.computeTradeId(a.getUniqueId(), b.getUniqueId());
        log("[TRADE] startLocalTrade sid=" + sid);

        TradeSession sess = sessionsById.computeIfAbsent(sid, __ ->
                buildSession(sid, a.getUniqueId(), a.getName(), b.getUniqueId(), b.getName()));

        playerToSession.put(a.getUniqueId(), sid);
        playerToSession.put(b.getUniqueId(), sid);
        sess.open();

        log("[TRADE] local TradeSession.open() called sid=" + sid);
    }

    private TradeSession buildSession(UUID sid, UUID aId, String aName, UUID bId, String bName) {
        final UUID capturedSid = sid;

        return new TradeSession(
                sid, plugin, cfg, aId, aName, bId, bName,
                (finished) -> {
                    log("[TRADE] onFinish sid=" + capturedSid);
                    finalizeIfBothReady(capturedSid);
                },
                (s, reason) -> {
                    log("[TRADE] onCancel sid=" + capturedSid + " reason=" + reason);
                    cancelSession(capturedSid, reason);
                },
                (s, ver) -> log("[TRADE] state changed sid=" + capturedSid + " ver=" + ver)
        );
    }

    public OEssentials getPlugin() {
        return plugin;
    }

    public TradeSession getSession(UUID sid) {
        return sessionsById.get(sid);
    }

    public UUID getTradeIdByPlayer(UUID playerId) {
        return playerId != null ? playerToSession.get(playerId) : null;
    }

    public void setOfferItem(UUID playerId, int index, ItemStack item) {
        if (playerId == null) return;
        UUID sid = getTradeIdByPlayer(playerId);
        log("[TRADE] setOfferItem editor=" + playerId + " sid=" + sid + " slot=" + index
                + " item=" + (item == null ? "null" : item.getType() + "x" + item.getAmount()));
        if (sid == null) return;

        TradeSession s = getSession(sid);
        if (s == null || s.isUiLocked() || s.isCompleted()) return;

        boolean isA = playerId.equals(s.getAId());
        if (isA) {
            s.setOfferItemA(index, (item == null || item.getType().isAir()) ? null : item.clone());
            s.setReadyA(false);
        } else if (playerId.equals(s.getBId())) {
            s.setOfferItemB(index, (item == null || item.getType().isAir()) ? null : item.clone());
            s.setReadyB(false);
        } else {
            return;
        }
        refreshLocalViewers(s);
    }

    public void toggleReady(UUID playerId) {
        if (playerId == null) return;
        UUID sid = getTradeIdByPlayer(playerId);
        log("[TRADE] toggleReady player=" + playerId + " sid=" + sid);
        if (sid == null) return;

        TradeSession s = getSession(sid);
        if (s == null || s.isUiLocked() || s.isCompleted()) return;

        boolean isA = playerId.equals(s.getAId());
        if (isA) {
            s.setReadyA(!s.isReadyA());
        } else if (playerId.equals(s.getBId())) {
            s.setReadyB(!s.isReadyB());
        } else {
            return;
        }
        refreshLocalViewers(s);

        if (s.isReadyA() && s.isReadyB()) {
            finalizeIfBothReady(sid);
        }
    }

    public void requestCancel(UUID playerId) {
        if (playerId == null) return;
        UUID sid = getTradeIdByPlayer(playerId);
        log("[TRADE] requestCancel by=" + playerId + " sid=" + sid);
        if (sid == null) return;

        TradeSession s = getSession(sid);
        if (s == null) return;

        returnAllAndCleanup(s, "Trade cancelled.");
    }

    public void onViewerClosed(UUID playerId) {
        if (playerId == null) return;
        UUID sid = getTradeIdByPlayer(playerId);
        log("[TRADE] onViewerClosed player=" + playerId + " sid=" + sid);
        if (sid == null) return;

        TradeSession s = getSession(sid);
        if (s == null) return;

        boolean isA = playerId.equals(s.getAId());
        if (isA) {
            returnItemsTo(s.getAId(), s.viewOfferA());
            s.clearOfferA();
            s.setReadyA(false);
        } else if (playerId.equals(s.getBId())) {
            returnItemsTo(s.getBId(), s.viewOfferB());
            s.clearOfferB();
            s.setReadyB(false);
        }
        refreshLocalViewers(s);
        returnAllAndCleanup(s, "Trade closed.");
    }

    private void finalizeIfBothReady(UUID sid) {
        TradeSession s = getSession(sid);
        if (s == null) return;
        // TR7: If session was already closed by returnAllAndCleanup, do not proceed.
        if (s.isClosed()) return;
        if (!s.tryMarkGrantingOnce()) {
            log("[TRADE] finalizeIfBothReady skipped (grant already in progress) sid=" + sid);
            return;
        }
        s.beginClosing();
        s.lockUiNow();
        OreScheduler.run(plugin, () -> {
            try {
                completeTradeLocally(s);
            } catch (Throwable t) {
                plugin.getLogger().warning("[TRADE] finalize failed: " + t.getMessage());
                returnAllAndCleanup(s, "Trade failed, items returned.");
            }
        });
    }

    private void completeTradeLocally(TradeSession s) {
        var itemsA = safeCloneArray(s.viewOfferA());
        var itemsB = safeCloneArray(s.viewOfferB());

        s.beginClosing();
        s.lockUiNow();
        s.clearOfferA();
        s.clearOfferB();

        returnItemsTo(s.getBId(), itemsA);
        returnItemsTo(s.getAId(), itemsB);

        notify(s.getAId(), "§aTrade complete!");
        notify(s.getBId(), "§aTrade complete!");

        closeLocalMenus(s);
        s.markCompleted();
        cleanupSession(s);
    }

    private void returnAllAndCleanup(TradeSession s, String reason) {
        // TR7: Mark the session as fully closed so any queued onFinish callback can detect
        // it has already been cancelled and must not proceed with item grants.
        if (!s.tryClose()) {
            log("[TRADE] returnAllAndCleanup skipped — session already closed sid=" + s.getId());
            return;
        }
        s.beginClosing();
        s.lockUiNow();

        var itemsA = safeCloneArray(s.viewOfferA());
        var itemsB = safeCloneArray(s.viewOfferB());

        s.clearOfferA();
        s.clearOfferB();

        returnItemsTo(s.getAId(), itemsA);
        returnItemsTo(s.getBId(), itemsB);

        if (reason != null) {
            notify(s.getAId(), "§c" + reason);
            notify(s.getBId(), "§c" + reason);
        }

        closeLocalMenus(s);
        s.markCompleted();
        cleanupSession(s);
    }

    private void returnItemsTo(UUID playerId, ItemStack[] items) {
        if (items == null || items.length == 0) return;
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            for (var it : items) {
                if (it == null || it.getType().isAir()) continue;
                safeGiveOrDrop(p, it);
            }
        } else {
            plugin.getLogger().info("[TRADE] Player offline; skipping return (player=" + playerId + ")");
        }
    }

    private void safeGiveOrDrop(Player p, ItemStack it) {
        if (it == null || it.getType() == Material.AIR) return;
        HashMap<Integer, ItemStack> left = p.getInventory().addItem(it);
        if (!left.isEmpty()) {
            left.values().forEach(rem -> p.getWorld().dropItemNaturally(p.getLocation(), rem));
        }
    }

    private ItemStack[] safeCloneArray(ItemStack[] src) {
        if (src == null) return new ItemStack[0];
        var out = new ItemStack[src.length];
        for (int i = 0; i < src.length; i++) {
            var it = src[i];
            out[i] = (it == null || it.getType().isAir()) ? null : it.clone();
        }
        return out;
    }

    private void notify(UUID playerId, String msg) {
        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) p.sendMessage(msg);
    }

    private void closeLocalMenus(TradeSession s) {
        var reg = getMenuRegistry();
        if (reg == null) return;
        try {
            var aMenu = reg.get(s.getAId());
            if (aMenu != null) {
                Player a = Bukkit.getPlayer(s.getAId());
                if (a != null && a.isOnline()) a.closeInventory();
            }
        } catch (Throwable ignored) {}
        try {
            var bMenu = reg.get(s.getBId());
            if (bMenu != null) {
                Player b = Bukkit.getPlayer(s.getBId());
                if (b != null && b.isOnline()) b.closeInventory();
            }
        } catch (Throwable ignored) {}
    }

    private void cleanupSession(TradeSession s) {
        UUID sid = s.getId();
        sessionsById.remove(sid);
        playerToSession.entrySet().removeIf(e -> sid.equals(e.getValue()));
    }

    private void cleanupSessionSilent(TradeSession s) {
        if (s == null) return;
        UUID sid = s.getId();

        try { sessionsById.remove(sid); } catch (Throwable ignored) {}
        try { playerToSession.entrySet().removeIf(e -> sid.equals(e.getValue())); } catch (Throwable ignored) {}
        try { if (menuRegistry != null) menuRegistry.unregister(s.getAId()); } catch (Throwable ignored) {}
        try { if (menuRegistry != null) menuRegistry.unregister(s.getBId()); } catch (Throwable ignored) {}
    }

    private void refreshLocalViewers(TradeSession s) {
        var reg = getMenuRegistry();
        if (reg == null) return;
        try { reg.refreshViewer(s.getAId()); } catch (Throwable ignored) {}
        try { reg.refreshViewer(s.getBId()); } catch (Throwable ignored) {}
    }

    public void cancelTradeFor(UUID playerId, String reason) {
        if (playerId == null) return;
        UUID sid = getTradeIdByPlayer(playerId);
        log("[TRADE] cancelTradeFor player=" + playerId + " sid=" + sid + " reason=" + reason);
        if (sid != null) cancelSession(sid, (reason == null ? "cancelled" : reason));
    }

    public void cancelAll() {
        log("[TRADE] cancelAll()");
        invites.clear();
        sessionsById.values().forEach(TradeSession::closeLocalViewers);
        sessionsById.clear();
        playerToSession.clear();
        try { menuRegistry.closeAll(); } catch (Throwable ignored) {}
        OreScheduler.cancelAll(plugin);
    }

    private void cancelSession(UUID sid, String reason) {
        TradeSession s = sessionsById.remove(sid);
        log("[TRADE] cancelSession sid=" + sid + " hadSession=" + (s != null) + " reason=" + reason);
        if (s == null) return;
        playerToSession.entrySet().removeIf(e -> sid.equals(e.getValue()));
        s.closeLocalViewers();
    }

    public void grantItems(Player player, ItemStack[] items) {
        if (player == null || items == null || items.length == 0) return;

        plugin.getLogger().info("[TRADE] grantItems -> " + player.getName() + " items=" + (items == null ? 0 : items.length));
        List<ItemStack> give = new ArrayList<>();
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir() || it.getAmount() <= 0) continue;
            give.add(it.clone());
        }
        if (give.isEmpty()) return;

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(give.toArray(new ItemStack[0]));

        if (!leftover.isEmpty()) {
            leftover.values().forEach(rem -> {
                if (rem != null && !rem.getType().isAir() && rem.getAmount() > 0) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rem);
                }
            });
        }

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        } catch (Throwable ignored) {}
    }

    public void removeSession(UUID sid) {
        if (sid == null) return;
        TradeSession s = sessionsById.remove(sid);
        playerToSession.entrySet().removeIf(e -> sid.equals(e.getValue()));
        if (s != null) {
            try { if (menuRegistry != null) menuRegistry.unregister(s.getAId()); } catch (Throwable ignored) {}
            try { if (menuRegistry != null) menuRegistry.unregister(s.getBId()); } catch (Throwable ignored) {}
        }
    }

    private void purgeExpiredInvites() {
        if (!dbg()) {
            invites.entrySet().removeIf(e -> e.getValue().expired(INVITE_TTL_SECONDS));
            return;
        }
        int before = invites.size();
        invites.entrySet().removeIf(e -> e.getValue().expired(INVITE_TTL_SECONDS));
        int after = invites.size();
        if (before != after) {
            log("[TRADE] purgeExpiredInvites removed=" + (before - after) + " remaining=" + after);
        }
    }

    private static String safe(Player p) {
        return (p == null ? "null" : p.getName() + "/" + p.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        try {
            var grants = pendingGrantsDao.fetchAndDelete(e.getPlayer().getUniqueId());
            if (grants != null && grants.items != null && grants.items.length > 0) {
                for (ItemStack it : grants.items) {
                    if (it == null || it.getType() == Material.AIR) continue;
                    var leftovers = e.getPlayer().getInventory().addItem(it);
                    leftovers.values().forEach(rem ->
                            e.getPlayer().getWorld().dropItemNaturally(e.getPlayer().getLocation(), rem));
                }
                e.getPlayer().sendMessage("§aYou received items from a completed trade while you were offline.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[TRADE] onJoin grant flush failed: " + t.getMessage());
        }
    }

}