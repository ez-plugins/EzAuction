package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.command.AuctionCommand;
import com.skyblockexp.ezauction.command.AuctionHologramCommand;
import com.skyblockexp.ezauction.compat.HologramPlatform;

/**
 * Handles GUI and command registration for the plugin.
 * (Stub for further expansion.)
 */
import com.skyblockexp.ezauction.config.*;
import com.skyblockexp.ezauction.gui.*;
import com.skyblockexp.ezauction.hologram.*;
import com.skyblockexp.ezauction.live.*;
import com.skyblockexp.ezauction.placeholder.*;
import com.skyblockexp.ezauction.util.*;
import org.bukkit.plugin.Plugin;
import org.bstats.bukkit.Metrics;
import com.skyblockexp.ezframework.bootstrap.Component;
import com.skyblockexp.ezframework.Registry;

public class GuiAndCommandSetupComponent implements Component {
    public static class GuiSetupResult {
        public final AuctionMenu auctionMenu;
        public final LiveAuctionMenu liveAuctionMenu;
        public final AuctionOrderMenu auctionOrderMenu;
        public final AuctionSellMenu auctionSellMenu;
        public final AuctionActivityMenu auctionActivityMenu;
        public final AuctionCommand auctionCommand;
        public final AuctionHologramManager hologramManager;
        public final AuctionHologramCommand hologramCommand;
        public final ItemValueProvider itemValueProvider;
        public final ItemValueProvider shopPriceValueProvider;
        public final Object placeholderExpansion; // Use Object to avoid class loading
        public final Metrics metrics;
        public GuiSetupResult(
            AuctionMenu auctionMenu,
            LiveAuctionMenu liveAuctionMenu,
            AuctionOrderMenu auctionOrderMenu,
            AuctionSellMenu auctionSellMenu,
            AuctionActivityMenu auctionActivityMenu,
            AuctionCommand auctionCommand,
            AuctionHologramManager hologramManager,
            AuctionHologramCommand hologramCommand,
            ItemValueProvider itemValueProvider,
            ItemValueProvider shopPriceValueProvider,
            Object placeholderExpansion,
            Metrics metrics
        ) {
            this.auctionMenu = auctionMenu;
            this.liveAuctionMenu = liveAuctionMenu;
            this.auctionOrderMenu = auctionOrderMenu;
            this.auctionSellMenu = auctionSellMenu;
            this.auctionActivityMenu = auctionActivityMenu;
            this.auctionCommand = auctionCommand;
            this.hologramManager = hologramManager;
            this.hologramCommand = hologramCommand;
            this.itemValueProvider = itemValueProvider;
            this.shopPriceValueProvider = shopPriceValueProvider;
            this.placeholderExpansion = placeholderExpansion;
            this.metrics = metrics;
        }
    }

    private EzAuctionPlugin plugin;
    private AuctionConfiguration configuration;
    private AuctionManager auctionManager;
    private com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService;
    private com.skyblockexp.ezauction.transaction.AuctionTransactionHistory transactionHistory;
    private LiveAuctionService liveAuctionService;
    private com.skyblockexp.ezauction.compat.CompatibilityFacade compatibilityFacade;
    private com.skyblockexp.ezauction.compat.ItemTagStorage itemTagStorage;
    private GuiSetupResult result;

    public GuiAndCommandSetupComponent(
        EzAuctionPlugin plugin,
        AuctionConfiguration configuration,
        AuctionManager auctionManager,
        com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService,
        com.skyblockexp.ezauction.transaction.AuctionTransactionHistory transactionHistory,
        LiveAuctionService liveAuctionService,
        com.skyblockexp.ezauction.compat.CompatibilityFacade compatibilityFacade,
        com.skyblockexp.ezauction.compat.ItemTagStorage itemTagStorage
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.auctionManager = auctionManager;
        this.transactionService = transactionService;
        this.transactionHistory = transactionHistory;
        this.liveAuctionService = liveAuctionService;
        this.compatibilityFacade = compatibilityFacade;
        this.itemTagStorage = itemTagStorage;
    }

