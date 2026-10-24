package fr.elias.oessentials.webpanel.internal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class WebPanelHttpSyncQueueTest {

    @Test
    void neverRunsMoreThanThreeHttpPlayerSyncsAtOnce() throws Exception {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(12);
        try (WebPanelHttpSyncQueue queue = new WebPanelHttpSyncQueue(snapshot -> {
            int count = inFlight.incrementAndGet();
            maximum.accumulateAndGet(count, Math::max);
            entered.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
                finished.countDown();
            }
        }, Logger.getLogger("test"))) {
            for (int i = 0; i < 12; i++) {
                queue.offer(UUID.randomUUID(), "Bot_" + i, "{}");
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(3, maximum.get());
            release.countDown();
            assertTrue(finished.await(10, TimeUnit.SECONDS));
            assertEquals(3, maximum.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    void coalescesPendingUpdatesForSamePlayerWhileRequestIsInFlight() throws Exception {
        UUID uuid = UUID.randomUUID();
        List<String> snapshots = java.util.Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);

        try (WebPanelHttpSyncQueue queue = new WebPanelHttpSyncQueue(snapshot -> {
            snapshots.add(snapshot.dataJson());
            if (snapshots.size() == 1) {
                firstStarted.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } else {
                secondFinished.countDown();
            }
        }, Logger.getLogger("test"))) {
            queue.offer(uuid, "Player", "first");
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
            queue.offer(uuid, "Player", "superseded");
            queue.offer(uuid, "Player", "latest");
            release.countDown();
            assertTrue(secondFinished.await(5, TimeUnit.SECONDS));
            assertEquals(List.of("first", "latest"), snapshots);
        } finally {
            release.countDown();
        }
    }
}
