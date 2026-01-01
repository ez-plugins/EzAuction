package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

/**
 * Persistence abstraction for auction transaction history entries.
 */
public interface AuctionHistoryStorage extends AutoCloseable {

    boolean initialize();

    Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAll();

    void saveAll(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history);

    void savePlayerHistory(UUID playerId, Deque<AuctionTransactionHistoryEntry> history);

    @Override
    void close();
}
