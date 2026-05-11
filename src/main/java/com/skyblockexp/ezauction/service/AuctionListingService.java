package com.skyblockexp.ezauction.service;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;

import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.event.AuctionListingCreateEvent;
import com.skyblockexp.ezauction.event.AuctionListingSellEvent;
import com.skyblockexp.ezauction.event.AuctionListingSoldEvent;
import com.skyblockexp.ezauction.integration.TeamsIntegration;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import com.skyblockexp.ezauction.history.AuctionTransactionHistoryService;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionListingRules;
import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;
import com.skyblockexp.ezauction.util.AuctionValidationUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles creation, cancellation, and purchase of auction listings.
 */
public class AuctionListingService {
    private final Map<String, AuctionListing> listings;
    private final AuctionTransactionService transactionService;
    private final AuctionListingLimitResolver listingLimitResolver;
    private final AuctionConfiguration configuration;
    private final AuctionListingRules listingRules;
    private final LiveAuctionService liveAuctionService;
    private final AuctionPersistenceManager persistenceManager;
    private final AuctionNotificationService notificationService;
    private final AuctionClaimService claimService;
    private final AuctionTransactionHistoryService transactionHistoryService;
    private final Map<UUID, List<ItemStack>> pendingReturns;
    private final Map<String, AuctionOrder> orders; // Needed for persistence
    private final TeamsIntegration teamsIntegration;

    public AuctionListingService(
            AuctionTransactionService transactionService,
            AuctionListingLimitResolver listingLimitResolver,
            AuctionConfiguration configuration,
            AuctionListingRules listingRules,
            LiveAuctionService liveAuctionService,
            AuctionPersistenceManager persistenceManager,
            AuctionNotificationService notificationService,
            AuctionClaimService claimService,
            AuctionTransactionHistoryService transactionHistoryService,
            Map<UUID, List<ItemStack>> pendingReturns,
            Map<String, AuctionListing> listings,
            Map<String, AuctionOrder> orders
    ) {
        this(transactionService, listingLimitResolver, configuration, listingRules, liveAuctionService,
                persistenceManager, notificationService, claimService, transactionHistoryService,
                pendingReturns, listings, orders, null);
    }

    public AuctionListingService(
            AuctionTransactionService transactionService,
            AuctionListingLimitResolver listingLimitResolver,
            AuctionConfiguration configuration,
            AuctionListingRules listingRules,
            LiveAuctionService liveAuctionService,
            AuctionPersistenceManager persistenceManager,
            AuctionNotificationService notificationService,
            AuctionClaimService claimService,
            AuctionTransactionHistoryService transactionHistoryService,
            Map<UUID, List<ItemStack>> pendingReturns,
            Map<String, AuctionListing> listings,
            Map<String, AuctionOrder> orders,
            TeamsIntegration teamsIntegration
    ) {
        this.listings = listings;
        this.transactionService = transactionService;
        this.listingLimitResolver = listingLimitResolver;
        this.configuration = configuration;
        this.listingRules = listingRules;
        this.liveAuctionService = liveAuctionService;
        this.persistenceManager = persistenceManager;
        this.notificationService = notificationService;
        this.claimService = claimService;
        this.transactionHistoryService = transactionHistoryService;
        this.pendingReturns = pendingReturns;
        this.orders = orders;
        this.teamsIntegration = teamsIntegration;
    }

    public AuctionOperationResult createListing(Player seller, ItemStack item, double price, Duration duration) {
        return createListing(seller, item, price, duration, null);
    }

    /**
     * Creates a new auction listing.
     *
     * @param teamId when non-null the listing is restricted to members of this team; pass
     *               {@code null} for a global listing visible to everyone
     */
    public AuctionOperationResult createListing(Player seller, ItemStack item, double price, Duration duration, UUID teamId) {
                if (configuration.debug()) {
                    System.out.println("[EzAuction][DEBUG] createListing called: seller=" + (seller != null ? seller.getName() : "null") + ", item=" + (item != null ? item.getType() : "null") + ", price=" + price + ", duration=" + duration);
                }
        if (seller == null) {
            return AuctionOperationResult.failure("Only players can create listings.");
        }
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return AuctionOperationResult.failure("You must provide a valid item to list.");
        }

        // Team scope validation
        if (teamId != null) {
            if (teamsIntegration == null || !teamsIntegration.isAvailable()) {
                return AuctionOperationResult.failure("Team auctions are not available on this server.");
            }
            Optional<UUID> sellerTeamId = teamsIntegration.getTeamId(seller.getUniqueId());
            if (sellerTeamId.isEmpty() || !sellerTeamId.get().equals(teamId)) {
                return AuctionOperationResult.failure("You are not a member of the specified team.");
            }
        }

