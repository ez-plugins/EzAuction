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
import com.skyblockexp.ezauction.service.*;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.AuctionManager;
import net.milkbowl.vault.economy.Economy;
import java.util.*;

import com.skyblockexp.ezframework.bootstrap.Component;
import com.skyblockexp.ezframework.Registry;
import org.bukkit.plugin.RegisteredServiceProvider;

public class ServiceSetupComponent implements Component {
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

    private EzAuctionPlugin plugin;
    private AuctionConfiguration configuration;
    private Economy economy;
    private com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepositoryField;
    private com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepositoryField;
    private CompatibilityFacade compatibilityFacade;
    private ServiceSetupResult result;

    public ServiceSetupComponent(
        EzAuctionPlugin plugin,
        AuctionConfiguration configuration,
        Economy economy,
        com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepository,
        com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepository,
        CompatibilityFacade compatibilityFacade
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.economy = economy;
        this.listingRepositoryField = listingRepository;
        this.historyRepositoryField = historyRepository;
        this.compatibilityFacade = compatibilityFacade;
    }

    public ServiceSetupComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
        this.configuration = null;
        this.economy = null;
        this.listingRepositoryField = null;
        this.historyRepositoryField = null;
        this.compatibilityFacade = null;
    }

    @Override
    public void start() throws Exception {
        // Resolve optional dependencies from the EzFramework Registry or Vault at runtime
        try {
            if (this.configuration == null) {
                try { this.configuration = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.config.AuctionConfiguration.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try {
            if (this.economy == null) {
                RegisteredServiceProvider<Economy> reg = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                if (reg != null) this.economy = reg.getProvider();
            }
        } catch (Throwable ignored) {}
        try {
            // Prefer repository views from the Registry when available (new-style usage).
            try {
                com.skyblockexp.ezauction.storage.AuctionListingRepository lr = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.storage.AuctionListingRepository.class);
                if (lr != null) this.listingRepositoryField = lr;
            } catch (Throwable ignored) {}
            try {
                com.skyblockexp.ezauction.storage.AuctionHistoryRepository hr = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.storage.AuctionHistoryRepository.class);
                if (hr != null) this.historyRepositoryField = hr;
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
        try {
            if (this.compatibilityFacade == null) {
                try { this.compatibilityFacade = Registry.forPlugin(plugin).get(CompatibilityFacade.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        AuctionTransactionService transactionService = new AuctionTransactionService(plugin, economy, configuration.backendMessages().economy(), configuration.backendMessages().fallback());
        LiveAuctionService liveAuctionService = new LiveAuctionService(plugin, transactionService, configuration.liveAuctionConfiguration(), configuration.backendMessages().live(), configuration.backendMessages().fallback());
        Map<String, AuctionListing> listings = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, AuctionOrder> orders = new java.util.concurrent.ConcurrentHashMap<>();
        Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns = new java.util.concurrent.ConcurrentHashMap<>();
        // Resolve repositories: prefer provided repository fields, then registry.
        com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepository = this.listingRepositoryField;
        if (listingRepository == null) {
            try { listingRepository = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.storage.AuctionListingRepository.class); } catch (Throwable ignored) {}
        }

        com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepository = this.historyRepositoryField;
        if (historyRepository == null) {
            try { historyRepository = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.storage.AuctionHistoryRepository.class); } catch (Throwable ignored) {}
        }

        AuctionTransactionHistory transactionHistory = new AuctionTransactionHistory(plugin, historyRepository);
        transactionHistory.enable();

        AuctionPersistenceManager persistenceManager = new AuctionPersistenceManager(
            listingRepository,
            (listingRepository instanceof com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage d) ? d : null,
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EzAuction-Persistence");
                t.setDaemon(true);
                return t;
            }),
            configuration
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
            transactionService, configuration.listingRules(), persistenceManager, notificationService, transactionHistoryService, claimService, pendingReturns, listings, orders, configuration
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
        this.result = new ServiceSetupResult(transactionService, transactionHistory, liveAuctionService, auctionManager, pendingReturns);
        // register core services into EzFramework Registry for lookup
        try {
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.transaction.AuctionTransactionService.class, transactionService);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.transaction.AuctionTransactionHistory.class, transactionHistory);
            Registry.forPlugin(plugin).register(LiveAuctionService.class, liveAuctionService);
            Registry.forPlugin(plugin).register(AuctionManager.class, auctionManager);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.persistence.AuctionPersistenceManager.class, persistenceManager);
            // register repository facade for lookup by other components
            try { Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.storage.AuctionListingRepository.class, listingRepository); } catch (Throwable ignored) {}
            try { Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.storage.AuctionHistoryRepository.class, historyRepository); } catch (Throwable ignored) {}
            // Create and register manager façades
            com.skyblockexp.ezauction.manager.ListingManager listingManager = new com.skyblockexp.ezauction.manager.ListingManager(listingService, persistenceManager, transactionService, notificationService);
            com.skyblockexp.ezauction.manager.OrderManager orderManager = new com.skyblockexp.ezauction.manager.OrderManager(orderService, persistenceManager, transactionService);
            com.skyblockexp.ezauction.manager.ReturnManager returnManager = new com.skyblockexp.ezauction.manager.ReturnManager(returnService, claimService, pendingReturns, persistenceManager);
            com.skyblockexp.ezauction.manager.TransactionManager transactionManager = new com.skyblockexp.ezauction.manager.TransactionManager(transactionService);
            com.skyblockexp.ezauction.manager.ConfigManager configManager = new com.skyblockexp.ezauction.manager.ConfigManager(plugin);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.manager.ListingManager.class, listingManager);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.manager.OrderManager.class, orderManager);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.manager.ReturnManager.class, returnManager);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.manager.TransactionManager.class, transactionManager);
            Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.manager.ConfigManager.class, configManager);
        } catch (Throwable ignored) {}
    }

    @Override
    public void stop() throws Exception {
        // no-op
    }

    public ServiceSetupResult getResult() {
        return this.result;
    }
}
