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
import com.skyblockexp.ezauction.bootstrap.component.ConfigurationLoaderComponent;
import com.skyblockexp.ezauction.bootstrap.component.EconomySetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.CompatibilityAndStorageComponent;
import com.skyblockexp.ezauction.bootstrap.component.ServiceSetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.GuiAndCommandSetupComponent;
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
    public boolean historyGuiEnabled;
    /** Vault economy provider. */
    public Economy economy;
    /** Handles all auction transactions. */
    public AuctionTransactionService transactionService;
    /** Stores and manages auction transaction history. */
    public AuctionTransactionHistory transactionHistory;
    /** Main auction manager for listings and logic. */
    public AuctionManager auctionManager;
    /** Storage for active auction listings. */
    public AuctionStorage listingStorage;
    /** Storage for auction history. */
    public AuctionHistoryStorage historyStorage;
    /** Main configuration for the plugin. */
    public AuctionConfiguration configuration;
    /** Main auction browser GUI. */
    public AuctionMenu auctionMenu;
    /** Provider for item value recommendations. */
    public ItemValueProvider itemValueProvider;
    /** Provider for shop price overlays. */
    public ItemValueProvider shopPriceValueProvider;
    /** GUI for managing auction orders. */
    public AuctionOrderMenu auctionOrderMenu;
    /** GUI for selling items in auctions. */
    public AuctionSellMenu auctionSellMenu;
    /** Consolidated activity menu. */
    public AuctionActivityMenu auctionActivityMenu;
    /** Main /auction command handler. */
    public AuctionCommand auctionCommand;
    /** GUI for live auctions. */
    public LiveAuctionMenu liveAuctionMenu;
    /** Hologram manager for auction displays. */
    public AuctionHologramManager hologramManager;
    /** Command handler for auction holograms. */
    public AuctionHologramCommand hologramCommand;
    /** Handles version/platform compatibility. */
    public CompatibilityFacade compatibilityFacade;
    /** Item tag storage for persistent item data. */
    public ItemTagStorage itemTagStorage;
    /** PlaceholderAPI expansion for EzAuction. */
    public Object placeholderExpansion;
    /** bStats metrics instance. */
    public Metrics metrics;
    /** Service for live auction events. */
    public LiveAuctionService liveAuctionService;
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
        this.configuration = null;
        this.economy = null;
        this.compatibilityFacade = null;
        this.itemTagStorage = null;
        this.listingStorage = null;
        this.historyStorage = null;
        this.historyGuiEnabled = false;
        this.transactionService = null;
        this.transactionHistory = null;
        this.liveAuctionService = null;
        this.auctionManager = null;
        this.pendingReturns = null;
        this.auctionMenu = null;
        this.liveAuctionMenu = null;
        this.auctionOrderMenu = null;
        this.auctionSellMenu = null;
        this.auctionActivityMenu = null;
        this.auctionCommand = null;
        this.hologramManager = null;
        this.hologramCommand = null;
        this.itemValueProvider = null;
        this.shopPriceValueProvider = null;
        this.placeholderExpansion = null;
        this.metrics = null;
    }

    /**
     * Loads and initializes the PluginRegistry. This should be called after the PluginRegistry object is constructed.
     */
    public void load() {
        // 1. Load configuration
        ConfigurationLoaderComponent configLoader = new ConfigurationLoaderComponent();
        AuctionConfiguration configuration = configLoader.load(plugin);

        // 2. Setup economy
        EconomySetupComponent economySetup = new EconomySetupComponent();
        Economy economy = economySetup.setup(plugin);

        // 3. Compatibility and storage
        CompatibilityAndStorageComponent compatAndStorage = new CompatibilityAndStorageComponent();
        CompatibilityAndStorageComponent.Result compatResult = compatAndStorage.setup(plugin, configuration);
        CompatibilityFacade compatibilityFacade = compatResult.compatibilityFacade;
        ItemTagStorage itemTagStorage = compatResult.compatibilityFacade.itemTagStorage();
        HologramPlatform hologramPlatform = compatibilityFacade.hologramPlatform();
        boolean hologramSupportAvailable = hologramPlatform.isSupported();
        if (!hologramSupportAvailable && configuration.hologramConfiguration().enabled()) {
            plugin.getLogger().warning("TextDisplay entities are unavailable on this server version. EzAuction holograms will be disabled.");
        }
        AuctionStorage listingStorage = compatResult.listingStorage;
        AuctionHistoryStorage historyStorage = compatResult.historyStorage;

        // 4. Determine if history GUI is enabled
        boolean guiEnabled = false;
        try {
            if (configuration != null) {
                guiEnabled = configuration.isHistoryGuiEnabled();
            }
        } catch (Exception ignored) {}

        // 5. Service and manager setup
        ServiceSetupComponent.ServiceSetupResult serviceResult = ServiceSetupComponent.setupAll(
            plugin, configuration, economy, listingStorage, historyStorage, compatibilityFacade
        );

        // 6. GUI and command setup
        GuiAndCommandSetupComponent.GuiSetupResult guiResult = GuiAndCommandSetupComponent.setupAll(
            plugin, configuration, serviceResult.auctionManager, serviceResult.transactionService, serviceResult.transactionHistory, serviceResult.liveAuctionService, compatibilityFacade, itemTagStorage
        );

        // Assign all fields via reflection (Java limitation workaround for staged init)
        setField("configuration", configuration);
        setField("economy", economy);
        setField("compatibilityFacade", compatibilityFacade);
        setField("itemTagStorage", itemTagStorage);
        setField("listingStorage", listingStorage);
        setField("historyStorage", historyStorage);
        setField("historyGuiEnabled", guiEnabled);
        setField("transactionService", serviceResult.transactionService);
        setField("transactionHistory", serviceResult.transactionHistory);
        setField("liveAuctionService", serviceResult.liveAuctionService);
        setField("auctionManager", serviceResult.auctionManager);
        setField("pendingReturns", serviceResult.pendingReturns);
        setField("auctionMenu", guiResult.auctionMenu);
        setField("liveAuctionMenu", guiResult.liveAuctionMenu);
        setField("auctionOrderMenu", guiResult.auctionOrderMenu);
        setField("auctionSellMenu", guiResult.auctionSellMenu);
        setField("auctionActivityMenu", guiResult.auctionActivityMenu);
        setField("auctionCommand", guiResult.auctionCommand);
        setField("hologramManager", guiResult.hologramManager);
        setField("hologramCommand", guiResult.hologramCommand);
        setField("itemValueProvider", guiResult.itemValueProvider);
        setField("shopPriceValueProvider", guiResult.shopPriceValueProvider);
        setField("placeholderExpansion", guiResult.placeholderExpansion);
        setField("metrics", guiResult.metrics);
    }

    // Helper to set final fields via reflection (Java limitation workaround for staged init)
    private void setField(String name, Object value) {
        try {
            java.lang.reflect.Field f = PluginRegistry.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(this, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field '" + name + "' in PluginRegistry", e);
        }
    }

    /**
     * Registers all event listeners and commands for the plugin.
     * Should be called from EzAuctionPlugin.onEnable().
     */
    public void enableAll() {
        plugin.getServer().getPluginManager().registerEvents(auctionOrderMenu, plugin);
        PluginCommand ordersCmd = plugin.getCommand("orders");
        if (ordersCmd != null) {
            OrdersCommand ordersCommand = new OrdersCommand(auctionOrderMenu);
            ordersCmd.setExecutor(ordersCommand);
            ordersCmd.setTabCompleter(ordersCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'orders' is not defined in plugin.yml; orders-only mode will not work.");
        }
        PluginCommand orderCmd = plugin.getCommand("order");
        if (orderCmd != null) {
            OrderCommand orderCommand = new OrderCommand(auctionOrderMenu);
            orderCmd.setExecutor(orderCommand);
            orderCmd.setTabCompleter(orderCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'order' is not defined in plugin.yml; orders-only mode will not work.");
        }
        
        if (OrdersOnlyConfig.isOrdersOnlyMode()) {
            // Orders-only mode: only register /orders, /order and order menu
            // Do not register auction, liveauction, hologram, or other auction features
            return;
        }
        
        // Normal mode: register all features
        plugin.getServer().getPluginManager().registerEvents(auctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(liveAuctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(auctionOrderMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionActivityMenuListener(auctionActivityMenu, auctionMenu, itemTagStorage), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionReturnListener(new AuctionClaimService(
            pendingReturns,
            configuration.backendMessages()
        )), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionSellMenuListener(auctionSellMenu), plugin);

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
        // Only attempt to unregister if PlaceholderAPI is present
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.getClass().getMethod("unregister").invoke(placeholderExpansion);
            } catch (Throwable ignored) {}
        }
        
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
     * Gets the main plugin configuration.
     *
     * @return The AuctionConfiguration instance
     */
    public AuctionConfiguration getConfiguration() {
        return this.configuration;
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
