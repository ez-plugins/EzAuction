package com.skyblockexp.ezauction.manager;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.service.AuctionListingService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/**
 * Thin manager façade for listing-related operations.
 */
public final class ListingManager {
    private final AuctionListingService listingService;
    private final AuctionPersistenceManager persistenceManager;
    private final AuctionTransactionService transactionService;
    private final AuctionNotificationService notificationService;

    public ListingManager(AuctionListingService listingService, AuctionPersistenceManager persistenceManager, AuctionTransactionService transactionService, AuctionNotificationService notificationService) {
        this.listingService = listingService;
        this.persistenceManager = persistenceManager;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    public AuctionOperationResult createListing(Player seller, ItemStack item, double price, Duration duration) {
        return listingService.createListing(seller, item, price, duration);
    }

    public AuctionOperationResult purchaseListing(Player buyer, String listingId) {
        return listingService.purchaseListing(buyer, listingId);
    }

    public AuctionOperationResult cancelListing(UUID sellerId, String listingId) {
        return listingService.cancelListing(sellerId, listingId);
    }

    public Map<String, AuctionListing> getListings() {
        return listingService.getListings();
    }

    public void saveState(Collection<AuctionListing> listings, Collection<AuctionOrder> orders) {
        persistenceManager.saveListings(List.copyOf(listings), List.copyOf(orders));
    }
}
