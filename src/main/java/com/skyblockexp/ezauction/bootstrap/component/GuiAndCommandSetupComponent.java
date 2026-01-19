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

public class GuiAndCommandSetupComponent {
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

    public static GuiSetupResult setupAll(
        EzAuctionPlugin plugin,
        AuctionConfiguration configuration,
        AuctionManager auctionManager,
        com.skyblockexp.ezauction.transaction.AuctionTransactionService transactionService,
        com.skyblockexp.ezauction.transaction.AuctionTransactionHistory transactionHistory,
        LiveAuctionService liveAuctionService,
        com.skyblockexp.ezauction.compat.CompatibilityFacade compatibilityFacade,
        com.skyblockexp.ezauction.compat.ItemTagStorage itemTagStorage
    ) {
        AuctionValueConfiguration valueConfiguration = configuration.valueConfiguration();
        AuctionMessageConfiguration messageConfiguration = configuration.messageConfiguration();
        AuctionCommandMessageConfiguration commandMessageConfiguration = configuration.commandMessageConfiguration();
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration = valueConfiguration != null ? valueConfiguration.shopPriceConfiguration() : null;
        ItemValueProvider itemValueProvider = resolveItemValueProvider(plugin, valueConfiguration);
        boolean shopPriceOverlayEnabled = shouldEnableShopPriceOverlay(plugin, shopPriceConfiguration);
        ItemValueProvider shopPriceValueProvider = shopPriceOverlayEnabled ? resolveShopPriceValueProvider(plugin, shopPriceConfiguration) : ItemValueProvider.none();
        if (shopPriceOverlayEnabled && shopPriceValueProvider == ItemValueProvider.none()) {
            shopPriceOverlayEnabled = false;
        }
        ItemValueProvider recommendationProvider = itemValueProvider;
        if ((valueConfiguration == null || !valueConfiguration.enabled()) && shopPriceOverlayEnabled) {
            recommendationProvider = shopPriceValueProvider;
        }
        AuctionMenu auctionMenu = new AuctionMenu(plugin, auctionManager, transactionService, configuration.menuConfiguration(), messageConfiguration.browser(), valueConfiguration, itemValueProvider, shopPriceValueProvider, shopPriceOverlayEnabled, itemTagStorage);
        LiveAuctionMenu liveAuctionMenu = new LiveAuctionMenu(plugin, auctionManager, transactionService, auctionMenu, liveAuctionService, messageConfiguration.live(), valueConfiguration, shopPriceValueProvider, shopPriceOverlayEnabled, itemTagStorage);
        AuctionMenuInteractionConfiguration menuInteractions = configuration.menuInteractionConfiguration();
        AuctionOrderMenu auctionOrderMenu = new AuctionOrderMenu(plugin, auctionManager, transactionService, configuration.listingRules(), configuration.durationOptions(), menuInteractions.orderMenu(), recommendationProvider, messageConfiguration.order(), itemTagStorage);
        AuctionSellMenu auctionSellMenu = new AuctionSellMenu(plugin, auctionManager, transactionService, configuration.listingRules(), configuration.durationOptions(), menuInteractions.sellMenu(), recommendationProvider, messageConfiguration.sell(), itemTagStorage);
        AuctionActivityMenu auctionActivityMenu = new AuctionActivityMenu(plugin, auctionManager, transactionService, transactionHistory, auctionMenu, itemTagStorage, messageConfiguration.browser(), AuctionActivityMenu.ActivityMessages.defaults());
        auctionMenu.setActivityMenu(auctionActivityMenu);
        AuctionCommand auctionCommand = new AuctionCommand(auctionManager, auctionMenu, auctionSellMenu, auctionOrderMenu, transactionHistory, transactionService, configuration.listingRules(), liveAuctionMenu, commandMessageConfiguration);
        AuctionHologramConfiguration hologramConfiguration = configuration.hologramConfiguration();
        HologramPlatform hologramPlatform = compatibilityFacade.hologramPlatform();
        boolean hologramSupportAvailable = hologramPlatform.isSupported();
        AuctionHologramManager hologramManager = null;
        AuctionHologramCommand hologramCommand = null;
        if (hologramConfiguration != null && hologramConfiguration.enabled() && hologramSupportAvailable) {
            hologramManager = new AuctionHologramManager(plugin, auctionManager, transactionService, hologramConfiguration, hologramPlatform);
            hologramManager.enable();
            hologramCommand = new AuctionHologramCommand(hologramManager, commandMessageConfiguration.holograms());
        } else if (hologramConfiguration != null && hologramConfiguration.enabled()) {
            plugin.getLogger().warning("Auction holograms requested but unavailable on this platform; feature will remain disabled.");
        }
        Plugin placeholderApi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        Object tempExpansion = null;
        if (placeholderApi != null && placeholderApi.isEnabled()) {
            try {
                Class<?> expansionClass = Class.forName("com.skyblockexp.ezauction.placeholder.EzAuctionPlaceholderExpansion");
                Object expansion = expansionClass.getConstructor(
                    com.skyblockexp.ezauction.AuctionManager.class,
                    org.bukkit.plugin.PluginDescriptionFile.class
                ).newInstance(auctionManager, plugin.getDescription());
                boolean registered = (boolean) expansionClass.getMethod("register").invoke(expansion);
                if (registered) {
                    tempExpansion = (EzAuctionPlaceholderExpansion) expansion;
                } else {
                    plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders for EzAuction.");
                }
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("PlaceholderAPI expansion class not found. Skipping placeholder registration.");
            } catch (Throwable t) {
                plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders for EzAuction: " + t.getMessage());
            }
        }
        Metrics tempMetrics = null;
        try {
            tempMetrics = new Metrics(plugin, 27737);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to start bStats metrics: " + throwable.getMessage());
        }
        return new GuiSetupResult(
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
