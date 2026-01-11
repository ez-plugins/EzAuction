package com.skyblockexp.ezauction.bootstrap;

import com.skyblockexp.ezauction.gui.AuctionReturnListener;
import com.skyblockexp.ezauction.gui.AuctionSellMenuListener;
import com.skyblockexp.ezauction.live.LiveAuctionCommand;

import com.skyblockexp.ezauction.*;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.api.AuctionListingLimitResolver;
import com.skyblockexp.ezauction.command.*;
import com.skyblockexp.ezauction.compat.*;
import com.skyblockexp.ezauction.config.*;
import com.skyblockexp.ezauction.gui.*;
import com.skyblockexp.ezauction.hologram.*;
import com.skyblockexp.ezauction.live.*;
import com.skyblockexp.ezauction.placeholder.*;
import com.skyblockexp.ezauction.storage.*;
import com.skyblockexp.ezauction.util.*;
import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.storage.AuctionStorageSnapshot;
import com.skyblockexp.ezauction.storage.DistributedAuctionListingStorage;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.config.AuctionMenuInteractionConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionCommandMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import com.skyblockexp.ezauction.util.EzShopsItemValueProvider;
import com.skyblockexp.ezauction.util.ConfiguredItemValueProvider;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import com.skyblockexp.ezauction.live.LiveAuctionEnqueueListener;
import com.skyblockexp.ezauction.hologram.AuctionHologramListener;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bstats.bukkit.Metrics;
import net.milkbowl.vault.economy.Economy;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import com.skyblockexp.ezauction.service.AuctionListingService;
import com.skyblockexp.ezauction.service.AuctionOrderService;
import com.skyblockexp.ezauction.service.AuctionReturnService;
import com.skyblockexp.ezauction.service.AuctionExpiryService;
import com.skyblockexp.ezauction.service.AuctionQueryService;
import com.skyblockexp.ezauction.persistence.AuctionPersistenceManager;
import com.skyblockexp.ezauction.notification.AuctionNotificationService;
import com.skyblockexp.ezauction.claim.AuctionClaimService;
import com.skyblockexp.ezauction.history.AuctionTransactionHistoryService;

/**
 * PluginRegistry acts as the central bootstrapper and dependency container for all major EzAuction plugin components.
 * <p>
 * Handles initialization, registration, and shutdown of managers, GUIs, services, and integrations. This class is designed
 * to keep EzAuctionPlugin clean and modular, and to facilitate maintainability and testability.
 * <p>
 * All plugin-wide dependencies are exposed as final fields for easy access by other components.
 *
 * <p><b>Thread safety:</b> This class is not thread-safe and should only be used on the main server thread.</p>
 *
 * @author SkyblockExperience Team
 * @since 1.1.0
 */
public class PluginRegistry {
    /** The main plugin instance. */
    private final EzAuctionPlugin plugin;
    /** Whether the auction history GUI is enabled. */
    public final boolean historyGuiEnabled;
    /** Vault economy provider. */
    public final Economy economy;
    /** Handles all auction transactions. */
    public final AuctionTransactionService transactionService;
    /** Stores and manages auction transaction history. */
    public final AuctionTransactionHistory transactionHistory;
    /** Main auction manager for listings and logic. */
    public final AuctionManager auctionManager;
    /** Storage for active auction listings. */
    public final AuctionStorage listingStorage;
    /** Storage for auction history. */
    public final AuctionHistoryStorage historyStorage;
    /** Main configuration for the plugin. */
    public final AuctionConfiguration configuration;
    /** Main auction browser GUI. */
    public final AuctionMenu auctionMenu;
    /** Provider for item value recommendations. */
    public final ItemValueProvider itemValueProvider;
    /** Provider for shop price overlays. */
    public final ItemValueProvider shopPriceValueProvider;
    /** GUI for managing auction orders. */
    public final AuctionOrderMenu auctionOrderMenu;
    /** GUI for selling items in auctions. */
    public final AuctionSellMenu auctionSellMenu;
    /** Main /auction command handler. */
    public final AuctionCommand auctionCommand;
    /** GUI for live auctions. */
    public final LiveAuctionMenu liveAuctionMenu;
    /** Hologram manager for auction displays. */
    public final AuctionHologramManager hologramManager;
    /** Command handler for auction holograms. */
    public final AuctionHologramCommand hologramCommand;
    /** Handles version/platform compatibility. */
    public final CompatibilityFacade compatibilityFacade;
    /** PlaceholderAPI expansion for EzAuction. */
    public final EzAuctionPlaceholderExpansion placeholderExpansion;
    /** bStats metrics instance. */
    public final Metrics metrics;
    /** Service for live auction events. */
    public final LiveAuctionService liveAuctionService;
    /**
     * Stores pending item returns for auction claims.
     * <p>
     * This map is populated from persistent storage on startup and is shared with
     * the AuctionClaimService and AuctionReturnListener to ensure players can reclaim items.
     * </p>
     */
    private final Map<UUID, List<org.bukkit.inventory.ItemStack>> pendingReturns;