    public GuiAndCommandSetupComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
        this.configuration = null;
        this.auctionManager = null;
        this.transactionService = null;
        this.transactionHistory = null;
        this.liveAuctionService = null;
        this.compatibilityFacade = null;
        this.itemTagStorage = null;
    }

    @Override
    public void start() throws Exception {
        // Resolve missing dependencies from the framework registry if not provided at construction
        try { if (this.configuration == null) try { this.configuration = Registry.forPlugin(plugin).get(AuctionConfiguration.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.auctionManager == null) try { this.auctionManager = Registry.forPlugin(plugin).get(AuctionManager.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.transactionService == null) try { this.transactionService = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.transaction.AuctionTransactionService.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.transactionHistory == null) try { this.transactionHistory = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.transaction.AuctionTransactionHistory.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.liveAuctionService == null) try { this.liveAuctionService = Registry.forPlugin(plugin).get(LiveAuctionService.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.compatibilityFacade == null) try { this.compatibilityFacade = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.compat.CompatibilityFacade.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        try { if (this.itemTagStorage == null) try { this.itemTagStorage = Registry.forPlugin(plugin).get(com.skyblockexp.ezauction.compat.ItemTagStorage.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        AuctionValueConfiguration valueConfiguration = this.configuration.valueConfiguration();
        AuctionMessageConfiguration messageConfiguration = configuration.messageConfiguration();
        AuctionCommandMessageConfiguration commandMessageConfiguration = configuration.commandMessageConfiguration();
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration = valueConfiguration != null ? valueConfiguration.shopPriceConfiguration() : null;
        ItemValueProvider itemValueProvider = resolveItemValueProvider(this.plugin, valueConfiguration);
        boolean shopPriceOverlayEnabled = shouldEnableShopPriceOverlay(plugin, shopPriceConfiguration);
        ItemValueProvider shopPriceValueProvider = shopPriceOverlayEnabled ? resolveShopPriceValueProvider(this.plugin, shopPriceConfiguration) : ItemValueProvider.none();
        if (shopPriceOverlayEnabled && shopPriceValueProvider == ItemValueProvider.none()) {
            shopPriceOverlayEnabled = false;
        }
        ItemValueProvider recommendationProvider = itemValueProvider;
        if ((valueConfiguration == null || !valueConfiguration.enabled()) && shopPriceOverlayEnabled) {
            recommendationProvider = shopPriceValueProvider;
        }
        AuctionMenu auctionMenu = new AuctionMenu(this.plugin, this.auctionManager, this.transactionService, this.configuration.menuConfiguration(), messageConfiguration.browser(), valueConfiguration, itemValueProvider, shopPriceValueProvider, shopPriceOverlayEnabled, this.itemTagStorage);
        LiveAuctionMenu liveAuctionMenu = new LiveAuctionMenu(this.plugin, this.auctionManager, this.transactionService, auctionMenu, this.liveAuctionService, messageConfiguration.live(), valueConfiguration, shopPriceValueProvider, shopPriceOverlayEnabled, this.itemTagStorage);
        AuctionMenuInteractionConfiguration menuInteractions = this.configuration.menuInteractionConfiguration();
        AuctionOrderMenu auctionOrderMenu = new AuctionOrderMenu(this.plugin, this.auctionManager, this.transactionService, this.configuration, this.configuration.listingRules(), this.configuration.durationOptions(), menuInteractions.orderMenu(), recommendationProvider, messageConfiguration.order(), this.itemTagStorage);
        AuctionSellMenu auctionSellMenu = new AuctionSellMenu(this.plugin, this.auctionManager, this.transactionService, this.configuration.listingRules(), this.configuration.durationOptions(), menuInteractions.sellMenu(), recommendationProvider, messageConfiguration.sell(), this.itemTagStorage);
        AuctionActivityMenu auctionActivityMenu = new AuctionActivityMenu(this.plugin, this.auctionManager, this.transactionService, this.transactionHistory, auctionMenu, this.itemTagStorage, messageConfiguration.browser(), messageConfiguration.activity());
        auctionMenu.setActivityMenu(auctionActivityMenu);
        AuctionCommand auctionCommand = new AuctionCommand(this.auctionManager, auctionMenu, auctionSellMenu, auctionOrderMenu, this.transactionHistory, this.transactionService, this.configuration.listingRules(), liveAuctionMenu, commandMessageConfiguration);
        AuctionHologramConfiguration hologramConfiguration = configuration.hologramConfiguration();
        HologramPlatform hologramPlatform = compatibilityFacade.hologramPlatform();
        boolean hologramSupportAvailable = hologramPlatform.isSupported();
        AuctionHologramManager hologramManager = null;
        AuctionHologramCommand hologramCommand = null;
        if (hologramConfiguration != null && hologramConfiguration.enabled() && hologramSupportAvailable) {
            hologramManager = new AuctionHologramManager(this.plugin, this.auctionManager, this.transactionService, hologramConfiguration, hologramPlatform);
            hologramManager.enable();
            hologramCommand = new AuctionHologramCommand(hologramManager, commandMessageConfiguration.holograms());
        } else if (hologramConfiguration != null && hologramConfiguration.enabled()) {
            this.plugin.getLogger().warning("Auction holograms requested but unavailable on this platform; feature will remain disabled.");
        }
        Plugin placeholderApi = this.plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        Object tempExpansion = null;
        if (placeholderApi != null && placeholderApi.isEnabled()) {
            try {
                Class<?> expansionClass = Class.forName("com.skyblockexp.ezauction.placeholder.EzAuctionPlaceholderExpansion");
                Object expansion = expansionClass.getConstructor(
                    com.skyblockexp.ezauction.AuctionManager.class,
                    org.bukkit.plugin.PluginDescriptionFile.class
                ).newInstance(this.auctionManager, this.plugin.getDescription());
                boolean registered = (boolean) expansionClass.getMethod("register").invoke(expansion);
                if (registered) {
                    tempExpansion = (EzAuctionPlaceholderExpansion) expansion;
                } else {
                    this.plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders for EzAuction.");
                }
            } catch (ClassNotFoundException e) {
                this.plugin.getLogger().warning("PlaceholderAPI expansion class not found. Skipping placeholder registration.");
            } catch (Throwable t) {
                this.plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders for EzAuction: " + t.getMessage());
            }
        }
        Metrics tempMetrics = null;
        try {
            tempMetrics = new Metrics(this.plugin, 27737);
        } catch (Throwable throwable) {
            this.plugin.getLogger().warning("Failed to start bStats metrics: " + throwable.getMessage());
        }
        this.result = new GuiSetupResult(
            auctionMenu,
            liveAuctionMenu,
            auctionOrderMenu,
            auctionSellMenu,
            auctionActivityMenu,
            auctionCommand,
            hologramManager,
            hologramCommand,
            itemValueProvider,
            shopPriceValueProvider,
            tempExpansion,
            tempMetrics
        );
        // register GUI and command managers for discovery via EzFramework
        try {
            Registry.forPlugin(plugin).register(AuctionMenu.class, auctionMenu);
            Registry.forPlugin(plugin).register(LiveAuctionMenu.class, liveAuctionMenu);
            Registry.forPlugin(plugin).register(AuctionOrderMenu.class, auctionOrderMenu);
            Registry.forPlugin(plugin).register(AuctionSellMenu.class, auctionSellMenu);
            Registry.forPlugin(plugin).register(AuctionActivityMenu.class, auctionActivityMenu);
            Registry.forPlugin(plugin).register(AuctionCommand.class, auctionCommand);
            if (hologramManager != null) Registry.forPlugin(plugin).register(AuctionHologramManager.class, hologramManager);
            if (hologramCommand != null) Registry.forPlugin(plugin).register(AuctionHologramCommand.class, hologramCommand);
            Registry.forPlugin(plugin).register(ItemValueProvider.class, itemValueProvider);
            Registry.forPlugin(plugin).register(ItemValueProvider.class, shopPriceValueProvider);
        } catch (Throwable ignored) {}
    }

    @Override
    public void stop() throws Exception {
        // no-op for setup component
    }

    public GuiSetupResult getResult() {
        return this.result;
    }

    private static ItemValueProvider resolveItemValueProvider(EzAuctionPlugin plugin, AuctionValueConfiguration valueConfiguration) {
        org.bukkit.plugin.ServicesManager servicesManager = plugin.getServer().getServicesManager();
        org.bukkit.plugin.RegisteredServiceProvider<ItemValueProvider> registration = servicesManager.getRegistration(ItemValueProvider.class);
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

    private static ItemValueProvider resolveShopPriceValueProvider(EzAuctionPlugin plugin, AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration) {
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

    private static boolean shouldEnableShopPriceOverlay(EzAuctionPlugin plugin, AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration) {
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
