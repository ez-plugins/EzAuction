package com.skyblockexp.ezauction;

import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.live.LiveAuctionEntry;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.storage.AuctionStorage;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import com.skyblockexp.ezauction.history.AuctionTransactionHistoryService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import com.skyblockexp.ezauction.service.AuctionListingService;
import com.skyblockexp.ezauction.service.AuctionOrderService;
import com.skyblockexp.ezauction.service.AuctionReturnService;
import com.skyblockexp.ezauction.service.AuctionExpiryService;
import com.skyblockexp.ezauction.service.AuctionQueryService;
import com.skyblockexp.ezauction.util.AuctionValidationUtils;

public class AuctionManager {
    // Services
    private final AuctionListingService listingService;
    private final AuctionOrderService orderService;
    private final AuctionReturnService returnService;
    private final AuctionExpiryService expiryService;
    private final AuctionQueryService queryService;
    // Core plugin reference (if needed)
    private final JavaPlugin plugin;
    // Config and resolver for listing limits
    private final AuctionConfiguration configuration;
    private final AuctionListingLimitResolver listingLimitResolver;

    public AuctionManager(JavaPlugin plugin,
                         AuctionListingService listingService,
                         AuctionOrderService orderService,
                         AuctionReturnService returnService,
                         AuctionExpiryService expiryService,
                         AuctionQueryService queryService,
                         AuctionConfiguration configuration,
                         AuctionListingLimitResolver listingLimitResolver) {
        this.plugin = plugin;
        this.listingService = listingService;
        this.orderService = orderService;
        this.returnService = returnService;
        this.expiryService = expiryService;
        this.queryService = queryService;
        this.configuration = configuration;
        this.listingLimitResolver = listingLimitResolver;
    }

    /**
     * Enables the auction manager by loading persisted auction data and starting periodic expiry tasks.
     * This method should be called during plugin startup to initialize the auction system.
     *
     * @throws IllegalStateException if storage initialization fails
     */
    public void enable() {
        expiryService.enable();
    }

    /**
     * Retrieves all active auction listings, sorted by expiry time.
     *
     * @return an unmodifiable list of active {@link AuctionListing} objects
     */
    public List<AuctionListing> listActiveListings() {
        return queryService.listActiveListings();
    }

    /**
     * Retrieves all active auction buy orders, sorted by expiry time.
     *
     * @return an unmodifiable list of active {@link AuctionOrder} objects
     */
    public List<AuctionOrder> listActiveOrders() {
        return queryService.listActiveOrders();
    }

    /**
     * Counts the number of active listings for a specific seller.
     *
     * @param sellerId the UUID of the seller
     * @return the count of active listings for the seller
     */
    public long countActiveListings(UUID sellerId) {
        return queryService.countActiveListings(sellerId);
    }

    /**
     * Counts the number of active buy orders for a specific buyer.
     *
     * @param buyerId the UUID of the buyer
     * @return the count of active orders for the buyer
     */
    public long countActiveOrders(UUID buyerId) {
        return queryService.countActiveOrders(buyerId);
    }

    /**
     * Counts the total number of active auction listings.
     *
     * @return the total count of active listings
     */
    public long countAllActiveListings() {
        return queryService.countAllActiveListings();
    }

    /**
     * Counts the total number of active buy orders.
     *
     * @return the total count of active orders
     */
    public long countAllActiveOrders() {
        return queryService.countAllActiveOrders();
    }

    /**
     * Returns the number of pending return items for a player.
     *
     * @param playerId the UUID of the player
     * @return the number of pending return items
     */
    public int countPendingReturnItems(UUID playerId) {
        return returnService.countPendingReturnItems(playerId);
    }

    /**
     * Resolves the listing limit for a seller based on configuration and limit resolver.
     *
     * @param sellerId the UUID of the seller
     * @return the resolved listing limit
     */
    public int resolveListingLimit(UUID sellerId) {
        return AuctionValidationUtils.resolveListingLimit(sellerId, configuration, listingLimitResolver);
    }

    /**
     * Handles player login events to notify about pending return items.
     *
     * @param player the player logging in
     */
    public void handlePlayerLogin(Player player) {
        returnService.handlePlayerLogin(player);
    }

    /**
     * Allows a player to claim their pending return items from expired or cancelled auctions.
     *
     * @param player the player claiming items
     * @return the result of the claim operation
     */
    public AuctionOperationResult claimReturnItems(Player player) {
        return returnService.claimReturnItems(player);
    }

    /**
     * Helper method to count the total item amount in a list of ItemStacks.
     *
     * @param items the list of ItemStacks
     * @return the total item amount
     */
    // ...existing code...

    /**
     * Disables the auction manager, saving all auction data and stopping periodic expiry tasks.
     * This method should be called during plugin shutdown to ensure data integrity.
     */
    public void disable() {
        expiryService.disable();
    }