    /**
     * Initializes and wires all plugin components, managers, GUIs, and integrations.
     *
     * @param plugin The main EzAuctionPlugin instance
     */
    public PluginRegistry(EzAuctionPlugin plugin) {
        this.plugin = plugin;
        this.configuration = AuctionConfigurationLoader.load(plugin);

        // 1. Setup economy
        Economy tempEconomy = null;
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found. EzAuction requires Vault and an economy provider to process currency.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        } else {
            RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (registration == null) {
                plugin.getLogger().severe("No Vault economy provider found. EzAuction cannot function without an economy bridge.");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
            } else {
                tempEconomy = registration.getProvider();
                if (tempEconomy == null) {
                    plugin.getLogger().severe("Unable to acquire a Vault economy provider. EzAuction cannot function without an economy bridge.");
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                }
            }
        }
        this.economy = tempEconomy;

        // Determine if history GUI is enabled
        boolean guiEnabled = false;
        try {
            if (configuration != null) {
                guiEnabled = configuration.isHistoryGuiEnabled();
            }
        } catch (Exception ignored) {}
        this.historyGuiEnabled = guiEnabled;

        // 2. Compatibility and config
        this.compatibilityFacade = CompatibilityFacade.create(plugin);
        ItemTagStorage itemTagStorage = compatibilityFacade.itemTagStorage();
        HologramPlatform hologramPlatform = compatibilityFacade.hologramPlatform();
        boolean hologramSupportAvailable = hologramPlatform.isSupported();
        if (!hologramSupportAvailable && configuration.hologramConfiguration().enabled()) {
            plugin.getLogger().warning("TextDisplay entities are unavailable on this server version. EzAuction holograms will be disabled.");
        }
        AuctionStorageBundle storageBundle = AuctionStorageFactory.create(plugin, configuration);
        this.listingStorage = storageBundle.listingStorage();
        this.historyStorage = storageBundle.historyStorage();

        AuctionBackendMessages backendMessages = configuration.backendMessages();
        this.transactionService = new AuctionTransactionService(plugin, economy, backendMessages.economy(), backendMessages.fallback());
        this.transactionHistory = new AuctionTransactionHistory(plugin, historyStorage);
        this.transactionHistory.enable();

        AuctionListingLimitResolver listingLimitResolver;
        RegisteredServiceProvider<AuctionListingLimitResolver> limitProvider = plugin.getServer().getServicesManager().getRegistration(AuctionListingLimitResolver.class);
        if (limitProvider != null && limitProvider.getProvider() != null) {
            listingLimitResolver = limitProvider.getProvider();
        } else {
            listingLimitResolver = AuctionListingLimitResolver.useBaseLimit();
        }

        this.liveAuctionService = new LiveAuctionService(plugin, transactionService, configuration.liveAuctionConfiguration(), backendMessages.live(), backendMessages.fallback());


        // Shared state for listings, orders, and returns
        Map<String, AuctionListing> listings = new java.util.concurrent.ConcurrentHashMap<>();
        Map<String, AuctionOrder> orders = new java.util.concurrent.ConcurrentHashMap<>();
        this.pendingReturns = new java.util.concurrent.ConcurrentHashMap<>();

