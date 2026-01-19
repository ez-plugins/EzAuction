package com.skyblockexp.ezauction.service;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.bootstrap.PluginRegistry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import com.skyblockexp.ezauction.history.AuctionTransactionHistoryService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Handles creation, fulfillment, and cancellation of auction buy orders.
 */
public class AuctionOrderService {
    private static final Logger LOGGER = Logger.getLogger(AuctionOrderService.class.getName());
    
    private final Map<String, AuctionOrder> orders;
    private final AuctionTransactionService transactionService;
    private final AuctionListingRules listingRules;
    private final AuctionPersistenceManager persistenceManager;
    private final AuctionNotificationService notificationService;
    private final AuctionTransactionHistoryService transactionHistoryService;
    private final AuctionClaimService claimService;
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private final Map<String, AuctionListing> listings;
    private final AuctionConfiguration configuration;

    public AuctionOrderService(
            AuctionTransactionService transactionService,
            AuctionListingRules listingRules,
            AuctionPersistenceManager persistenceManager,
            AuctionNotificationService notificationService,
            AuctionTransactionHistoryService transactionHistoryService,
            AuctionClaimService claimService,
            Map<UUID, List<ItemStack>> pendingReturns,
            Map<String, AuctionListing> listings,
            Map<String, AuctionOrder> orders
    ) {
        this.orders = orders;
        this.transactionService = transactionService;
        this.listingRules = listingRules;
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
        this.transactionHistoryService = transactionHistoryService;
        this.claimService = claimService;
        this.pendingReturns = pendingReturns;
        this.listings = listings;
        this.configuration = EzAuctionPlugin.getStaticRegistry().getConfiguration();
    }

