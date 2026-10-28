package fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets;


import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;
import java.util.UUID;

public class PlayerQuitPacket extends Packet {

    private UUID playerId;

    public PlayerQuitPacket() {
        this.playerId = null;
    }

    public PlayerQuitPacket(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    protected void read(FriendlyByteInputStream stream) {
        this.playerId = stream.readUUID();
    }

    @Override
    protected void write(FriendlyByteOutputStream stream) {
        stream.writeUUID(playerId);
    }
}

