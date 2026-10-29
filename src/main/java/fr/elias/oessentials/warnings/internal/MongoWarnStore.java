package fr.elias.oessentials.warnings.internal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Warnings shared by every server on the network: one document per player,
 * {@code {_id: <uuid>, warnings: [{id, issuer, issuerName, reason, issuedAt, expiresAt}]}}.
 * Each operation reads or updates the document in place, so a warning given on one server is
 * visible on the others immediately.
 */
final class MongoWarnStore {
    private final MongoCollection<Document> coll;

    MongoWarnStore(MongoCollection<Document> coll) {
        this.coll = coll;
    }

    List<WarnService.WarnEntry> load(UUID player) {
        Document doc = coll.find(Filters.eq("_id", player.toString())).first();
        List<WarnService.WarnEntry> out = new ArrayList<>();
        if (doc == null) return out;
        for (Document w : doc.getList("warnings", Document.class, List.of())) {
            try {
                out.add(new WarnService.WarnEntry(
                        w.getString("id"),
                        UUID.fromString(w.getString("issuer")),
                        w.getString("issuerName"),
                        w.getString("reason"),
                        w.getLong("issuedAt"),
                        w.getLong("expiresAt")));
            } catch (RuntimeException ignored) {
                // skip a malformed entry rather than hiding the player's other warnings
            }
        }
        return out;
    }

    void add(UUID player, WarnService.WarnEntry e) {
        coll.updateOne(Filters.eq("_id", player.toString()), Updates.push("warnings", toDocument(e)),
                new UpdateOptions().upsert(true));
    }

    boolean remove(UUID player, String warnId) {
        return coll.updateOne(Filters.eq("_id", player.toString()),
                Updates.pull("warnings", new Document("id", warnId))).getModifiedCount() > 0;
    }

    void clear(UUID player) {
        coll.deleteOne(Filters.eq("_id", player.toString()));
    }

    boolean isEmpty() {
        return coll.countDocuments() == 0;
    }

    private static Document toDocument(WarnService.WarnEntry e) {
        return new Document("id", e.id())
                .append("issuer", e.issuer().toString())
                .append("issuerName", e.issuerName())
                .append("reason", e.reason())
                .append("issuedAt", e.issuedAt())
                .append("expiresAt", e.expiresAt());
    }
}
