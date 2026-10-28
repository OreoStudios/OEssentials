package fr.elias.oessentials.messaging.internal.invsee.rabbit.packets;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

import java.util.UUID;


public final class InvseeStatePacket extends Packet {

    private UUID targetId;
    private UUID viewerId;
    private byte[] contentsBytes;

    public InvseeStatePacket() {
    }

    public InvseeStatePacket(UUID targetId, UUID viewerId, byte[] contentsBytes) {
        this.targetId = targetId;
        this.viewerId = viewerId;
        this.contentsBytes = (contentsBytes == null ? new byte[0] : contentsBytes);
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getViewerId() {
        return viewerId;
    }

    public byte[] getContentsBytes() {
        return contentsBytes;
    }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeUUID(targetId);
        out.writeUUID(viewerId);
        writeBlob(out, contentsBytes);
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        targetId = in.readUUID();
        viewerId = in.readUUID();
        contentsBytes = readBlob(in);
    }


    private static void writeBlob(FriendlyByteOutputStream out, byte[] data) {
        int n = (data == null ? 0 : data.length);
        out.writeInt(n);
        for (int i = 0; i < n; i++) {
            out.writeByte(data[i]);
        }
    }

    private static byte[] readBlob(FriendlyByteInputStream in) {
        int n = Math.max(0, in.readInt());
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = in.readByte();
        }
        return b;
    }
}
