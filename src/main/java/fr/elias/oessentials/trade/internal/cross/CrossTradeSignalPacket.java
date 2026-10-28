package fr.elias.oessentials.trade.internal.cross;

import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;
import fr.elias.oessentials.messaging.api.Packet;

import java.util.UUID;

/**
 * Notifications between servers for cross-server trades. They never carry items: the trade
 * record in MongoDB is the only source of truth, so a lost or repeated signal cannot lose or
 * duplicate anything, it only delays a menu refresh (menus also poll the record).
 */
public final class CrossTradeSignalPacket extends Packet {
    public static final byte INVITE = 1;
    public static final byte OPEN = 2;
    public static final byte CHANGED = 3;

    private static final UUID NONE = new UUID(0L, 0L);

    private byte kind;
    private UUID tradeId = NONE;
    private UUID fromId = NONE;
    private String fromName = "";
    private UUID toId = NONE;
    private String toName = "";

    public CrossTradeSignalPacket() {}

    public CrossTradeSignalPacket(byte kind, UUID tradeId, UUID fromId, String fromName, UUID toId, String toName) {
        this.kind = kind;
        this.tradeId = tradeId == null ? NONE : tradeId;
        this.fromId = fromId == null ? NONE : fromId;
        this.fromName = fromName == null ? "" : fromName;
        this.toId = toId == null ? NONE : toId;
        this.toName = toName == null ? "" : toName;
    }

    public byte kind() { return kind; }
    public UUID tradeId() { return tradeId; }
    public UUID fromId() { return fromId; }
    public String fromName() { return fromName; }
    public UUID toId() { return toId; }
    public String toName() { return toName; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeByte(kind);
        out.writeUUID(tradeId);
        out.writeUUID(fromId);
        out.writeString(fromName);
        out.writeUUID(toId);
        out.writeString(toName);
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        kind = in.readByte();
        tradeId = in.readUUID();
        fromId = in.readUUID();
        fromName = in.readString();
        toId = in.readUUID();
        toName = in.readString();
    }
}
