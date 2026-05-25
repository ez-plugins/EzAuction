package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOrder;

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
import com.skyblockexp.ezauction.config.AuctionStorageConfiguration;
import com.skyblockexp.ezauction.integration.EzCountdownIntegration;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import com.skyblockexp.ezauction.live.preferences.MysqlPlayerLivePreferencesStorage;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesManager;
import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesStorage;
import com.skyblockexp.ezauction.live.preferences.YamlPlayerLivePreferencesStorage;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import com.skyblockexp.ezauction.service.AuctionManager;
import net.milkbowl.vault.economy.Economy;
import java.util.*;

public class ServiceSetupComponent {
    public static class ServiceSetupResult {
        public final AuctionTransactionService transactionService;
        public final AuctionTransactionHistory transactionHistory;
        public final LiveAuctionService liveAuctionService;
        public final AuctionManager auctionManager;
        public final Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns;
        public final com.skyblockexp.ezauction.integration.DiscordIntegration discordIntegration;
        public final com.skyblockexp.ezauction.claim.AuctionClaimService claimService;
        public final com.skyblockexp.ezauction.integration.TeamsIntegration teamsIntegration;
        public final EzCountdownIntegration ezCountdownIntegration;
        public final LiveBidSessionRegistry liveBidSessionRegistry;
        public final PlayerLivePreferencesManager playerLivePreferencesManager;
        public ServiceSetupResult(
            AuctionTransactionService transactionService,
            AuctionTransactionHistory transactionHistory,
            LiveAuctionService liveAuctionService,
            AuctionManager auctionManager,
            Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns,
            com.skyblockexp.ezauction.integration.DiscordIntegration discordIntegration,
            com.skyblockexp.ezauction.claim.AuctionClaimService claimService,
            com.skyblockexp.ezauction.integration.TeamsIntegration teamsIntegration,
            EzCountdownIntegration ezCountdownIntegration,
            LiveBidSessionRegistry liveBidSessionRegistry,
            PlayerLivePreferencesManager playerLivePreferencesManager
        ) {
            this.transactionService = transactionService;
            this.transactionHistory = transactionHistory;
            this.liveAuctionService = liveAuctionService;
            this.auctionManager = auctionManager;
            this.pendingReturns = pendingReturns;
            this.discordIntegration = discordIntegration;
            this.claimService = claimService;
            this.teamsIntegration = teamsIntegration;
            this.ezCountdownIntegration = ezCountdownIntegration;
            this.liveBidSessionRegistry = liveBidSessionRegistry;
            this.playerLivePreferencesManager = playerLivePreferencesManager;
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
        // Apply configured currency format if set
        com.skyblockexp.ezauction.config.AuctionDisplayConfiguration displayConfig = configuration.displayConfiguration();
        if (displayConfig != null && !displayConfig.currencyTemplate().isEmpty()) {
            transactionService.setCurrencyFormat(displayConfig.currencyTemplate(), displayConfig.currencyNumberFormat());
        }
        AuctionTransactionHistory transactionHistory = new AuctionTransactionHistory(plugin, historyStorage);
        transactionHistory.enable();
        // --- new live auction infrastructure ---
        boolean ezcountdownEnabled = configuration.liveAuctionConfiguration().countdownConfiguration().ezcountdownEnabled();
        boolean ezcountdownAutoDetect = configuration.liveAuctionConfiguration().countdownConfiguration().ezcountdownAutoDetect();
        EzCountdownIntegration ezCountdownIntegration = new EzCountdownIntegration(plugin, ezcountdownEnabled, ezcountdownAutoDetect);
        LiveBidSessionRegistry liveBidSessionRegistry = new LiveBidSessionRegistry();
        AuctionStorageConfiguration storageCfg = configuration.storageConfiguration();
        PlayerLivePreferencesStorage prefsStorage;
        if (storageCfg.type() == AuctionStorageConfiguration.StorageType.MYSQL && storageCfg.mysql() != null) {
            prefsStorage = new MysqlPlayerLivePreferencesStorage(plugin.getLogger(), storageCfg.mysql());
        } else {
            prefsStorage = new YamlPlayerLivePreferencesStorage(plugin);
        }
        try { prefsStorage.initialize(); } catch (Exception ex) {
            plugin.getLogger().warning("[EzAuction] Failed to initialize live preferences storage: " + ex.getMessage());
        }
        PlayerLivePreferencesManager preferencesManager = new PlayerLivePreferencesManager(plugin, prefsStorage);
        preferencesManager.loadAll();
        // ----------------------------------------
        Map<String, AuctionListing> listings = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, AuctionOrder> orders = new java.util.concurrent.ConcurrentHashMap<>();
        Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns = new java.util.concurrent.ConcurrentHashMap<>();
        Map<UUID, Long> claimExpiries = new java.util.concurrent.ConcurrentHashMap<>();
        LiveAuctionService liveAuctionService = new LiveAuctionService(plugin, transactionService,
                configuration.liveAuctionConfiguration(), configuration.backendMessages().live(),
                configuration.backendMessages().fallback(), ezCountdownIntegration,
                liveBidSessionRegistry, preferencesManager, pendingReturns);
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
        com.skyblockexp.ezauction.integration.DiscordIntegration discordIntegration = new com.skyblockexp.ezauction.integration.DiscordIntegration(plugin);
        int claimExpiryDays = configuration.claimExpiryDays();
        com.skyblockexp.ezauction.integration.TeamsIntegration teamsIntegration = new com.skyblockexp.ezauction.integration.TeamsIntegration(plugin);
        com.skyblockexp.ezauction.notification.AuctionNotificationService notificationService = new com.skyblockexp.ezauction.notification.AuctionNotificationService(plugin, configuration.backendMessages(), transactionService);
        com.skyblockexp.ezauction.claim.AuctionClaimService claimService = new com.skyblockexp.ezauction.claim.AuctionClaimService(
                pendingReturns, configuration.backendMessages(), claimExpiries, claimExpiryDays);
        // Initialise expiry for items loaded from persistent storage (they get now + expiryDays).
        for (UUID uid : pendingReturns.keySet()) {
            claimService.initExpiry(uid);
        }
        com.skyblockexp.ezauction.history.AuctionTransactionHistoryService transactionHistoryService = new com.skyblockexp.ezauction.history.AuctionTransactionHistoryService(transactionHistory, plugin, configuration.backendMessages().fallback());
        com.skyblockexp.ezauction.service.AuctionListingService listingService = new com.skyblockexp.ezauction.service.AuctionListingService(
            transactionService, listingLimitResolver, configuration, configuration.listingRules(), liveAuctionService, persistenceManager, notificationService, claimService, transactionHistoryService, pendingReturns, listings, orders, teamsIntegration
        );
        com.skyblockexp.ezauction.service.AuctionOrderService orderService = new com.skyblockexp.ezauction.service.AuctionOrderService(
            transactionService, configuration.listingRules(), persistenceManager, notificationService, transactionHistoryService, claimService, pendingReturns, listings, orders
        );
        com.skyblockexp.ezauction.service.AuctionReturnService returnService = new com.skyblockexp.ezauction.service.AuctionReturnService(
            pendingReturns,
            persistenceManager,
            configuration.backendMessages(),
            claimExpiries
        );
        com.skyblockexp.ezauction.service.AuctionExpiryService expiryService = new com.skyblockexp.ezauction.service.AuctionExpiryService(plugin, listings, orders, persistenceManager, notificationService, transactionHistoryService, claimService, transactionService, pendingReturns);
        com.skyblockexp.ezauction.service.AuctionQueryService queryService = new com.skyblockexp.ezauction.service.AuctionQueryService(listings, orders, liveAuctionService, configuration, teamsIntegration);
        AuctionManager auctionManager = new AuctionManager(plugin, listingService, orderService, returnService, expiryService, queryService, configuration, listingLimitResolver, teamsIntegration);
        auctionManager.enable();
        // Wire live-auction resolution back to listing store so sold/unsold auctions
        // free up listing slots and are cleaned from persistent storage.
        liveAuctionService.setListingResolutionCallback(listingService::resolveLiveListing);
        liveAuctionService.enable();
        return new ServiceSetupResult(transactionService, transactionHistory, liveAuctionService, auctionManager,
                pendingReturns, discordIntegration, claimService, teamsIntegration,
                ezCountdownIntegration, liveBidSessionRegistry, preferencesManager);
    }
}
