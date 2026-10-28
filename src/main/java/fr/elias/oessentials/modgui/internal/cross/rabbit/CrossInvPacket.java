package fr.elias.oessentials.modgui.internal.cross.rabbit;

import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;

public final class CrossInvPacket extends Packet {
    private String json;

    public CrossInvPacket() {
    }

    public CrossInvPacket(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    protected void read(FriendlyByteInputStream in) {
        this.json = in.readString();
    }

    @Override
    protected void write(FriendlyByteOutputStream out) {
        out.writeString(json != null ? json : "");
    }
}
