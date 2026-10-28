package fr.elias.oessentials.player.internal.msg;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;

public class CrossServerMsgPacket extends Packet {

    private UUID senderUuid;
    private String senderName;
    private UUID targetUuid;
    private String message;

    public CrossServerMsgPacket() {}

    public CrossServerMsgPacket(UUID senderUuid, String senderName, UUID targetUuid, String message) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.message    = message;
    }

    public UUID getSenderUuid()  { return senderUuid; }
    public String getSenderName() { return senderName; }
    public UUID getTargetUuid()  { return targetUuid; }
    public String getMessage()   { return message; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(senderUuid);
        out.writeString(senderName != null ? senderName : "");
        out.writeUUID(targetUuid);
        out.writeString(message != null ? message : "");
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        this.senderUuid = in.readUUID();
        this.senderName = in.readString();
        this.targetUuid = in.readUUID();
        this.message    = in.readString();
    }
}
