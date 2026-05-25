package com.skyblockexp.ezauction.gui.menu.browser;

import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMenuConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Immutable snapshot of all config / service dependencies shared by the browser helper classes.
 */
public final class BrowserMenuContext {

    public final JavaPlugin plugin;
    public final AuctionManager auctionManager;
    public final AuctionTransactionService transactionService;
    public final AuctionMenuConfiguration menuConfiguration;
    public final AuctionMenuConfiguration.BrowserMenuConfiguration browserConfig;
    public final AuctionMessageConfiguration.BrowserMessages messages;
    public final AuctionMenuConfiguration.ConfirmMenuConfiguration confirmConfig;
    public final AuctionMenuConfiguration.ToggleButtonConfiguration listingsToggleConfig;
    public final AuctionMenuConfiguration.ToggleButtonConfiguration ordersToggleConfig;
    public final AuctionMenuConfiguration.ToggleButtonConfiguration teamListingsToggleConfig;
    public final AuctionMenuConfiguration.BrowserMenuConfiguration.SearchButtonConfiguration searchButtonConfig;
    public final AuctionMenuConfiguration.BrowserMenuConfiguration.SortButtonConfiguration sortButtonConfig;
    public final AuctionMenuConfiguration.BrowserMenuConfiguration.SearchTipsButtonConfiguration searchTipsButtonConfig;
    public final AuctionMenuConfiguration.BrowserMenuConfiguration.ClaimsButtonConfiguration claimsButtonConfig;
    public final AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration confirmButtonConfig;
    public final AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration cancelButtonConfig;
    public final AuctionValueConfiguration valueConfiguration;
    public final ItemValueProvider itemValueProvider;
    public final ItemValueProvider shopPriceProvider;
    public final boolean valueDisplayEnabled;
    public final String valueDisplayFormat;
    public final boolean shopPriceDisplayEnabled;
    public final String shopPriceFormat;
    public final String actionKey;
    public final String listingKey;
    public final String actionTypeKey;
    public final ItemTagStorage itemTagStorage;
    public final ItemStack browserFiller;
    public final ItemStack confirmFiller;
    public final int listingsPerPage;

    public BrowserMenuContext(
            JavaPlugin plugin,
            AuctionManager auctionManager,
            AuctionTransactionService transactionService,
            AuctionMenuConfiguration menuConfiguration,
            AuctionMenuConfiguration.BrowserMenuConfiguration browserConfig,
            AuctionMessageConfiguration.BrowserMessages messages,
            AuctionMenuConfiguration.ConfirmMenuConfiguration confirmConfig,
            AuctionMenuConfiguration.ToggleButtonConfiguration listingsToggleConfig,
            AuctionMenuConfiguration.ToggleButtonConfiguration ordersToggleConfig,
            AuctionMenuConfiguration.ToggleButtonConfiguration teamListingsToggleConfig,
            AuctionMenuConfiguration.BrowserMenuConfiguration.SearchButtonConfiguration searchButtonConfig,
            AuctionMenuConfiguration.BrowserMenuConfiguration.SortButtonConfiguration sortButtonConfig,
            AuctionMenuConfiguration.BrowserMenuConfiguration.SearchTipsButtonConfiguration searchTipsButtonConfig,
            AuctionMenuConfiguration.BrowserMenuConfiguration.ClaimsButtonConfiguration claimsButtonConfig,
            AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration confirmButtonConfig,
            AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration cancelButtonConfig,
            AuctionValueConfiguration valueConfiguration,
            ItemValueProvider itemValueProvider,
            ItemValueProvider shopPriceProvider,
            boolean valueDisplayEnabled,
            String valueDisplayFormat,
            boolean shopPriceDisplayEnabled,
            String shopPriceFormat,
            String actionKey,
            String listingKey,
            String actionTypeKey,
            ItemTagStorage itemTagStorage,
            ItemStack browserFiller,
            ItemStack confirmFiller,
            int listingsPerPage) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.transactionService = transactionService;
        this.menuConfiguration = menuConfiguration;
        this.browserConfig = browserConfig;
        this.messages = messages;
        this.confirmConfig = confirmConfig;
        this.listingsToggleConfig = listingsToggleConfig;
        this.ordersToggleConfig = ordersToggleConfig;
        this.teamListingsToggleConfig = teamListingsToggleConfig;
        this.searchButtonConfig = searchButtonConfig;
        this.sortButtonConfig = sortButtonConfig;
        this.searchTipsButtonConfig = searchTipsButtonConfig;
        this.claimsButtonConfig = claimsButtonConfig;
        this.confirmButtonConfig = confirmButtonConfig;
        this.cancelButtonConfig = cancelButtonConfig;
        this.valueConfiguration = valueConfiguration;
        this.itemValueProvider = itemValueProvider;
        this.shopPriceProvider = shopPriceProvider;
        this.valueDisplayEnabled = valueDisplayEnabled;
        this.valueDisplayFormat = valueDisplayFormat;
        this.shopPriceDisplayEnabled = shopPriceDisplayEnabled;
        this.shopPriceFormat = shopPriceFormat;
        this.actionKey = actionKey;
        this.listingKey = listingKey;
        this.actionTypeKey = actionTypeKey;
        this.itemTagStorage = itemTagStorage;
        this.browserFiller = browserFiller;
        this.confirmFiller = confirmFiller;
        this.listingsPerPage = listingsPerPage;
    }
}
