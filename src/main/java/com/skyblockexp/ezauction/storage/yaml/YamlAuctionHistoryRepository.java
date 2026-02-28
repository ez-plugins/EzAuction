package com.skyblockexp.ezauction.storage.yaml;

import com.skyblockexp.ezauction.storage.AuctionHistoryRepository;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;

/**
 * Concrete repository backed by the YAML history storage.
 */
public final class YamlAuctionHistoryRepository implements AuctionHistoryRepository {
    private final AuctionHistoryRepository delegate;

    public YamlAuctionHistoryRepository(YamlAuctionHistoryStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean initialize() {
        return delegate.initialize();
    }

    @Override
    public Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAll() {
        return delegate.loadAll();
    }

    @Override
    public void saveAll(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history) {
        delegate.saveAll(history);
    }

    @Override
    public void savePlayerHistory(UUID playerId, Deque<AuctionTransactionHistoryEntry> history) {
        delegate.savePlayerHistory(playerId, history);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
