
package com.skyblockexp.ezauction.service;

import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import com.skyblockexp.ezauction.history.AuctionTransactionHistoryService;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles expiry task scheduling and purging of expired listings/orders.
 */
public class AuctionExpiryService {
    private final JavaPlugin plugin;
    private final Map<String, AuctionListing> listings;
    private final Map<String, AuctionOrder> orders;
    private final AuctionPersistenceManager persistenceManager;
    private final AuctionNotificationService notificationService;
    private final AuctionTransactionHistoryService transactionHistoryService;
    private final AuctionClaimService claimService;
    private final AuctionTransactionService transactionService;
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private BukkitTask expiryTask;

    public AuctionExpiryService(JavaPlugin plugin,
                               Map<String, AuctionListing> listings,
                               Map<String, AuctionOrder> orders,
                               AuctionPersistenceManager persistenceManager,
                               AuctionNotificationService notificationService,
                               AuctionTransactionHistoryService transactionHistoryService,
                               AuctionClaimService claimService,
                               AuctionTransactionService transactionService,
                               Map<UUID, List<ItemStack>> pendingReturns) {
        this.plugin = plugin;
        this.listings = listings;
        this.orders = orders;
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
        this.transactionHistoryService = transactionHistoryService;
        this.claimService = claimService;
        this.transactionService = transactionService;
        this.pendingReturns = pendingReturns;
    }

    public void enable() {
        startExpiryTask();
    }

    public void disable() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        if (persistenceManager != null) {
            persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
            persistenceManager.saveReturns(pendingReturns);
            persistenceManager.awaitPersistenceCompletion();
            persistenceManager.setStorageReady(false);
        }
    }

    private void startExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
        }
        expiryTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::purgeExpiredEntries, 20L, 20L * 60L);
    }

    public void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Map.Entry<String, AuctionListing>> listingIterator = listings.entrySet().iterator();
        while (listingIterator.hasNext()) {
            Map.Entry<String, AuctionListing> entry = listingIterator.next();
            AuctionListing listing = entry.getValue();
            if (listing.expiryEpochMillis() < now) {
                listingIterator.remove();
                claimService.returnListingItem(listing, pendingReturns);
                notificationService.notifySellerExpiry(listing);
                transactionHistoryService.recordListingTransactionHistory(listing, null);
                changed = true;
            }
        }
        Iterator<Map.Entry<String, AuctionOrder>> orderIterator = orders.entrySet().iterator();
        while (orderIterator.hasNext()) {
            Map.Entry<String, AuctionOrder> entry = orderIterator.next();
            AuctionOrder order = entry.getValue();
            if (order.expiryEpochMillis() < now) {
                orderIterator.remove();
                transactionService.refundOrderBuyer(order.buyerId(), order.reservedAmount());
                notificationService.notifyOrderExpiry(order);
                transactionHistoryService.recordOrderTransactionHistory(order, order.buyerId(), null, null);
                changed = true;
            }
        }
        if (changed) {
            persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        }
    }
}
