package fr.elias.oessentials.webpanel.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebPanelMetricsTest {
    @Test
    void heartbeatSendsMetricsAndPreservesEmptyOnlineList() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<JsonObject> received = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        server.createContext("/api/v1/plugin/heartbeat", exchange -> {
            try (exchange) {
                key.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
                received.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8)).getAsJsonObject());
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.start();
        try {
            WebPanelConfig config = mock(WebPanelConfig.class);
            when(config.getUrl()).thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
            when(config.getApiKey()).thenReturn("test-key");
            WebPanelClient client = new WebPanelClient(config, Logger.getAnonymousLogger());
            JsonObject metrics = new JsonObject();
            metrics.addProperty("tps", 19.5);
            metrics.addProperty("players", 0);
            assertTrue(client.pushHeartbeat(List.of(), metrics));
            assertEquals("test-key", key.get());
            assertEquals(0, received.get().getAsJsonArray("onlineUuids").size());
            assertEquals(metrics, received.get().getAsJsonObject("metrics"));
            assertTrue(client.pushHeartbeat(List.of("550e8400-e29b-41d4-a716-446655440000")));
            assertFalse(received.get().has("metrics"));
            assertEquals(1, received.get().getAsJsonArray("onlineUuids").size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void prometheusOmitsUnavailableMetricsAndUsesGaugeTypes() {
        JsonObject metrics = new JsonObject();
        metrics.addProperty("players", 3);
        metrics.addProperty("memoryUsedMb", 512);
        String text = WebPanelMetrics.render(metrics);
        assertTrue(text.contains("# TYPE oreo_players_online gauge\noreo_players_online 3\n"));
        assertTrue(text.contains("oreo_memory_used_megabytes 512\n"));
        assertFalse(text.contains("oreo_tps"));
        assertFalse(text.contains("NaN"));
    }
}
