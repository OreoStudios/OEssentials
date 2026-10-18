package fr.elias.oessentials.tab.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.chat.RemoteChatSession;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.PlayerInfo;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomTablistLayoutTest {
    private PacketEventsAPI<?> previousApi;

    @BeforeEach
    void setupPacketEvents() {
        previousApi = PacketEvents.getAPI();
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class, RETURNS_DEEP_STUBS);
        when(api.getServerManager().getVersion()).thenReturn(ServerVersion.V_1_21_4);
        PacketEvents.setAPI(api);
    }

    @AfterEach
    void restorePacketEvents() {
        PacketEvents.setAPI(previousApi);
    }

    @Test
    void joiningPlayerKeepsProfileTexturesChatAndAllActions() {
        PlayerInfo real = realPlayer();
        UserProfile profile = real.getGameProfile();
        RemoteChatSession chat = real.getChatSession();
        EnumSet<Action> actions = EnumSet.of(Action.ADD_PLAYER, Action.INITIALIZE_CHAT,
                Action.UPDATE_LISTED, Action.UPDATE_GAME_MODE, Action.UPDATE_LATENCY,
                Action.UPDATE_DISPLAY_NAME);
        var packet = new WrapperPlayServerPlayerInfoUpdate(actions.clone(), List.of(real));

        assertTrue(VanillaTabInterceptor.delistRealEntries(packet));

        assertEquals(actions, packet.getActions());
        assertSame(real, packet.getEntries().getFirst());
        assertSame(profile, real.getGameProfile());
        assertEquals("skin-value", real.getGameProfile().getTextureProperties().getFirst().getValue());
        assertSame(chat, real.getChatSession());
        assertEquals(GameMode.CREATIVE, real.getGameMode());
        assertEquals(42, real.getLatency());
        assertEquals(Component.text("Player"), real.getDisplayName());
        assertFalse(real.isListed());
    }

    @Test
    void skinRefreshKeepsAddPlayerWithoutRequiringChatInitialization() {
        var packet = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED), List.of(realPlayer()));
        assertTrue(VanillaTabInterceptor.delistRealEntries(packet));
        assertTrue(packet.getActions().contains(Action.ADD_PLAYER));
        assertEquals(1, packet.getEntries().size());
        assertFalse(packet.getEntries().getFirst().isListed());
    }

    @Test
    void mixedPacketKeepsDecorationsListedWhileDelistingRealPlayer() {
        PlayerInfo real = realPlayer();
        PlayerInfo fake = new PlayerInfo(new UserProfile(PacketTablistManager.colSlotUuid(0, 0), "slot"));
        fake.setListed(true);
        var packet = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED), List.of(fake, real));

        assertTrue(VanillaTabInterceptor.delistRealEntries(packet));
        assertTrue(fake.isListed());
        assertFalse(real.isListed());
        assertEquals(2, packet.getEntries().size());
        assertFalse(VanillaTabInterceptor.delistRealEntries(packet), "Already delisted packets need no rewrite");
    }

    @Test
    void standaloneListingUpdateCannotRelistRealPlayers() {
        var packet = new WrapperPlayServerPlayerInfoUpdate(Action.UPDATE_LISTED, List.of(realPlayer()));
        assertTrue(VanillaTabInterceptor.delistRealEntries(packet));
        assertFalse(packet.getEntries().getFirst().isListed());
    }

    @Test
    void packetsWithoutListingActionStayUntouched() {
        for (Action action : List.of(Action.ADD_PLAYER, Action.INITIALIZE_CHAT, Action.UPDATE_LATENCY)) {
            PlayerInfo real = realPlayer();
            var packet = new WrapperPlayServerPlayerInfoUpdate(action, List.of(real));
            assertFalse(VanillaTabInterceptor.delistRealEntries(packet));
            assertEquals(EnumSet.of(action), packet.getActions());
            assertTrue(real.isListed());
        }
    }

    private static PlayerInfo realPlayer() {
        UserProfile profile = new UserProfile(UUID.randomUUID(), "Player",
                List.of(new TextureProperty("textures", "skin-value", "signature")));
        return new PlayerInfo(profile, true, 42, GameMode.CREATIVE, Component.text("Player"),
                new RemoteChatSession(UUID.randomUUID(), null));
    }
}
