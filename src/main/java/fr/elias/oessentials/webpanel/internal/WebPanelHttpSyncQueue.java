package fr.elias.oessentials.webpanel.internal;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Limits the number of concurrent player HTTP syncs without blocking Bukkit's scheduler.
 * Multiple updates for the same UUID are coalesced into the newest pending snapshot.
 * Updates arriving during an in-flight send are processed after it finishes.
 */
final class WebPanelHttpSyncQueue implements AutoCloseable {
    private static final int MAX_CONCURRENT_REQUESTS = 3;

    record Snapshot(UUID uuid, String name, String dataJson) {}

    private final Consumer<Snapshot> sender;
    private final Logger log;
    private final ExecutorService executor;
    private final ConcurrentHashMap<UUID, Snapshot> pending = new ConcurrentHashMap<>();
    private final Set<UUID> queuedOrSending = ConcurrentHashMap.newKeySet();
    private final Queue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger activeWorkers = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    WebPanelHttpSyncQueue(Consumer<Snapshot> sender, Logger log) {
        this.sender = sender;
        this.log = log;
        this.executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS, task -> {
            Thread thread = new Thread(task, "OEssentials-WebPanel-Sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    void offer(UUID uuid, String name, String dataJson) {
        if (closed.get()) return;
        pending.put(uuid, new Snapshot(uuid, name, dataJson));
        if (queuedOrSending.add(uuid)) queue.offer(uuid);
        scheduleWorkers();
    }

    private void scheduleWorkers() {
        while (!closed.get() && !queue.isEmpty()) {
            int count = activeWorkers.get();
            if (count >= MAX_CONCURRENT_REQUESTS) return;
            if (!activeWorkers.compareAndSet(count, count + 1)) continue;
            try {
                executor.execute(this::drain);
            } catch (RejectedExecutionException ignored) {
                activeWorkers.decrementAndGet();
                return;
            }
        }
    }

    private void drain() {
        try {
            UUID uuid;
            while (!closed.get() && (uuid = queue.poll()) != null) {
                try {
                    Snapshot snapshot = pending.remove(uuid);
                    if (snapshot != null) sender.accept(snapshot);
                } catch (RuntimeException ex) {
                    log.log(Level.WARNING, "[WebPanel] Unexpected player sync error", ex);
                } finally {
                    queuedOrSending.remove(uuid);
                    // If the player changed while the request was in flight,
                    // send that newer snapshot next; never run two sends for one UUID at once.
                    if (!closed.get() && pending.containsKey(uuid) && queuedOrSending.add(uuid)) {
                        queue.offer(uuid);
                    }
                }
            }
        } finally {
            activeWorkers.decrementAndGet();
            if (!closed.get() && !queue.isEmpty()) scheduleWorkers();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        pending.clear();
        queue.clear();
        executor.shutdownNow();
    }
}