    public AuctionOperationResult createOrder(Player buyer, ItemStack template, double offeredPrice, Duration duration, double reservedAmount) {
        if (configuration != null && configuration.debug()) {
            LOGGER.fine("[EzAuction][DEBUG] createOrder ENTRY");
        }
        
        if (buyer == null || template == null || template.getType() == Material.AIR || template.getAmount() <= 0) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Invalid buyer or template. buyer=" + buyer + ", template=" + (template != null ? template.getType() + "x" + template.getAmount() : "null"));
            }
            return AuctionOperationResult.failure("Invalid buyer or template.");
        }
        Duration sanitizedDuration = listingRules.clampDuration(duration);
        if (sanitizedDuration == null || sanitizedDuration.isZero() || sanitizedDuration.isNegative()) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Invalid duration. duration=" + duration);
            }
            return AuctionOperationResult.failure("Order duration must be positive.");
        }
        double normalizedPrice = offeredPrice;
        if (normalizedPrice <= 0.0D) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Price not positive. price=" + normalizedPrice);
            }
            return AuctionOperationResult.failure("Order price must be positive.");
        }
        int requestedAmount = Math.max(1, template.getAmount());
        double perItemPrice = normalizedPrice / requestedAmount;
        double minimumPrice = listingRules.minimumPrice();
        if (perItemPrice < minimumPrice) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Per-item price too low. perItemPrice=" + perItemPrice + ", minimumPrice=" + minimumPrice);
            }
            return AuctionOperationResult.failure("Per-item price must be at least " + transactionService.formatCurrency(minimumPrice) + ".");
        }
        double normalizedReserved = reservedAmount;
        if (normalizedReserved < normalizedPrice) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Reserved amount too low. reserved=" + normalizedReserved + ", price=" + normalizedPrice);
            }
            return AuctionOperationResult.failure("Reserved amount must cover the full order price.");
        }
        ItemStack requestedItem = template.clone();
        AuctionOperationResult reserveResult = transactionService.reserveOrderFunds(buyer, normalizedReserved);
        if (!reserveResult.success()) {
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order creation failed: Could not reserve funds. buyer=" + (buyer != null ? buyer.getName() : "null") + ", reserved=" + normalizedReserved + ", msg=" + reserveResult.message());
            }
            return reserveResult;
        }
        String id = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + sanitizedDuration.toMillis();
        AuctionOrder order = new AuctionOrder(id, buyer.getUniqueId(), normalizedPrice, expiry, requestedItem, normalizedReserved);
        orders.put(id, order);
        if (configuration != null && configuration.debug()) {
            LOGGER.fine("[EzAuction][DEBUG] Order added: id=" + id + ", buyer=" + buyer.getName() + ", price=" + normalizedPrice + ", qty=" + requestedItem.getAmount());
        }
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        notificationService.notifyOrderCreated(order, buyer);
        transactionHistoryService.recordOrderTransactionHistory(order, buyer.getUniqueId(), buyer.getName(), null);
        return AuctionOperationResult.success("Your order has been created and funds reserved.");
    }

    public AuctionOperationResult fulfillOrder(Player seller, String orderId) {
        if (seller == null || orderId == null || orderId.isEmpty()) {
            return AuctionOperationResult.failure("Invalid seller or orderId.");
        }
        AuctionOrder order = orders.get(orderId);
        if (order == null) {
            return AuctionOperationResult.failure("Order not found or already fulfilled.");
        }
        long now = System.currentTimeMillis();
        if (order.expiryEpochMillis() < now) {
            orders.remove(orderId);
            if (configuration != null && configuration.debug()) {
                LOGGER.fine("[EzAuction][DEBUG] Order expired and removed: id=" + orderId);
            }
            persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
            notificationService.notifyOrderExpiry(order);
            transactionService.refundOrderBuyer(order.buyerId(), order.reservedAmount());
            return AuctionOperationResult.failure("This order has expired.");
        }
        if (order.buyerId().equals(seller.getUniqueId())) {
            return AuctionOperationResult.failure("You cannot fulfill your own order.");
        }
        ItemStack requestedItem = order.requestedItem();
        if (requestedItem == null || requestedItem.getType() == Material.AIR || requestedItem.getAmount() <= 0) {
            return AuctionOperationResult.failure("Order item is invalid.");
        }
        if (!seller.getInventory().containsAtLeast(requestedItem, requestedItem.getAmount())) {
            return AuctionOperationResult.failure("You do not have enough of the requested item.");
        }
        ItemStack removalStack = requestedItem.clone();
        Map<Integer, ItemStack> removed = seller.getInventory().removeItem(removalStack);
        if (!removed.isEmpty()) {
            return AuctionOperationResult.failure("Failed to remove item from your inventory.");
        }
        AuctionOperationResult payoutResult = transactionService.payOrderSeller(seller.getUniqueId(), order.offeredPrice());
        if (!payoutResult.success()) {
            seller.getInventory().addItem(removalStack);
            return AuctionOperationResult.failure("Failed to pay seller. Transaction cancelled.");
        }
        double remainder = Math.max(0.0D, order.reservedAmount() - order.offeredPrice());
        if (remainder > 0.0D) {
            transactionService.refundOrderBuyer(order.buyerId(), remainder);
        }
        claimService.deliverOrderItem(order, removalStack, pendingReturns);
        orders.remove(orderId);
        if (configuration != null && configuration.debug()) {
            LOGGER.fine("[EzAuction][DEBUG] Order fulfilled and removed: id=" + orderId + ", seller=" + seller.getName());
        }
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        notificationService.notifyOrderFulfilled(order, seller);
        transactionHistoryService.recordOrderTransactionHistory(order, seller.getUniqueId(), seller.getName(), removalStack);
        return AuctionOperationResult.success("Order fulfilled and item delivered.");
    }

    public AuctionOperationResult cancelOrder(UUID buyerId, String orderId) {
        if (buyerId == null || orderId == null || orderId.isEmpty()) {
            return AuctionOperationResult.failure("Invalid buyerId or orderId.");
        }
        AuctionOrder order = orders.get(orderId);
        if (order == null) {
            return AuctionOperationResult.failure("Order not found or already removed.");
        }
        if (!buyerId.equals(order.buyerId())) {
            return AuctionOperationResult.failure("You do not own this order.");
        }
        orders.remove(orderId);
        if (configuration != null && configuration.debug()) {
            LOGGER.fine("[EzAuction][DEBUG] Order cancelled and removed: id=" + orderId + ", buyer=" + buyerId);
        }
        transactionService.refundOrderBuyer(order.buyerId(), order.reservedAmount());
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        notificationService.notifyOrderCancelled(order);
        transactionHistoryService.recordOrderTransactionHistory(order, buyerId, null, null);
        return AuctionOperationResult.success("Your order has been cancelled and funds refunded.");
    }

    public Map<String, AuctionOrder> getOrders() {
        return orders;
    }
}
