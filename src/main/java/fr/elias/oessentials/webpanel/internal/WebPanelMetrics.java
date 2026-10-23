package fr.elias.oessentials.webpanel.internal;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Samples on the global scheduler; HTTP scrapes only read an immutable text snapshot. */
final class WebPanelMetrics implements AutoCloseable {
    private volatile String exposition = "";
    private HttpServer server;
    private ExecutorService executor;

    JsonObject sample(int players) {
        JsonObject metrics = new JsonObject();
        metrics.addProperty("players", players);
        metrics.addProperty("maxPlayers", Bukkit.getMaxPlayers());
        Runtime runtime = Runtime.getRuntime();
        metrics.addProperty("memoryUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / 1048576L);
        metrics.addProperty("memoryMaxMb", runtime.maxMemory() / 1048576L);
        // Folia has no single server tick rate, and world enumeration is region-owned.
        if (!OreScheduler.isFolia()) {
            try {
                double[] tps = (double[]) Bukkit.getServer().getClass().getMethod("getTPS").invoke(Bukkit.getServer());
                if (tps.length > 0) addFinite(metrics, "tps", Math.min(20, tps[0]));
            } catch (ReflectiveOperationException | UnsupportedOperationException ignored) { }
            try {
                Number mspt = (Number) Bukkit.getServer().getClass().getMethod("getAverageTickTime").invoke(Bukkit.getServer());
                addFinite(metrics, "mspt", mspt.doubleValue());
            } catch (ReflectiveOperationException | UnsupportedOperationException ignored) { }
            int chunks = 0, entities = 0;
            for (World world : Bukkit.getWorlds()) {
                chunks += world.getLoadedChunks().length;
                entities += world.getEntityCount();
            }
            metrics.addProperty("loadedChunks", chunks);
            metrics.addProperty("entities", entities);
        }
        exposition = render(metrics);
        return metrics;
    }

    private static void addFinite(JsonObject metrics, String key, double value) {
        if (Double.isFinite(value) && value >= 0) metrics.addProperty(key, value);
    }

    static String render(JsonObject metrics) {
        StringBuilder out = new StringBuilder();
        String[][] names = {
                {"tps", "tps"}, {"mspt", "tick_duration_milliseconds"},
                {"players", "players_online"}, {"maxPlayers", "players_max"},
                {"memoryUsedMb", "memory_used_megabytes"}, {"memoryMaxMb", "memory_max_megabytes"},
                {"loadedChunks", "loaded_chunks"}, {"entities", "entities"}
        };
        for (String[] pair : names) {
            if (!metrics.has(pair[0])) continue;
            String name = "oreo_" + pair[1];
            out.append("# TYPE ").append(name).append(" gauge\n")
                    .append(name).append(' ').append(metrics.get(pair[0]).getAsNumber()).append('\n');
        }
        return out.toString();
    }

    void start(OEssentials plugin) {
        if (!plugin.getConfig().getBoolean("web-panel.prometheus.enabled", false)) return;
        String host = plugin.getConfig().getString("web-panel.prometheus.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("web-panel.prometheus.port", 9100);
        try {
            if (port < 1 || port > 65535) throw new IllegalArgumentException("port must be 1..65535");
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "Oreo-Prometheus");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/metrics", exchange -> {
                try (exchange) {
                    if (!"/metrics".equals(exchange.getRequestURI().getPath())) {
                        exchange.sendResponseHeaders(404, -1);
                    } else if (!"GET".equals(exchange.getRequestMethod())) {
                        exchange.getResponseHeaders().set("Allow", "GET");
                        exchange.sendResponseHeaders(405, -1);
                    } else {
                        byte[] body = exposition.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
                        exchange.sendResponseHeaders(200, body.length);
                        exchange.getResponseBody().write(body);
                    }
                }
            });
            server.start();
            plugin.getLogger().info("[WebPanel] Prometheus listening on " + host + ":" + port + "/metrics");
        } catch (IOException | IllegalArgumentException ex) {
            close();
            plugin.getLogger().warning("[WebPanel] Prometheus could not start: " + ex.getMessage());
        }
    }

    @Override public void close() {
        if (server != null) { server.stop(0); server = null; }
        if (executor != null) { executor.shutdownNow(); executor = null; }
    }
}