        // Services
        DistributedAuctionListingStorage distributedStorage =
            (listingStorage instanceof DistributedAuctionListingStorage d) ? d : null;
        java.util.concurrent.ExecutorService persistenceExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "EzAuction-Persistence");
                t.setDaemon(true);
                return t;
            });
        AuctionPersistenceManager persistenceManager = new AuctionPersistenceManager(
            listingStorage,
            distributedStorage,
            persistenceExecutor
        );
        // Ensure persistence is enabled
        persistenceManager.setStorageReady(true);

        // Load persisted listings, orders, and returns from storage
        AuctionStorageSnapshot snapshot = persistenceManager.loadFromStorage();
        if (snapshot != null) {
            listings.putAll(snapshot.listings());
            orders.putAll(snapshot.orders());
            if (snapshot.pendingReturns() != null) {
                this.pendingReturns.putAll(snapshot.pendingReturns());
            }
        }

        AuctionBackendMessages finalMessages = backendMessages != null ? backendMessages : AuctionBackendMessages.defaults();
        AuctionNotificationService notificationService = new AuctionNotificationService(finalMessages, transactionService);
        AuctionClaimService claimService = new AuctionClaimService(pendingReturns, finalMessages);
        AuctionTransactionHistoryService transactionHistoryService = new AuctionTransactionHistoryService(transactionHistory, plugin, finalMessages.fallback());

        AuctionListingService listingService = new AuctionListingService(
            transactionService, listingLimitResolver, configuration, configuration.listingRules(), liveAuctionService, persistenceManager, notificationService, claimService, transactionHistoryService, pendingReturns, listings, orders
        );
        AuctionOrderService orderService = new AuctionOrderService(
            transactionService, configuration.listingRules(), persistenceManager, notificationService, transactionHistoryService, claimService, pendingReturns, listings
        );
        AuctionReturnService returnService = new AuctionReturnService(persistenceManager);
        AuctionExpiryService expiryService = new AuctionExpiryService(plugin, listings, orders, persistenceManager, notificationService, transactionHistoryService, claimService, transactionService, pendingReturns);
        AuctionQueryService queryService = new AuctionQueryService(listings, orders, liveAuctionService, configuration);
        this.auctionManager = new AuctionManager(plugin, listingService, orderService, returnService, expiryService, queryService, configuration, listingLimitResolver);
        this.auctionManager.enable();
        if (liveAuctionService != null) {
            liveAuctionService.enable();
        }

        AuctionValueConfiguration valueConfiguration = configuration.valueConfiguration();
        AuctionMessageConfiguration messageConfiguration = configuration.messageConfiguration();
        AuctionCommandMessageConfiguration commandMessageConfiguration = configuration.commandMessageConfiguration();
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration = valueConfiguration != null ? valueConfiguration.shopPriceConfiguration() : null;
        this.itemValueProvider = resolveItemValueProvider(plugin, valueConfiguration);
        boolean shopPriceOverlayEnabled = shouldEnableShopPriceOverlay(plugin, shopPriceConfiguration);
        this.shopPriceValueProvider = shopPriceOverlayEnabled ? resolveShopPriceValueProvider(plugin, shopPriceConfiguration) : ItemValueProvider.none();
        if (shopPriceOverlayEnabled && shopPriceValueProvider == ItemValueProvider.none()) {
            shopPriceOverlayEnabled = false;
        }
        ItemValueProvider recommendationProvider = itemValueProvider;
        if ((valueConfiguration == null || !valueConfiguration.enabled()) && shopPriceOverlayEnabled) {
            recommendationProvider = shopPriceValueProvider;
        }

        this.auctionMenu = new AuctionMenu(plugin, auctionManager, transactionService, configuration.menuConfiguration(), messageConfiguration.browser(), valueConfiguration, itemValueProvider, shopPriceValueProvider, shopPriceOverlayEnabled, itemTagStorage);
        this.liveAuctionMenu = new LiveAuctionMenu(plugin, auctionManager, transactionService, auctionMenu, liveAuctionService, messageConfiguration.live(), valueConfiguration, shopPriceValueProvider, shopPriceOverlayEnabled, itemTagStorage);
        AuctionMenuInteractionConfiguration menuInteractions = configuration.menuInteractionConfiguration();
        this.auctionOrderMenu = new AuctionOrderMenu(plugin, auctionManager, transactionService, configuration.listingRules(), configuration.durationOptions(), menuInteractions.orderMenu(), recommendationProvider, messageConfiguration.order(), itemTagStorage);
        this.auctionSellMenu = new AuctionSellMenu(plugin, auctionManager, transactionService, configuration.listingRules(), configuration.durationOptions(), menuInteractions.sellMenu(), recommendationProvider, messageConfiguration.sell(), itemTagStorage);

        this.auctionCommand = new AuctionCommand(auctionManager, auctionMenu, auctionSellMenu, auctionOrderMenu, transactionHistory, transactionService, configuration.listingRules(), liveAuctionMenu, commandMessageConfiguration);

        // Hologram logic
        AuctionHologramConfiguration hologramConfiguration = configuration.hologramConfiguration();
        if (hologramConfiguration != null && hologramConfiguration.enabled() && hologramSupportAvailable) {
            this.hologramManager = new AuctionHologramManager(plugin, auctionManager, transactionService, hologramConfiguration, hologramPlatform);
            this.hologramManager.enable();
            this.hologramCommand = new AuctionHologramCommand(hologramManager, commandMessageConfiguration.holograms());
        } else {
            this.hologramManager = null;
            this.hologramCommand = null;
            if (hologramConfiguration != null && hologramConfiguration.enabled()) {
                plugin.getLogger().warning("Auction holograms requested but unavailable on this platform; feature will remain disabled.");
            }
        }

        // PlaceholderAPI
        Plugin placeholderApi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        EzAuctionPlaceholderExpansion tempExpansion = null;
        if (placeholderApi != null && placeholderApi.isEnabled()) {
            tempExpansion = new EzAuctionPlaceholderExpansion(auctionManager, plugin.getDescription());
            if (!tempExpansion.register()) {
                plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders for EzAuction.");
                tempExpansion = null;
            }
        }
        this.placeholderExpansion = tempExpansion;

        // Metrics
        Metrics tempMetrics = null;
        try {
            tempMetrics = new Metrics(plugin, 27737);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to start bStats metrics: " + throwable.getMessage());
        }
        this.metrics = tempMetrics;
    }

    /**
     * Registers all event listeners and commands for the plugin.
     * Should be called from EzAuctionPlugin.onEnable().
     */
    public void enableAll() {
        // Register events and commands
        plugin.getServer().getPluginManager().registerEvents(auctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(liveAuctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(auctionOrderMenu, plugin);
        // Register AuctionReturnListener with the shared pendingReturns map for claim handling
        plugin.getServer().getPluginManager().registerEvents(new AuctionReturnListener(new AuctionClaimService(
            pendingReturns,
            configuration.backendMessages()
        )), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionSellMenuListener(auctionSellMenu), plugin);

        // Register AuctionHistoryListener only if history GUI is enabled
        if (historyGuiEnabled) {
            plugin.getServer().getPluginManager().registerEvents(new AuctionHistoryListener(), plugin);
        }

        if (liveAuctionService != null && liveAuctionService.isEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(new LiveAuctionEnqueueListener(liveAuctionService), plugin);
            LiveAuctionCommand liveCmd = new LiveAuctionCommand(auctionManager, liveAuctionMenu, auctionSellMenu, configuration.commandMessageConfiguration());
            plugin.getCommand("liveauction").setExecutor(liveCmd);
            plugin.getCommand("liveauction").setTabCompleter(liveCmd);
        }

        PluginCommand command = plugin.getCommand("auction");
        if (command != null) {
            command.setExecutor(auctionCommand);
            command.setTabCompleter(auctionCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'auction' is not defined in plugin.yml; the auction house will be unusable.");
        }

        if (hologramManager != null && hologramCommand != null) {
            PluginCommand hologramPluginCommand = plugin.getCommand("auctionhologram");
            if (hologramPluginCommand != null) {
                hologramPluginCommand.setExecutor(hologramCommand);
                hologramPluginCommand.setTabCompleter(hologramCommand);
            } else {
                plugin.getLogger().warning("Plugin command 'auctionhologram' is not defined; hologram placement will be unavailable.");
            }
            plugin.getServer().getPluginManager().registerEvents(new AuctionHologramListener(hologramManager), plugin);
        }

        // Update checker
        new com.skyblockexp.ezauction.update.SpigotUpdateChecker(plugin, 129779).checkForUpdates();
    }

    /**
     * Disables and cleans up all managers, services, and resources.
     * Should be called from EzAuctionPlugin.onDisable().
     */
    public void disableAll() {
        // Disable and cleanup all managers/services
        if (auctionManager != null) auctionManager.disable();
        if (liveAuctionService != null) liveAuctionService.disable();
        if (transactionHistory != null) transactionHistory.disable();
        if (hologramManager != null) hologramManager.disable();
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        
        try {
            if (listingStorage != null) listingStorage.close();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close EzAuction listing storage.", ex);
        }

        try {
            if (historyStorage != null && historyStorage != listingStorage) historyStorage.close();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close EzAuction history storage.", ex);
        }
    }

    /**
     * Resolves the item value provider for auction listings, using a registered service or configuration.
     *
     * @param plugin The plugin instance
     * @param valueConfiguration The auction value configuration
     * @return the item value provider
     */
    private ItemValueProvider resolveItemValueProvider(EzAuctionPlugin plugin, AuctionValueConfiguration valueConfiguration) {
        ServicesManager servicesManager = plugin.getServer().getServicesManager();
        RegisteredServiceProvider<ItemValueProvider> registration = servicesManager.getRegistration(ItemValueProvider.class);
        if (registration != null && registration.getProvider() != null) {
            return registration.getProvider();
        }
        if (valueConfiguration != null && valueConfiguration.enabled()) {
            AuctionValueConfiguration.Mode mode = valueConfiguration.mode();
            if (mode == AuctionValueConfiguration.Mode.EZSHOPS_BUY || mode == AuctionValueConfiguration.Mode.EZSHOPS_SELL) {
                ItemValueProvider provider = EzShopsItemValueProvider.create(plugin, mode);
                if (provider != null) {
                    return provider;
                }
                plugin.getLogger().warning("EzShops price service unavailable; disabling shop value display.");
                return ItemValueProvider.none();
            }
            return new ConfiguredItemValueProvider(valueConfiguration);
        }
        return ItemValueProvider.none();
    }

    /**
     * Resolves the shop price value provider for overlays, if available.
     *
     * @param plugin The plugin instance
     * @param shopPriceConfiguration The shop price configuration
     * @return the shop price value provider
     */
    private ItemValueProvider resolveShopPriceValueProvider(EzAuctionPlugin plugin, AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration) {
        if (shopPriceConfiguration == null) {
            return ItemValueProvider.none();
        }
        ItemValueProvider provider = EzShopsItemValueProvider.create(plugin, shopPriceConfiguration.mode());
        if (provider != null) {
            return provider;
        }
        plugin.getLogger().warning("EzShops price service unavailable; disabling shop price display.");
        return ItemValueProvider.none();
    }

    /**
     * Determines if the shop price overlay should be enabled based on configuration and plugin availability.
     *
     * @param plugin The plugin instance
     * @param shopPriceConfiguration The shop price configuration
     * @return true if the overlay should be enabled, false otherwise
     */
    private boolean shouldEnableShopPriceOverlay(EzAuctionPlugin plugin, AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration) {
        if (shopPriceConfiguration == null) {
            return false;
        }
        if (shopPriceConfiguration.autoDetect()) {
            Plugin ezShops = plugin.getServer().getPluginManager().getPlugin("EzShops");
            return ezShops != null && ezShops.isEnabled();
        }
        return shopPriceConfiguration.enabled();
    }
}
