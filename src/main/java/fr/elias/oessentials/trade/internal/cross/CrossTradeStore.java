package fr.elias.oessentials.trade.internal.cross;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

/**
 * Cross-server trades, one MongoDB document each. Every change is a single conditional update,
 * so two servers acting at the same time can never both succeed:
 *
 * <ul>
 *   <li>an offered item lives in the document, not in either server's memory;</li>
 *   <li>any offer change resets both confirmations and bumps {@code ver}, and a confirmation only
 *       applies to the version the player saw;</li>
 *   <li>{@code OPEN -> COMMITTED} requires both confirmations, {@code OPEN -> CANCELLED} only that
 *       the trade is still open; whichever lands first wins;</li>
 *   <li>items are handed out only after flipping {@code delivered*}/{@code returned*} from false to
 *       true, so each side receives them exactly once, on whatever server the player is on.</li>
 * </ul>
 */
public final class CrossTradeStore {
    public static final int SLOTS = 9;

    public enum Side {
        A, B;

        String offer() { return this == A ? "offerA" : "offerB"; }
        String ready() { return this == A ? "readyA" : "readyB"; }
        String delivered() { return this == A ? "deliveredA" : "deliveredB"; }
        String returned() { return this == A ? "returnedA" : "returnedB"; }
        Side other() { return this == A ? B : A; }
    }

    private final MongoCollection<Document> coll;

    public CrossTradeStore(MongoCollection<Document> coll) {
        this.coll = coll;
    }

    /** A new open trade between requester {@code a} and accepter {@code b}. */
    public UUID create(UUID aId, String aName, UUID bId, String bName) {
        UUID id = UUID.randomUUID();
        List<String> empty = new ArrayList<>(Collections.nCopies(SLOTS, (String) null));
        coll.insertOne(new Document("_id", id.toString())
                .append("aId", aId.toString()).append("aName", aName)
                .append("bId", bId.toString()).append("bName", bName)
                .append("state", "OPEN").append("ver", 0L)
                .append("offerA", empty).append("offerB", new ArrayList<>(empty))
                .append("readyA", false).append("readyB", false)
                .append("deliveredA", false).append("deliveredB", false)
                .append("returnedA", false).append("returnedB", false)
                .append("createdAt", System.currentTimeMillis()));
        return id;
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

    /** Trades whose items {@code player} still has to receive (delivery or return). */
    public List<CrossTrade> owedTo(UUID player) {
        String p = player.toString();
        List<CrossTrade> out = new ArrayList<>();
        for (Document d : coll.find(or(
                and(eq("aId", p), eq("state", "COMMITTED"), eq("deliveredA", false)),
                and(eq("bId", p), eq("state", "COMMITTED"), eq("deliveredB", false)),
                and(eq("aId", p), eq("state", "CANCELLED"), eq("returnedA", false)),
                and(eq("bId", p), eq("state", "CANCELLED"), eq("returnedB", false))))) {
            out.add(CrossTrade.of(d));
        }
        return out;
    }

    /** Puts an item (already removed from the player) into a free slot of {@code side}. */
    public boolean putItem(UUID id, Side side, int slot, String encodedItem) {
        String field = side.offer() + "." + slot;
        return coll.updateOne(and(eq("_id", id.toString()), eq("state", "OPEN"), eq(field, null)),
                combine(set(field, encodedItem), set("readyA", false), set("readyB", false), inc("ver", 1L)))
                .getModifiedCount() == 1;
    }

    /** Takes an item back out of {@code side}; returns it (to give back), or {@code null} if it was not there anymore. */
    public String takeItem(UUID id, Side side, int slot) {
        String field = side.offer() + "." + slot;
        Document before = coll.findOneAndUpdate(and(eq("_id", id.toString()), eq("state", "OPEN"), ne(field, null)),
                combine(set(field, null), set("readyA", false), set("readyB", false), inc("ver", 1L)),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
        if (before == null) return null;
        List<String> offer = before.getList(side.offer(), String.class);
        return offer == null || slot >= offer.size() ? null : offer.get(slot);
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
     * Claims what {@code side} is owed: the other side's offer after a commit, its own after a
     * cancel. Returns the encoded items exactly once; {@code null} if already claimed or still open.
     */
    public List<String> claim(UUID id, Side side) {
        Document doc = coll.findOneAndUpdate(and(eq("_id", id.toString()), eq("state", "COMMITTED"), eq(side.delivered(), false)),
                set(side.delivered(), true), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
        if (doc != null) return doc.getList(side.other().offer(), String.class);
        doc = coll.findOneAndUpdate(and(eq("_id", id.toString()), eq("state", "CANCELLED"), eq(side.returned(), false)),
                set(side.returned(), true), new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
        return doc == null ? null : doc.getList(side.offer(), String.class);
    }

    /** Read-only view of a trade document. */
    public record CrossTrade(UUID id, UUID aId, String aName, UUID bId, String bName, String state, long ver,
                             List<String> offerA, List<String> offerB, boolean readyA, boolean readyB) {
        static CrossTrade of(Document d) {
            return new CrossTrade(UUID.fromString(d.getString("_id")),
                    UUID.fromString(d.getString("aId")), d.getString("aName"),
                    UUID.fromString(d.getString("bId")), d.getString("bName"),
                    d.getString("state"), d.get("ver", Number.class).longValue(),
                    d.getList("offerA", String.class), d.getList("offerB", String.class),
                    Boolean.TRUE.equals(d.getBoolean("readyA")), Boolean.TRUE.equals(d.getBoolean("readyB")));
        }

        public boolean open() { return "OPEN".equals(state); }
        public Side sideOf(UUID player) { return player.equals(aId) ? Side.A : player.equals(bId) ? Side.B : null; }
        public List<String> offer(Side s) { return s == Side.A ? offerA : offerB; }
        public boolean ready(Side s) { return s == Side.A ? readyA : readyB; }
        public UUID idOf(Side s) { return s == Side.A ? aId : bId; }
        public String nameOf(Side s) { return s == Side.A ? aName : bName; }
    }
}
