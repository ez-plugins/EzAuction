package com.skyblockexp.ezauction.persistence;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.storage.AuctionStorage;
import com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles persistence logic for auction listings, orders, and returns.
 */
public class AuctionPersistenceManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionPersistenceManager.class.getName());
    
    private final AuctionStorage storage;
    private final DistributedAuctionListingStorage distributedStorage;
    private final ExecutorService executor;
    private volatile boolean storageReady = false;
    private final Object persistenceLock = new Object();
    private CompletableFuture<Void> persistenceChain = CompletableFuture.completedFuture(null);


    /**
     * Constructs a new AuctionPersistenceManager.
     *
     * @param storage             The main auction storage backend
     * @param distributedStorage  The distributed storage backend (may be null)
     * @param executor            The executor for async persistence tasks
     */
    public AuctionPersistenceManager(AuctionStorage storage, DistributedAuctionListingStorage distributedStorage, ExecutorService executor) {
        this.storage = storage;
        this.distributedStorage = distributedStorage;
        this.executor = executor;
    }


    /**
     * Checks if the storage backend is ready for operations.
     *
     * @return true if storage is ready, false otherwise
     */
    public boolean isStorageReady() {
        return storageReady;
    }


    /**
     * Sets the storage ready state.
     *
     * @param ready true if storage is ready, false otherwise
     */
    public void setStorageReady(boolean ready) {
        this.storageReady = ready;
    }


    /**
     * Loads all auction data from storage.
     *
     * @return AuctionStorageSnapshot containing listings, orders, and returns, or null if not ready
     */
    public AuctionStorageSnapshot loadFromStorage() {
        if (!storageReady) return null;
        return storage.load();
    }


    /**
     * Persists the current auction listings and orders asynchronously.
     *
     * @param listings The list of active auction listings
     * @param orders   The list of active auction orders
     */
    public void saveListings(List<AuctionListing> listings, List<AuctionOrder> orders) {
        if (!storageReady) return;
        schedulePersistenceTask(() -> storage.saveListings(listings, orders));
    }


    /**
     * Persists the current pending return items for players asynchronously.
     *
     * @param returnsByPlayer Map of player UUIDs to their pending return items
     */
    public void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer) {
        if (!storageReady) return;
        schedulePersistenceTask(() -> storage.saveReturns(returnsByPlayer));
    }


    /**
     * Waits for all scheduled persistence tasks to complete.
     * Blocks the calling thread until completion.
     */
    public void awaitPersistenceCompletion() {
        CompletableFuture<Void> chain;
        synchronized (persistenceLock) {
            chain = persistenceChain;
        }
        try {
            chain.get();
        } catch (Exception ex) {
            com.skyblockexp.ezauction.bootstrap.PluginRegistry registry = EzAuctionPlugin.getStaticRegistry();
            if (registry != null && registry.getConfiguration() != null && registry.getConfiguration().debug()) {
                LOGGER.log(Level.SEVERE, "[EzAuction][ERROR] Persistence completion error", ex);
            }
        }
    }

    private void schedulePersistenceTask(Runnable task) {
        synchronized (persistenceLock) {
            persistenceChain = persistenceChain.thenRunAsync(() -> {
                try {
                    task.run();
                } catch (Exception ex) {
                    com.skyblockexp.ezauction.bootstrap.PluginRegistry registry = EzAuctionPlugin.getStaticRegistry();
                    if (registry != null && registry.getConfiguration() != null && registry.getConfiguration().debug()) {
                        LOGGER.log(Level.SEVERE, "[EzAuction][ERROR] Persistence task error", ex);
                    }
                }
            }, executor);
        }
    }

    // Distributed listing operations

    /**
     * Inserts a new auction listing into the distributed storage backend asynchronously.
     *
     * @param listing The auction listing to insert
     */
    public void insertListing(AuctionListing listing) {
        if (!storageReady || distributedStorage == null || listing == null) return;
        schedulePersistenceTask(() -> distributedStorage.insertListing(listing));
    }


    /**
     * Deletes an auction listing from the distributed storage backend asynchronously.
     *
     * @param listingId The ID of the listing to delete
     */
    public void deleteListing(String listingId) {
        if (!storageReady || distributedStorage == null || listingId == null || listingId.isEmpty()) return;
        schedulePersistenceTask(() -> distributedStorage.deleteListing(listingId));
    }


    /**
     * Attempts to claim a listing in the distributed storage backend.
     *
     * @param listingId The ID of the listing to claim
     * @return true if the claim was successful, false otherwise
     */
    public boolean tryClaimListing(String listingId) {
        if (!storageReady || distributedStorage == null || listingId == null || listingId.isEmpty()) return false;
        try {
            return distributedStorage.tryClaimListing(listingId);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
