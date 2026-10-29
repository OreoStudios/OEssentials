package fr.elias.oessentials.player.internal.tp.rabbit.packets;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;

public class TpaBringPacket extends Packet {
    private UUID requesterUuid;
    private String targetServer;

    public TpaBringPacket() {}
    public TpaBringPacket(UUID requesterUuid, String targetServer) {
        this.requesterUuid = requesterUuid;
        this.targetServer = targetServer;
    }

    public UUID getRequesterUuid() { return requesterUuid; }
    public String getTargetServer() { return targetServer; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(requesterUuid);
        out.writeString(targetServer != null ? targetServer : "");
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        this.requesterUuid = in.readUUID();
        this.targetServer  = in.readString();
    }
}
