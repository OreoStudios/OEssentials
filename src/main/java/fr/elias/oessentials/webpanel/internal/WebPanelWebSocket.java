package fr.elias.oessentials.webpanel.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;

/** Outbound authenticated command connection. Player/config uploads use the existing HTTP API. */
final class WebPanelWebSocket implements AutoCloseable {
    private static final int MAX_MESSAGE = 2 * 1024 * 1024;
    private final WebPanelConfig config;
    private final Consumer<JsonObject> onAction;
    private final Logger log;
    private final HttpClient http;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Oreo-Panel-WebSocket");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean connecting = new AtomicBoolean();
    private volatile WebSocket socket;
    private volatile boolean closed;
    private volatile long lastReceived;
    private volatile long nextAttempt;
    private volatile int retrySeconds = 5;

    WebPanelWebSocket(WebPanelConfig config, Consumer<JsonObject> onAction, Logger log) {
        this(config, onAction, log, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    WebPanelWebSocket(WebPanelConfig config, Consumer<JsonObject> onAction, Logger log, HttpClient http) {
        this.config = config;
        this.onAction = onAction;
        this.log = log;
        this.http = http;
    }

    static URI endpoint(String baseUrl) {
        URI base = URI.create(baseUrl);
        String scheme = switch (base.getScheme()) {
            case "https" -> "wss";
            case "http" -> "ws";
            default -> throw new IllegalArgumentException("Panel URL must use http or https");
        };
        if (base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new IllegalArgumentException("Panel URL must not contain credentials, query or fragment");
        }
        return URI.create(scheme + "://" + base.getRawAuthority()
                + base.getRawPath().replaceAll("/+$", "") + "/api/v1/plugin/connect");
    }

    void start() {
        endpoint(config.getUrl()); // Fail fast on invalid configuration.
        timer.scheduleWithFixedDelay(this::tick, 0, 10, TimeUnit.SECONDS);
    }

    void tick() {
        if (closed) return;
        WebSocket current = socket;
        if (current != null) {
            if (System.nanoTime() - lastReceived > TimeUnit.SECONDS.toNanos(60)) {
                current.abort();
                if (socket == current) socket = null;
            } else {
                current.sendText("ping", true).exceptionally(error -> {
                    current.abort();
                    if (socket == current) socket = null;
                    return null;
                });
            }
            return;
        }
        if (System.nanoTime() < nextAttempt || !connecting.compareAndSet(false, true)) return;
        try {
            http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10))
                    .header("X-Api-Key", config.getApiKey())
                    .buildAsync(endpoint(config.getUrl()), new Listener())
                    .whenComplete((ws, error) -> {
                        connecting.set(false);
                        if (closed) { if (ws != null) ws.abort(); return; }
                        if (error != null) {
                            nextAttempt = System.nanoTime() + TimeUnit.SECONDS.toNanos(retrySeconds);
                            retrySeconds = Math.min(60, retrySeconds * 2);
                            log.warning("[WebPanel] WebSocket unavailable; retrying. HTTP sync remains active.");
                        }
                    });
        } catch (RuntimeException error) {
            connecting.set(false);
            nextAttempt = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        }
    }

    private final class Listener implements WebSocket.Listener {
        private final StringBuilder text = new StringBuilder();
        @Override public void onOpen(WebSocket ws) {
            if (closed) { ws.abort(); return; }
            lastReceived = System.nanoTime();
            retrySeconds = 5;
            socket = ws;
            log.info("[WebPanel] WebSocket connected (no RabbitMQ credentials required).");
            ws.request(1);
        }
        @Override public CompletionStage<?> onText(WebSocket ws, CharSequence part, boolean last) {
            if (closed || socket != ws) { ws.abort(); return null; }
            if (text.length() + part.length() > MAX_MESSAGE) { ws.abort(); socket = null; return null; }
            text.append(part);
            if (last) {
                lastReceived = System.nanoTime();
                String message = text.toString();
                text.setLength(0);
                if (!"pong".equals(message)) {
                    try { onAction.accept(JsonParser.parseString(message).getAsJsonObject()); }
                    catch (RuntimeException ex) { log.warning("[WebPanel] Invalid WebSocket command."); }
                }
            }
            ws.request(1);
            return null;
        }
        @Override public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            if (socket == ws) socket = null;
            return null;
        }
        @Override public void onError(WebSocket ws, Throwable error) {
            if (socket == ws) socket = null;
        }
    }

    @Override public void close() {
        closed = true;
        timer.shutdownNow();
        WebSocket current = socket;
        socket = null;
        if (current != null) current.abort();
        http.shutdownNow();
    }
}