        AuctionListingCreateEvent event = new AuctionListingCreateEvent(seller, item, price);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return AuctionOperationResult.failure("Auction listing creation was cancelled by another plugin.");
        }
        seller = event.getPlayer();
        item = event.getItem();
        price = event.getPrice();
        Duration sanitizedDuration = listingRules.clampDuration(duration);
        if (sanitizedDuration == null || sanitizedDuration.isZero() || sanitizedDuration.isNegative()) {
            return AuctionOperationResult.failure("Listing duration must be positive.");
        }
        double normalizedPrice = price;
        if (normalizedPrice <= 0) {
            return AuctionOperationResult.failure("Listing price must be positive.");
        }
        double minimumPrice = listingRules.minimumPrice();
        if (normalizedPrice < minimumPrice) {
            return AuctionOperationResult.failure("Listing price must be at least " + transactionService.formatCurrency(minimumPrice) + ".");
        }
        double normalizedDeposit = listingRules.depositAmount(normalizedPrice);
        if (normalizedDeposit < 0) normalizedDeposit = 0.0D;


        UUID sellerId = seller.getUniqueId();
        int listingLimit = AuctionValidationUtils.resolveListingLimit(sellerId, configuration, listingLimitResolver);
        if (listingLimit > 0) {
            long activeListings = listings.values().stream().filter(l -> sellerId.equals(l.sellerId())).count();
            if (activeListings >= listingLimit) {
                return AuctionOperationResult.failure("You have reached your listing limit (" + listingLimit + ").");
            }
        }

        ItemStack listingItem = item.clone();
                if (configuration.debug()) {
                    System.out.println("[EzAuction][DEBUG] Cloned item: " + listingItem + ", amount=" + listingItem.getAmount());
                }
        if (!seller.getInventory().containsAtLeast(listingItem, listingItem.getAmount())) {
            return AuctionOperationResult.failure("You do not have enough of the item in your inventory.");
        }

        if (normalizedDeposit > 0.0D) {
            AuctionOperationResult depositResult = transactionService.chargeListingDeposit(seller, normalizedDeposit);
            if (!depositResult.success()) {
                return depositResult;
            }
        }

        Map<Integer, ItemStack> leftover = seller.getInventory().removeItem(listingItem);
                if (configuration.debug()) {
                    System.out.println("[EzAuction][DEBUG] Attempted to remove item from inventory. Leftover: " + leftover);
                }
        if (!leftover.isEmpty()) {
            if (normalizedDeposit > 0.0D) {
                transactionService.refundListingDeposit(seller.getUniqueId(), normalizedDeposit);
            }
            return AuctionOperationResult.failure("Failed to remove item from your inventory.");
        }

        String id = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + sanitizedDuration.toMillis();
        AuctionListing listing = new AuctionListing(id, seller.getUniqueId(), normalizedPrice, expiry, listingItem, normalizedDeposit, teamId);
        listings.put(id, listing);
                if (configuration.debug()) {
                    System.out.println("[EzAuction][DEBUG] Listing added to map: id=" + id + ", listing=" + listing);
                }
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));

        if (liveAuctionService != null) {
            liveAuctionService.enqueue(listing, seller.getUniqueId(), seller.getName());
        }

        // Notify that the auction has started
        notificationService.notifyAuctionStarted(listing, seller, sanitizedDuration);
        transactionHistoryService.recordListingTransactionHistory(listing, seller);

        return AuctionOperationResult.success("Your item has been listed for auction!");
    }

    public AuctionOperationResult purchaseListing(Player buyer, String listingId) {
        if (buyer == null || listingId == null || listingId.isEmpty()) {
            return AuctionOperationResult.failure("Invalid buyer or listingId.");
        }
        AuctionListing listing = listings.get(listingId);
        if (listing == null) {
            return AuctionOperationResult.failure("Listing not found or already purchased.");
        }
        long now = System.currentTimeMillis();
        if (listing.expiryEpochMillis() < now) {
            listings.remove(listingId);
            persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
            notificationService.notifySellerExpiry(listing);
            claimService.returnListingItem(listing, pendingReturns);
            return AuctionOperationResult.failure("This listing has expired.");
        }
        if (listing.sellerId().equals(buyer.getUniqueId())) {
            return AuctionOperationResult.failure("You cannot purchase your own listing.");
        }

        // Team scope validation: buyer must be a member of the seller's team
        if (listing.isTeamListing()) {
            if (teamsIntegration == null || !teamsIntegration.isAvailable()) {
                return AuctionOperationResult.failure("Team listings cannot be purchased: TeamsAPI is unavailable.");
            }
            if (!teamsIntegration.isSameTeam(listing.sellerId(), buyer.getUniqueId())) {
                return AuctionOperationResult.failure("You must be a member of the seller's team to purchase this listing.");
            }
        }

        // Fire AuctionListingSellEvent (cancellable) before sale
        AuctionListingSellEvent sellEvent = new AuctionListingSellEvent(listing);
        Bukkit.getPluginManager().callEvent(sellEvent);
        if (sellEvent.isCancelled()) {
            return AuctionOperationResult.failure("This listing cannot be purchased right now.");
        }
        ItemStack itemToGive = listing.item();
        if (!claimService.hasInventorySpace(buyer, itemToGive)) {
            return AuctionOperationResult.failure("You do not have enough inventory space.");
        }
        AuctionOperationResult withdrawResult = transactionService.withdrawBuyer(buyer, listing.price());
        if (!withdrawResult.success()) {
            return withdrawResult;
        }
        boolean claimSuccess = claimService.claimListing(listingId, listings, pendingReturns);
        if (!claimSuccess) {
            transactionService.refundBuyer(buyer, listing.price());
            return AuctionOperationResult.failure("Failed to claim the listing. You have not been charged.");
        }
        Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(itemToGive);
        if (!leftover.isEmpty()) {
            claimService.storeReturnItem(buyer.getUniqueId(), itemToGive, pendingReturns);
            transactionService.refundBuyer(buyer, listing.price());
            return AuctionOperationResult.failure("Could not add item to your inventory. You have not been charged.");
        }
        AuctionOperationResult creditResult = transactionService.creditSeller(listing.sellerId(), listing.price());
        if (!creditResult.success()) {
            buyer.getInventory().removeItem(itemToGive);
            claimService.storeReturnItem(buyer.getUniqueId(), itemToGive, pendingReturns);
            transactionService.refundBuyer(buyer, listing.price());
            return AuctionOperationResult.failure("Failed to credit the seller. Transaction cancelled.");
        }
        if (listing.deposit() > 0.0D) {
            transactionService.refundListingDeposit(listing.sellerId(), listing.deposit());
        }
        listings.remove(listingId);
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        notificationService.notifySellerSale(listing);
        transactionHistoryService.recordListingTransactionHistory(listing, buyer);

        // Fire AuctionListingSoldEvent after sale
        AuctionListingSoldEvent soldEvent = new AuctionListingSoldEvent(listing);
        Bukkit.getPluginManager().callEvent(soldEvent);

        return AuctionOperationResult.success("You have successfully purchased the listing!");
    }

    /**
     * Places a bid on the given listing. Currently this method only fires an event and
     * notifies listeners — it does not mutate the listing state or handle payments.
     * This provides a safe hook for implementing a full bid workflow later.
     */
    public AuctionOperationResult placeBid(Player bidder, String listingId, double amount) {
        if (bidder == null || listingId == null || listingId.isEmpty()) {
            return AuctionOperationResult.failure("Invalid bidder or listingId.");
        }
        AuctionListing listing = listings.get(listingId);
        if (listing == null) return AuctionOperationResult.failure("Listing not found.");
        if (listing.isExpired()) return AuctionOperationResult.failure("Listing has expired.");
        if (listing.sellerId().equals(bidder.getUniqueId())) return AuctionOperationResult.failure("You cannot bid on your own listing.");
        // Fire event
        com.skyblockexp.ezauction.event.AuctionListingBidEvent bidEvent = new com.skyblockexp.ezauction.event.AuctionListingBidEvent(listing, bidder, amount);
        org.bukkit.Bukkit.getPluginManager().callEvent(bidEvent);
        if (bidEvent.isCancelled()) return AuctionOperationResult.failure("Bid cancelled by another plugin.");

        // Notify services (no state change)
        notificationService.notifyAuctionBid(listing, bidder, amount);

        return AuctionOperationResult.success("Your bid was submitted.");
    }

    public AuctionOperationResult cancelListing(UUID sellerId, String listingId) {
        if (sellerId == null || listingId == null || listingId.isEmpty()) {
            return AuctionOperationResult.failure("Invalid sellerId or listingId.");
        }
        AuctionListing listing = listings.get(listingId);
        if (listing == null) {
            return AuctionOperationResult.failure("Listing not found or already removed.");
        }
        if (!sellerId.equals(listing.sellerId())) {
            return AuctionOperationResult.failure("You do not own this listing.");
        }
        listings.remove(listingId);
        claimService.returnListingItem(listing, pendingReturns);
        if (listing.deposit() > 0.0D) {
            transactionService.refundListingDeposit(sellerId, listing.deposit());
        }
        persistenceManager.saveListings(new ArrayList<>(listings.values()), new ArrayList<>(orders.values()));
        notificationService.notifySellerCancelled(listing);
        transactionHistoryService.recordListingTransactionHistory(listing, null);
        return AuctionOperationResult.success("Your listing has been cancelled and the item returned.");
    }

    public Map<String, AuctionListing> getListings() {
        return listings;
    }
}
