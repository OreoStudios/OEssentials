package fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets;


import fr.elias.oessentials.messaging.api.Packet;
import fr.elias.oessentials.messaging.api.FriendlyByteInputStream;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;
import java.util.UUID;

public class PlayerJoinPacket extends Packet {

    private UUID playerId;
    private String playerName;

    public PlayerJoinPacket() {
        this.playerId = null;
        this.playerName = null;
    }

    public PlayerJoinPacket(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    protected void read(FriendlyByteInputStream stream) {
        this.playerId = stream.readUUID();
        this.playerName = stream.readString();
    }

    @Override
    protected void write(FriendlyByteOutputStream stream) {
        stream.writeUUID(playerId);
        stream.writeString(playerName);
    }
}
