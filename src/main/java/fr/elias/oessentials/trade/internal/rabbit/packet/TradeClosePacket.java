package fr.elias.oessentials.trade.internal.rabbit.packet;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;


public final class TradeClosePacket extends Packet {
    private UUID sessionId;
    private UUID grantTo;

    public TradeClosePacket() {}

    public TradeClosePacket(UUID sessionId, UUID grantTo) {
        this.sessionId = sessionId;
        this.grantTo = grantTo;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getGrantTo()   { return grantTo; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(sessionId);
        out.writeUUID(grantTo);
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        sessionId = in.readUUID();
        grantTo   = in.readUUID();
    }
}
