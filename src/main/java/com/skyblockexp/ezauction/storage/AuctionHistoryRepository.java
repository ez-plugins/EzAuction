package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

/**
 * Repository abstraction for auction transaction history persistence.
 *
 * Declares the same persistence methods previously available on the
 * legacy storage interface so concrete implementations can implement
 * the repository contract directly.
 */
public interface AuctionHistoryRepository {
	boolean initialize();

	Map<UUID, Deque<AuctionTransactionHistoryEntry>> loadAll();

	void saveAll(Map<UUID, Deque<AuctionTransactionHistoryEntry>> history);

	void savePlayerHistory(UUID playerId, Deque<AuctionTransactionHistoryEntry> history);

	void close();
}
