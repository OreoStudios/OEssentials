package fr.elias.oessentials.modgui.internal.cross;

import fr.elias.oessentials.api.ICrossServerAPI;
import fr.elias.oessentials.modgui.internal.cross.ModBridge;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CrossServerAPIImpl implements ICrossServerAPI {
    private final ModBridge bridge;
    public CrossServerAPIImpl(ModBridge bridge) { this.bridge = bridge; }

    @Override public void kill(@NotNull UUID id, @NotNull String name) { bridge.kill(id, name); }
    @Override public void kick(@NotNull UUID id, @NotNull String name, @NotNull String reason) { bridge.kick(id, name, reason); }
    @Override public void ban(@NotNull UUID id, @NotNull String name, @NotNull String reason) { bridge.ban(id, name, reason); }
    @Override public void freezeToggle(@NotNull UUID id, @NotNull String name, long seconds) { bridge.freezeToggle(id, name, seconds); }
    @Override public void vanishToggle(@NotNull UUID id, @NotNull String name) { bridge.vanishToggle(id, name); }
    @Override public void gamemodeCycle(@NotNull UUID id, @NotNull String name) { bridge.gamemodeCycle(id, name); }
    @Override public void heal(@NotNull UUID id, @NotNull String name) { bridge.heal(id, name); }
    @Override public void feed(@NotNull UUID id, @NotNull String name) { bridge.feed(id, name); }
}