    /**
     * Creates a new auction listing for the given seller and item.
     * Handles validation, persistence, notifications, and transaction history.
     *
     * @param seller   the player creating the listing
     * @param item     the item to list
     * @param price    the price for the listing
     * @param duration the duration of the listing
     * @return the result of the listing creation operation
     */
    public AuctionOperationResult createListing(Player seller, ItemStack item, double price, Duration duration) {
        return listingService.createListing(seller, item, price, duration);
    }

    /**
     * Purchases an active auction listing for the given buyer.
     * Handles validation, payment, item delivery, notifications, and transaction history.
     *
     * @param buyer     the player purchasing the listing
     * @param listingId the ID of the listing to purchase
     * @return the result of the purchase operation
     */
    public AuctionOperationResult purchaseListing(Player buyer, String listingId) {
        return listingService.purchaseListing(buyer, listingId);
    }

    /**
     * Cancels an active auction listing for the given seller.
     * Handles item return, deposit refund, notifications, and transaction history.
     *
     * @param sellerId  the UUID of the seller
     * @param listingId the ID of the listing to cancel
     * @return the result of the cancellation operation
     */
    public AuctionOperationResult cancelListing(UUID sellerId, String listingId) {
        return listingService.cancelListing(sellerId, listingId);
    }

    /**
     * Creates a new buy order for the given buyer and item template.
     * Handles validation, fund reservation, notifications, and transaction history.
     *
     * @param buyer          the player creating the order
     * @param template       the item template for the order
     * @param offeredPrice   the total price offered
     * @param duration       the duration of the order
     * @param reservedAmount the amount to reserve from the buyer
     * @return the result of the order creation operation
     */
    public AuctionOperationResult createOrder(Player buyer, ItemStack template, double offeredPrice, Duration duration, double reservedAmount) {
        return orderService.createOrder(buyer, template, offeredPrice, duration, reservedAmount);
    }

    /**
     * Fulfills a buy order by delivering the requested item from the seller to the buyer.
     * Handles validation, payment, item delivery, notifications, and transaction history.
     *
     * @param seller  the player fulfilling the order
     * @param orderId the ID of the order to fulfill
     * @return the result of the fulfillment operation
     */
    public AuctionOperationResult fulfillOrder(Player seller, String orderId) {
        return orderService.fulfillOrder(seller, orderId);
    }

    /**
     * Cancels an active buy order for the given buyer.
     * Handles fund refund, notifications, and transaction history.
     *
     * @param buyerId the UUID of the buyer
     * @param orderId the ID of the order to cancel
     * @return the result of the cancellation operation
     */
    public AuctionOperationResult cancelOrder(UUID buyerId, String orderId) {
        return orderService.cancelOrder(buyerId, orderId);
    }

    /**
     * Starts the periodic expiry task for listings and orders.
     * Should be called after loading auction data to enable automatic expiry handling.
     */
    // ...existing code...

    /**
     * Purges expired auction listings and orders, returning items and refunding funds as needed.
     * Also triggers notifications and transaction history updates for expired entries.
     */
    public void purgeExpiredEntries() {
        expiryService.purgeExpiredEntries();
    }
    
    /**
     * Returns the highest priced active auction listing, or {@code null} if none exist.
     *
     * @return the highest priced active {@link AuctionListing}, or {@code null} if none exist
     */
    public AuctionListing findHighestPricedListing() {
        return queryService.findHighestPricedListing();
    }

    /**
     * Returns the highest priced active auction order, or {@code null} if none exist.
     *
     * @return the highest priced active {@link AuctionOrder}, or {@code null} if none exist
     */
    public AuctionOrder findHighestPricedOrder() {
        return queryService.findHighestPricedOrder();
    }

    /**
     * Returns the next expiring active auction listing, or {@code null} if none exist.
     *
     * @return the next expiring active {@link AuctionListing}, or {@code null} if none exist
     */
    public AuctionListing findNextExpiringListing() {
        return queryService.findNextExpiringListing();
    }

    /**
     * Returns {@code true} if live auctions are enabled in the configuration.
     *
     * @return {@code true} if live auctions are enabled, otherwise {@code false}
     */
    public boolean liveAuctionsEnabled() {
        return queryService.liveAuctionsEnabled();
    }

    /**
     * Returns a snapshot list of queued live auctions, or an empty list if not available.
     *
     * @return a list of queued {@link LiveAuctionEntry} objects, or an empty list if not available
     */
    public java.util.List<com.skyblockexp.ezauction.live.LiveAuctionEntry> listQueuedLiveAuctions() {
        return queryService.listQueuedLiveAuctions();
    }

    /**
     * Returns the total number of active listings across all sellers.
     *
     * @return the total number of active listings
     */
    public long countActiveListings() {
        return queryService.countActiveListings();
    }

    /**
     * Returns the total number of active orders across all buyers.
     *
     * @return the total number of active orders
     */
    public long countActiveOrders() {
        return queryService.countActiveOrders();
    }
}
