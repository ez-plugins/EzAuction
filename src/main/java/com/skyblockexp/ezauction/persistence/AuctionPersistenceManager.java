package com.skyblockexp.ezauction.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.storage.AuctionListingRepository;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage;
import org.bukkit.inventory.ItemStack;

/**
 * Handles persistence logic for auction listings, orders, and returns.
 */
public class AuctionPersistenceManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionPersistenceManager.class.getName());

    private final AuctionListingRepository storage;
    private final DistributedAuctionListingStorage distributedStorage;
    private final ExecutorService executor;
    private final AuctionConfiguration configuration;
    private volatile boolean storageReady = false;
    private final Object persistenceLock = new Object();
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);

    /**
     * Constructs a new AuctionPersistenceManager.
     *
     * @param storage             The main auction storage backend
     * @param distributedStorage  The distributed storage backend (may be null)
     * @param executor            The executor for async persistence tasks
     * @param configuration       The main auction configuration (for debug/logging)
     */
    public AuctionPersistenceManager(AuctionListingRepository storage, DistributedAuctionListingStorage distributedStorage, ExecutorService executor, AuctionConfiguration configuration) {
        this.storage = storage;
        this.distributedStorage = distributedStorage;
        this.executor = executor;
        this.configuration = configuration;
    }

    public boolean isStorageReady() {
        return storageReady;
    }

    public void setStorageReady(boolean ready) {
        this.storageReady = ready;
    }

    public AuctionStorageSnapshot loadFromStorage() {
        if (!storageReady) return null;
        return storage.load();
    }

    public void saveListings(List<AuctionListing> listings, List<AuctionOrder> orders) {
        if (!storageReady) return;
        schedulePersistenceTask(() -> storage.saveListings(listings, orders));
    }

    public void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer) {
        if (!storageReady) return;
        schedulePersistenceTask(() -> storage.saveReturns(returnsByPlayer));
    }

    public void awaitPersistenceCompletion() {
        CompletableFuture<Void> chain;
        synchronized (persistenceLock) {
            chain = persistenceChain;
        }
        try {
            chain.get();
        } catch (Exception ex) {
            try {
                if (configuration != null && configuration.debug()) {
                    LOGGER.log(Level.SEVERE, "[EzAuction][ERROR] Persistence completion error", ex);
                }
            } catch (Throwable ignored) {}
        }
    }

    private void schedulePersistenceTask(Runnable task) {
        synchronized (persistenceLock) {
            persistenceChain = persistenceChain.thenRunAsync(() -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    try {
                        if (configuration != null && configuration.debug()) {
                            LOGGER.log(Level.SEVERE, "[EzAuction][ERROR] Persistence task error", ex);
                        }
                    } catch (Throwable ignored) {}
                }
            }, executor);
        }
    }

    // Distributed listing operations

    public void insertListing(AuctionListing listing) {
        if (!storageReady || distributedStorage == null || listing == null) return;
        schedulePersistenceTask(() -> distributedStorage.insertListing(listing));
    }

    public void deleteListing(String listingId) {
        if (!storageReady || distributedStorage == null || listingId == null || listingId.isEmpty()) return;
        schedulePersistenceTask(() -> distributedStorage.deleteListing(listingId));
    }

    public boolean tryClaimListing(String listingId) {
        if (!storageReady || distributedStorage == null || listingId == null || listingId.isEmpty()) return false;
        try {
            return distributedStorage.tryClaimListing(listingId);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
