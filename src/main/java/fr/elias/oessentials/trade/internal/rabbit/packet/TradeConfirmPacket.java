package fr.elias.oessentials.trade.internal.rabbit.packet;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;

public final class TradeConfirmPacket extends Packet {
    private UUID sessionId;
    private UUID confirmerId;

    public TradeConfirmPacket() { }

    public TradeConfirmPacket(UUID sessionId, UUID confirmerId) {
        this.sessionId = sessionId;
        this.confirmerId = confirmerId;
    }
  //
    public UUID getSessionId()  { return sessionId; }
    public UUID getConfirmerId(){ return confirmerId; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(sessionId);
        out.writeUUID(confirmerId);
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        sessionId   = in.readUUID();
        confirmerId = in.readUUID();
    }
}
