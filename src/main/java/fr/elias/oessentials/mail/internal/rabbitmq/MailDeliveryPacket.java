package fr.elias.oessentials.mail.internal.rabbitmq;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;

/**
 * Sent cross-server when a mail is delivered to a player who is online
 * on a different server. The receiving server notifies the player in chat.
 */
public final class MailDeliveryPacket extends Packet {

    private UUID    targetUuid;
    private String  senderName;
    private boolean hasItem;
    private boolean isBroadcast;

    public MailDeliveryPacket() {}

    public MailDeliveryPacket(UUID targetUuid, String senderName,
                               boolean hasItem, boolean isBroadcast) {
        this.targetUuid  = targetUuid;
        this.senderName  = senderName;
        this.hasItem     = hasItem;
        this.isBroadcast = isBroadcast;
    }

    public UUID    getTargetUuid()  { return targetUuid; }
    public String  getSenderName()  { return senderName; }
    public boolean isHasItem()      { return hasItem; }
    public boolean isBroadcast()    { return isBroadcast; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(targetUuid);
        out.writeString(senderName != null ? senderName : "");
        out.writeBoolean(hasItem);
        out.writeBoolean(isBroadcast);
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        this.targetUuid  = in.readUUID();
        this.senderName  = in.readString();
        this.hasItem     = in.readBoolean();
        this.isBroadcast = in.readBoolean();
    }
}
