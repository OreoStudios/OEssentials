package fr.elias.oessentials.webpanel.internal;

import com.google.gson.JsonObject;
import fr.elias.oessentials.OEssentials;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebPanelWebSocketTest {
    @Test void explicitWebSocketDoesNotUseCrossServerBrokerCredentials() {
        OEssentials plugin = mock(OEssentials.class);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("rabbitmq.enabled", true);
        yaml.set("rabbitmq.uri", "amqp://private-broker");
        yaml.set("web-panel.transport", "websocket");
        when(plugin.getConfig()).thenReturn(yaml);
        WebPanelConfig config = new WebPanelConfig(plugin);
        assertTrue(config.isWebSocketEnabled());
        assertFalse(config.isAmqpEnabled());
        yaml.set("web-panel.transport", null);
        assertTrue(new WebPanelConfig(plugin).isAmqpEnabled());
        assertEquals(URI.create("wss://panel.example/api/v1/plugin/connect"),
                WebPanelWebSocket.endpoint("https://panel.example/"));
        assertThrows(IllegalArgumentException.class, () -> WebPanelWebSocket.endpoint("https://panel.example/?apiKey=secret"));
    }

    @Test void fragmentsReconnectAndShutdownUseSameCommandHandler() {
        WebPanelConfig config = mock(WebPanelConfig.class);
        when(config.getUrl()).thenReturn("https://panel.example");
        when(config.getApiKey()).thenReturn("oreo_test_secret");
        HttpClient http = mock(HttpClient.class);
        WebSocket.Builder builder = mock(WebSocket.Builder.class, RETURNS_SELF);
        when(http.newWebSocketBuilder()).thenReturn(builder);
        WebSocket socket = mock(WebSocket.class);
        when(socket.sendText(anyString(), eq(true))).thenReturn(CompletableFuture.completedFuture(socket));
        var listeners = new ArrayList<WebSocket.Listener>();
        when(builder.buildAsync(any(), any())).thenAnswer(call -> {
            WebSocket.Listener listener = call.getArgument(1);
            listeners.add(listener);
            listener.onOpen(socket);
            return CompletableFuture.completedFuture(socket);
        });
        var actions = new ArrayList<JsonObject>();
        WebPanelWebSocket client = new WebPanelWebSocket(config, actions::add, Logger.getAnonymousLogger(), http);
        try {
            client.tick();
            verify(builder).header("X-Api-Key", "oreo_test_secret");
            listeners.getFirst().onText(socket, "{\"type\":", false);
            assertTrue(actions.isEmpty());
            listeners.getFirst().onText(socket, "\"CONFIG_FETCH\"}", true);
            assertEquals("CONFIG_FETCH", actions.getFirst().get("type").getAsString());
            listeners.getFirst().onClose(socket, 1001, "restart");
            client.tick();
            assertEquals(2, listeners.size());
            client.tick();
            verify(socket).sendText("ping", true);
        } finally { client.close(); }
        client.tick();
        assertEquals(2, listeners.size());
        verify(http).shutdownNow();
    }
}
