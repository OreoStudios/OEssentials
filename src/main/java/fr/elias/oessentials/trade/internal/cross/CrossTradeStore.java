package fr.elias.oessentials.trade.internal.cross;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.elemMatch;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.size;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.pull;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;

/**
 * Cross-server trades, one MongoDB document each. Every change is a single conditional update, so
 * two servers acting at once can never both succeed, and every movement of an item between a
 * player's inventory and the trade has a recorded intermediate step:
 *
 * <ul>
 *   <li><b>staged</b> ({@code stagedA/B}): an item about to leave a player's inventory. It becomes
 *       part of the offer only after the player was saved without it;</li>
 *   <li><b>handout</b> ({@code handoutA/B}): items owed to a player (delivery, return, take-back).
 *       They are removed only after the player was saved with them;</li>
 *   <li>each step carries a token that is also written to the player's saved receipts
 *       ({@code PlayerJournal}); after a crash, the receipt decides whether the step happened.</li>
 * </ul>
 *
 * <p>Any offer change resets both confirmations and bumps {@code ver}; a confirmation applies only
 * to the version the player saw; {@code OPEN -> COMMITTED} needs both, {@code OPEN -> CANCELLED}
 * only that the trade is still open.</p>
 */
public final class CrossTradeStore {
    public static final int SLOTS = 9;

    public enum Side {
        A, B;

        String offer() { return this == A ? "offerA" : "offerB"; }
        String ready() { return this == A ? "readyA" : "readyB"; }
        String settled() { return this == A ? "settledA" : "settledB"; }
        String staged() { return this == A ? "stagedA" : "stagedB"; }
        String handout() { return this == A ? "handoutA" : "handoutB"; }
        String id() { return this == A ? "aId" : "bId"; }
        String alive() { return this == A ? "aliveA" : "aliveB"; }
        Side other() { return this == A ? B : A; }
    }

    private final MongoCollection<Document> coll;

    public CrossTradeStore(MongoCollection<Document> coll) {
        this.coll = coll;
    }

    /** A new open trade between requester {@code a} and accepter {@code b}. */
    public UUID create(UUID aId, String aName, UUID bId, String bName) {
        UUID id = UUID.randomUUID();
        coll.insertOne(new Document("_id", id.toString())
                .append("aId", aId.toString()).append("aName", aName)
                .append("bId", bId.toString()).append("bName", bName)
                .append("state", "OPEN").append("ver", 0L)
                .append("offerA", emptyOffer()).append("offerB", emptyOffer())
                .append("readyA", false).append("readyB", false)
                .append("settledA", false).append("settledB", false)
                .append("stagedA", new ArrayList<>()).append("stagedB", new ArrayList<>())
                .append("handoutA", new ArrayList<>()).append("handoutB", new ArrayList<>())
                .append("createdAt", System.currentTimeMillis()));
        // Heartbeats use the database clock, so servers with drifting clocks agree on staleness.
        coll.updateOne(eq("_id", id.toString()),
                combine(com.mongodb.client.model.Updates.currentDate("aliveA"), com.mongodb.client.model.Updates.currentDate("aliveB")));
        return id;
    }

    private static List<String> emptyOffer() {
        return new ArrayList<>(Collections.nCopies(SLOTS, (String) null));
    }

    public CrossTrade get(UUID id) {
        Document d = coll.find(eq("_id", id.toString())).first();
        return d == null ? null : CrossTrade.of(d);
    }

    /** The open trade {@code player} takes part in, or {@code null}. */
    public CrossTrade openTradeOf(UUID player) {
        Document d = coll.find(and(eq("state", "OPEN"),
                or(eq("aId", player.toString()), eq("bId", player.toString())))).first();
        return d == null ? null : CrossTrade.of(d);
    }

