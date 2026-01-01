package com.skyblockexp.ezauction.transaction;

import com.skyblockexp.ezauction.HistorySaveDispatcher;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.storage.AuctionHistoryStorage;
import com.skyblockexp.ezauction.util.EconomyUtils;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Stores and loads auction transaction history for each player.
 */
public class AuctionTransactionHistory {
    private static final int MAX_ENTRIES_PER_PLAYER = 25;

    private final JavaPlugin plugin;
    private final AuctionHistoryStorage storage;
    private final Map<UUID, Deque<AuctionTransactionHistoryEntry>> entriesByPlayer = new ConcurrentHashMap<>();
    private HistorySaveDispatcher historySaveDispatcher;
    private boolean storageReady;

    public AuctionTransactionHistory(JavaPlugin plugin, AuctionHistoryStorage storage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public void enable() {
        HistorySaveDispatcher dispatcher = historySaveDispatcher;
        if (dispatcher != null) {
            dispatcher.flushAndShutdown();
            historySaveDispatcher = null;
        }
        storageReady = storage.initialize();
        if (!storageReady) {
            plugin.getLogger().severe("Failed to initialize auction history storage. History will not be persisted.");
            entriesByPlayer.clear();
            return;
        }
        entriesByPlayer.clear();
        Map<UUID, Deque<AuctionTransactionHistoryEntry>> loaded = storage.loadAll();
        if (loaded != null && !loaded.isEmpty()) {
            entriesByPlayer.putAll(loaded);
        }
        historySaveDispatcher = new HistorySaveDispatcher(plugin, storage);
    }

    public void disable() {
        HistorySaveDispatcher dispatcher = historySaveDispatcher;
        historySaveDispatcher = null;
        if (dispatcher != null) {
            dispatcher.flushAndShutdown();
        }
        if (storageReady) {
            try {
                storage.saveAll(entriesByPlayer);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to persist " + EzAuctionPlugin.DISPLAY_NAME + " transaction history to storage.", ex);
            }
        }
        entriesByPlayer.clear();
        storageReady = false;
    }

    /**
     * Records a transaction entry for the provided player.
     */
    public void recordTransaction(AuctionTransactionType type, UUID ownerId, UUID counterpartId, String counterpartName,
            double price, ItemStack item, long timestamp) {
        if (type == null || ownerId == null) {
            return;
        }
        double normalizedPrice = EconomyUtils.normalizeCurrency(price);
        AuctionTransactionHistoryEntry entry = new AuctionTransactionHistoryEntry(
                type,
                counterpartId,
                counterpartName,
                normalizedPrice,
                Math.max(0L, timestamp),
                item);

        Deque<AuctionTransactionHistoryEntry> history = entriesByPlayer.computeIfAbsent(ownerId, key -> new ArrayDeque<>());
        history.addFirst(entry);
        while (history.size() > MAX_ENTRIES_PER_PLAYER) {
            history.removeLast();
        }
        persistPlayerHistory(ownerId, history);
    }

    public void recordOrderFulfillment(UUID buyerId, UUID sellerId, String sellerName, String buyerName, double price,
            ItemStack item, long timestamp) {
        if (buyerId == null || sellerId == null) {
            return;
        }
        ItemStack clonedItem = item != null ? item.clone() : null;
        recordTransaction(
                AuctionTransactionType.BUY,
                buyerId,
                sellerId,
                sellerName,
                price,
                clonedItem,
                timestamp);
        recordTransaction(
                AuctionTransactionType.SELL,
                sellerId,
                buyerId,
                buyerName,
                price,
                clonedItem,
                timestamp);
    }

    /**
     * Returns a read-only copy of a player's transaction history sorted newest first.
     */
    public List<AuctionTransactionHistoryEntry> getHistory(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        Deque<AuctionTransactionHistoryEntry> history = entriesByPlayer.get(playerId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return List.copyOf(history);
    }

    private void persistPlayerHistory(UUID ownerId, Deque<AuctionTransactionHistoryEntry> history) {
        if (!storageReady || ownerId == null) {
            return;
        }
        HistorySaveDispatcher dispatcher = historySaveDispatcher;
        if (dispatcher == null) {
            return;
        }
        dispatcher.enqueue(ownerId, history);
    }
}
