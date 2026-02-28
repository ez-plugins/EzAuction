package com.skyblockexp.ezauction;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Dispatches asynchronous persistence tasks for auction history entries.
 */
public final class HistorySaveDispatcher implements AutoCloseable {

    private static final int DEFAULT_BATCH_SIZE = 25;

    private final JavaPlugin plugin;
    private final com.skyblockexp.ezauction.storage.AuctionHistoryRepository storage;
    private final ExecutorService executor;
    private final ConcurrentHashMap<UUID, Deque<AuctionTransactionHistoryEntry>> pendingUpdates =
            new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<UUID> queue = new ConcurrentLinkedQueue<>();
    private final Set<UUID> enqueued = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final int batchSize;

    public HistorySaveDispatcher(JavaPlugin plugin, com.skyblockexp.ezauction.storage.AuctionHistoryRepository storage) {
        this(plugin, storage, DEFAULT_BATCH_SIZE);
    }

    public HistorySaveDispatcher(JavaPlugin plugin, com.skyblockexp.ezauction.storage.AuctionHistoryRepository storage, int batchSize) {
        this.plugin = plugin;
        this.storage = storage;
        this.batchSize = Math.max(1, batchSize);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, EzAuctionPlugin.DISPLAY_NAME + "-HistorySave");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void enqueue(UUID playerId, Deque<AuctionTransactionHistoryEntry> snapshot) {
        if (playerId == null || snapshot == null || closed.get()) {
            return;
        }
        pendingUpdates.put(playerId, new ArrayDeque<>(snapshot));
        if (enqueued.add(playerId)) {
            queue.offer(playerId);
        }
        triggerDrain();
    }

    private void triggerDrain() {
        if (closed.get()) {
            return;
        }
        if (draining.compareAndSet(false, true)) {
            try {
                executor.execute(this::drainQueue);
            } catch (RejectedExecutionException ex) {
                draining.set(false);
                plugin.getLogger().log(Level.SEVERE, "Unable to schedule auction history persistence task.", ex);
            }
        }
    }

    private void drainQueue() {
        try {
            int processed = 0;
            UUID playerId;
            while (!closed.get() && (playerId = queue.poll()) != null) {
                enqueued.remove(playerId);
                Deque<AuctionTransactionHistoryEntry> snapshot = pendingUpdates.remove(playerId);
                if (snapshot == null || snapshot.isEmpty()) {
                    continue;
                }
                try {
                    storage.savePlayerHistory(playerId, snapshot);
                } catch (RuntimeException ex) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Failed to persist auction history for player " + playerId + '.', ex);
                    pendingUpdates.put(playerId, snapshot);
                    if (enqueued.add(playerId)) {
                        queue.offer(playerId);
                    }
                    break;
                }
                processed++;
                if (processed >= batchSize) {
                    break;
                }
            }
        } finally {
            draining.set(false);
            if (!closed.get() && !queue.isEmpty()) {
                triggerDrain();
            }
        }
    }

    public void flushAndShutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        List<UUID> remaining = new ArrayList<>(pendingUpdates.keySet());
        for (UUID playerId : remaining) {
            Deque<AuctionTransactionHistoryEntry> snapshot = pendingUpdates.remove(playerId);
            if (snapshot == null || snapshot.isEmpty()) {
                continue;
            }
            try {
                storage.savePlayerHistory(playerId, snapshot);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to persist auction history for player " + playerId + " during shutdown.", ex);
            }
        }

        queue.clear();
        enqueued.clear();
    }

    @Override
    public void close() {
        flushAndShutdown();
    }
}
