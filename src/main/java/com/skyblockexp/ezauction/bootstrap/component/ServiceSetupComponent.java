package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;

/**
 * Handles instantiation of core plugin services.
 * (Stub for further expansion.)
 */
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.compat.CompatibilityFacade;
import com.skyblockexp.ezauction.storage.AuctionStorage;
import com.skyblockexp.ezauction.storage.AuctionHistoryStorage;
import com.skyblockexp.ezauction.service.*;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.AuctionManager;
import net.milkbowl.vault.economy.Economy;
import java.util.*;

public class ServiceSetupComponent {
    public static class ServiceSetupResult {
        public final AuctionTransactionService transactionService;
        public final AuctionTransactionHistory transactionHistory;
        public final LiveAuctionService liveAuctionService;
        public final AuctionManager auctionManager;
        public final Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns;
        public ServiceSetupResult(
            AuctionTransactionService transactionService,
            AuctionTransactionHistory transactionHistory,
            LiveAuctionService liveAuctionService,
            AuctionManager auctionManager,
            Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns
        ) {
            this.transactionService = transactionService;
            this.transactionHistory = transactionHistory;
            this.liveAuctionService = liveAuctionService;
            this.auctionManager = auctionManager;
            this.pendingReturns = pendingReturns;
        }
    }

    public static ServiceSetupResult setupAll(
        EzAuctionPlugin plugin,
        AuctionConfiguration configuration,
        Economy economy,
        AuctionStorage listingStorage,
        AuctionHistoryStorage historyStorage,
        CompatibilityFacade compatibilityFacade
    ) {
        AuctionTransactionService transactionService = new AuctionTransactionService(plugin, economy, configuration.backendMessages().economy(), configuration.backendMessages().fallback());
        AuctionTransactionHistory transactionHistory = new AuctionTransactionHistory(plugin, historyStorage);
        transactionHistory.enable();
        LiveAuctionService liveAuctionService = new LiveAuctionService(plugin, transactionService, configuration.liveAuctionConfiguration(), configuration.backendMessages().live(), configuration.backendMessages().fallback());
        Map<String, AuctionListing> listings = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, AuctionOrder> orders = new java.util.concurrent.ConcurrentHashMap<>();
        Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns = new java.util.concurrent.ConcurrentHashMap<>();
        AuctionPersistenceManager persistenceManager = new AuctionPersistenceManager(
            listingStorage,
            (listingStorage instanceof com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage d) ? d : null,
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EzAuction-Persistence");
                t.setDaemon(true);
                return t;
            })
        );
        persistenceManager.setStorageReady(true);
        com.skyblockexp.ezauction.storage.AuctionStorageSnapshot snapshot = persistenceManager.loadFromStorage();
        if (snapshot != null) {
            listings.putAll(snapshot.listings());
            orders.putAll(snapshot.orders());
            if (snapshot.pendingReturns() != null) {
                pendingReturns.putAll(snapshot.pendingReturns());
            }
        }
        com.skyblockexp.ezauction.api.AuctionListingLimitResolver listingLimitResolver;
        org.bukkit.plugin.RegisteredServiceProvider<com.skyblockexp.ezauction.api.AuctionListingLimitResolver> limitProvider = plugin.getServer().getServicesManager().getRegistration(com.skyblockexp.ezauction.api.AuctionListingLimitResolver.class);
        if (limitProvider != null && limitProvider.getProvider() != null) {
            listingLimitResolver = limitProvider.getProvider();
        } else {
            listingLimitResolver = com.skyblockexp.ezauction.api.AuctionListingLimitResolver.useBaseLimit();
        }
        com.skyblockexp.ezauction.notification.AuctionNotificationService notificationService = new com.skyblockexp.ezauction.notification.AuctionNotificationService(configuration.backendMessages(), transactionService);
        com.skyblockexp.ezauction.claim.AuctionClaimService claimService = new com.skyblockexp.ezauction.claim.AuctionClaimService(pendingReturns, configuration.backendMessages());
        com.skyblockexp.ezauction.history.AuctionTransactionHistoryService transactionHistoryService = new com.skyblockexp.ezauction.history.AuctionTransactionHistoryService(transactionHistory, plugin, configuration.backendMessages().fallback());
        com.skyblockexp.ezauction.service.AuctionListingService listingService = new com.skyblockexp.ezauction.service.AuctionListingService(
            transactionService, listingLimitResolver, configuration, configuration.listingRules(), liveAuctionService, persistenceManager, notificationService, claimService, transactionHistoryService, pendingReturns, listings, orders
        );
        com.skyblockexp.ezauction.service.AuctionOrderService orderService = new com.skyblockexp.ezauction.service.AuctionOrderService(
            transactionService, configuration.listingRules(), persistenceManager, notificationService, transactionHistoryService, claimService, pendingReturns, listings, orders
        );
        com.skyblockexp.ezauction.service.AuctionReturnService returnService = new com.skyblockexp.ezauction.service.AuctionReturnService(
            pendingReturns,
            persistenceManager,
            configuration.backendMessages()
        );
        com.skyblockexp.ezauction.service.AuctionExpiryService expiryService = new com.skyblockexp.ezauction.service.AuctionExpiryService(plugin, listings, orders, persistenceManager, notificationService, transactionHistoryService, claimService, transactionService, pendingReturns);
        com.skyblockexp.ezauction.service.AuctionQueryService queryService = new com.skyblockexp.ezauction.service.AuctionQueryService(listings, orders, liveAuctionService, configuration);
        AuctionManager auctionManager = new AuctionManager(plugin, listingService, orderService, returnService, expiryService, queryService, configuration, listingLimitResolver);
        auctionManager.enable();
        liveAuctionService.enable();
        return new ServiceSetupResult(transactionService, transactionHistory, liveAuctionService, auctionManager, pendingReturns);
    }
}
