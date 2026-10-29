package fr.elias.oessentials.player.internal.tp.rabbit.packets;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;


public class TpaSummonPacket extends Packet {

    private UUID requesterUuid;
    private String destServer;

    public TpaSummonPacket() {}

    public TpaSummonPacket(UUID requesterUuid, String destServer) {
        this.requesterUuid = requesterUuid;
        this.destServer = destServer;
    }

    public UUID getRequesterUuid() { return requesterUuid; }
    public String getDestServer()  { return destServer; }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(requesterUuid);
        out.writeString(destServer != null ? destServer : "");
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        this.requesterUuid = in.readUUID();
        this.destServer    = in.readString();
    }
}