    /** Trades with anything still to settle for {@code player}: staged items, hand-outs, or an unsettled end. */
    public List<CrossTrade> pendingFor(UUID player) {
        String p = player.toString();
        List<CrossTrade> out = new ArrayList<>();
        for (Side s : Side.values()) {
            for (Document d : coll.find(and(eq(s.id(), p), or(
                    Filters.not(size(s.staged(), 0)),
                    Filters.not(size(s.handout(), 0)),
                    and(ne("state", "OPEN"), eq(s.settled(), false)))))) {
                out.add(CrossTrade.of(d));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ offering

    /** Step 1 of offering: records the item as staged for {@code slot} (still in the player's inventory). */
    public boolean stage(UUID id, Side side, int slot, String item, String token) {
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), eq(side.offer() + "." + slot, null),
                        Filters.not(elemMatch(side.staged(), eq("slot", slot)))),
                push(side.staged(), new Document("slot", slot).append("item", item).append("token", token)))
                .getModifiedCount() == 1;
    }

    /** Step 3 of offering (the player was saved without the item): the staged item joins the offer. */
    public boolean promote(UUID id, Side side, int slot, String item, String token) {
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), eq(side.offer() + "." + slot, null),
                        elemMatch(side.staged(), eq("token", token))),
                combine(set(side.offer() + "." + slot, item), pull(side.staged(), new Document("token", token)),
                        set("readyA", false), set("readyB", false), inc("ver", 1L)))
                .getModifiedCount() == 1;
    }

    /** Forgets a staged item that never left the player's saved inventory. */
    public void dropStaged(UUID id, Side side, String token) {
        coll.updateOne(eq("_id", id.toString()), pull(side.staged(), new Document("token", token)));
    }

    /** A staged item that did leave the saved inventory but never joined the offer: owe it back. */
    public boolean stagedToHandout(UUID id, Side side, String stagedToken, String item, String handoutToken) {
        return coll.updateOne(and(eq("_id", id.toString()), elemMatch(side.staged(), eq("token", stagedToken))),
                combine(pull(side.staged(), new Document("token", stagedToken)),
                        push(side.handout(), handout(handoutToken, List.of(item)))))
                .getModifiedCount() == 1;
    }

    /** Takes an offered item back: moves it from the offer to the player's hand-outs, if it is still there. */
    public boolean takeBack(UUID id, Side side, int slot, String expectedItem, String token) {
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), eq(side.offer() + "." + slot, expectedItem)),
                combine(set(side.offer() + "." + slot, null), push(side.handout(), handout(token, List.of(expectedItem))),
                        set("readyA", false), set("readyB", false), inc("ver", 1L)))
                .getModifiedCount() == 1;
    }

    // ------------------------------------------------------------------ outcome

    /** Heartbeat: {@code side}'s player still has this trade open on a running server. */
    public void touch(UUID id, Side side) {
        coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN")),
                com.mongodb.client.model.Updates.currentDate(side.alive()));
    }

    /**
     * OPEN -> CANCELLED when {@code side}'s heartbeat is older than {@code timeoutMs} by the database
     * clock (their server stopped, or their menu never opened). True for the one caller that did it.
     */
    public boolean cancelIfGone(UUID id, Side side, long timeoutMs) {
        Bson stale = Filters.expr(new Document("$lt", java.util.List.of("$" + side.alive(),
                new Document("$subtract", java.util.List.of("$$NOW", timeoutMs)))));
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), stale),
                combine(set("state", "CANCELLED"), set("reason", "partner-unreachable"), set("endedAt", System.currentTimeMillis())))
                .getModifiedCount() == 1;
    }

    /** Confirms (or un-confirms) the offers exactly as they were at version {@code seenVersion}. */
    public boolean setReady(UUID id, Side side, boolean ready, long seenVersion) {
        Bson filter = ready
                ? and(eq("_id", id.toString()), eq("state", "OPEN"), eq("ver", seenVersion))
                : and(eq("_id", id.toString()), eq("state", "OPEN"));
        return coll.updateOne(filter, set(side.ready(), ready)).getModifiedCount() == 1;
    }

    /** OPEN and both confirmed -> COMMITTED. True for the one caller that made the change. */
    public boolean tryCommit(UUID id) {
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), eq("readyA", true), eq("readyB", true)),
                combine(set("state", "COMMITTED"), set("endedAt", System.currentTimeMillis())))
                .getModifiedCount() == 1;
    }

    /** OPEN -> CANCELLED. True for the one caller that made the change. */
    public boolean tryCancel(UUID id, String reason) {
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN")),
                combine(set("state", "CANCELLED"), set("reason", reason), set("endedAt", System.currentTimeMillis())))
                .getModifiedCount() == 1;
    }

    /**
     * After the trade ended, turns what {@code side} is owed into a hand-out, exactly once: the other
     * side's offer after a commit, its own after a cancel. Offers cannot change once the trade ended.
     */
    public boolean settle(UUID id, Side side, String token) {
        CrossTrade t = get(id);
        if (t == null || t.open()) return false;
        List<String> owed = new ArrayList<>();
        for (String s : "COMMITTED".equals(t.state()) ? t.offer(side.other()) : t.offer(side)) if (s != null) owed.add(s);
        return coll.updateOne(and(eq("_id", id.toString()), ne("state", "OPEN"), eq(side.settled(), false)),
                combine(set(side.settled(), true), push(side.handout(), handout(token, owed))))
                .getModifiedCount() == 1;
    }

    /** Removes a hand-out once the player was saved with its items. */
    public void removeHandout(UUID id, Side side, String token) {
        coll.updateOne(eq("_id", id.toString()), pull(side.handout(), new Document("token", token)));
    }

    private static Document handout(String token, List<String> items) {
        return new Document("token", token).append("items", items);
    }

    // ------------------------------------------------------------------ views

    public record Staged(int slot, String item, String token) {}

    public record Handout(String token, List<String> items) {}

    /** Read-only view of a trade document. */
    public record CrossTrade(UUID id, UUID aId, String aName, UUID bId, String bName, String state, long ver,
                             List<String> offerA, List<String> offerB, boolean readyA, boolean readyB,
                             List<Staged> stagedA, List<Staged> stagedB, List<Handout> handoutA, List<Handout> handoutB) {
        static CrossTrade of(Document d) {
            return new CrossTrade(UUID.fromString(d.getString("_id")),
                    UUID.fromString(d.getString("aId")), d.getString("aName"),
                    UUID.fromString(d.getString("bId")), d.getString("bName"),
                    d.getString("state"), d.get("ver", Number.class).longValue(),
                    d.getList("offerA", String.class), d.getList("offerB", String.class),
                    Boolean.TRUE.equals(d.getBoolean("readyA")), Boolean.TRUE.equals(d.getBoolean("readyB")),
                    staged(d, "stagedA"), staged(d, "stagedB"), handouts(d, "handoutA"), handouts(d, "handoutB"));
        }

        private static List<Staged> staged(Document d, String field) {
            List<Staged> out = new ArrayList<>();
            for (Document s : d.getList(field, Document.class, List.of())) {
                out.add(new Staged(s.getInteger("slot"), s.getString("item"), s.getString("token")));
            }
            return out;
        }

        private static List<Handout> handouts(Document d, String field) {
            List<Handout> out = new ArrayList<>();
            for (Document h : d.getList(field, Document.class, List.of())) {
                out.add(new Handout(h.getString("token"), h.getList("items", String.class, List.of())));
            }
            return out;
        }

        public boolean open() { return "OPEN".equals(state); }
        public Side sideOf(UUID player) { return player.equals(aId) ? Side.A : player.equals(bId) ? Side.B : null; }
        public List<String> offer(Side s) { return s == Side.A ? offerA : offerB; }
        public boolean ready(Side s) { return s == Side.A ? readyA : readyB; }
        public String nameOf(Side s) { return s == Side.A ? aName : bName; }
        public List<Staged> staged(Side s) { return s == Side.A ? stagedA : stagedB; }
        public List<Handout> handouts(Side s) { return s == Side.A ? handoutA : handoutB; }
    }
}
