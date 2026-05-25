package com.skyblockexp.ezauction.gui.menu;

import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMenuConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.gui.holder.AbstractAuctionHolder;
import com.skyblockexp.ezauction.gui.holder.BrowserMenuHolder;
import com.skyblockexp.ezauction.gui.holder.ConfirmMenuHolder;
import com.skyblockexp.ezauction.gui.holder.PreviewMenuHolder;
import com.skyblockexp.ezauction.gui.menu.browser.BrowserActions;
import com.skyblockexp.ezauction.gui.menu.browser.BrowserClickHandler;
import com.skyblockexp.ezauction.gui.menu.browser.BrowserMenuBuilder;
import com.skyblockexp.ezauction.gui.menu.browser.BrowserMenuContext;
import com.skyblockexp.ezauction.gui.menu.browser.BrowserSearchHandler;
import com.skyblockexp.ezauction.gui.menu.browser.OpenBrowserCallback;
import com.skyblockexp.ezauction.gui.state.SearchPrompt;
import com.skyblockexp.ezauction.gui.view.BrowserView;
import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;

/**
 * Displays the active auction listings in an interactive GUI.
 *
 * <p>This class acts as the event-dispatcher coordinator. Heavy lifting is delegated
 * to {@link BrowserMenuBuilder}, {@link BrowserSearchHandler}, and {@link BrowserClickHandler}.
 */
public class AuctionMenu implements Listener {

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionMenuConfiguration menuConfiguration;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration browserConfig;
    private final AuctionMessageConfiguration.BrowserMessages messages;
    private final AuctionMenuConfiguration.ConfirmMenuConfiguration confirmConfig;
    private final AuctionMenuConfiguration.ToggleButtonConfiguration listingsToggleConfig;
    private final AuctionMenuConfiguration.ToggleButtonConfiguration ordersToggleConfig;
    private final AuctionMenuConfiguration.ToggleButtonConfiguration teamListingsToggleConfig;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration.SearchButtonConfiguration searchButtonConfig;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration.SortButtonConfiguration sortButtonConfig;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration.SearchTipsButtonConfiguration searchTipsButtonConfig;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration.ClaimsButtonConfiguration claimsButtonConfig;
    private final AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration confirmButtonConfig;
    private final AuctionMenuConfiguration.ConfirmMenuConfiguration.ButtonConfiguration cancelButtonConfig;
    private final AuctionValueConfiguration valueConfiguration;
    private final ItemValueProvider itemValueProvider;
    private final ItemValueProvider shopPriceProvider;
    private final boolean valueDisplayEnabled;
    private final String valueDisplayFormat;
    private final boolean shopPriceDisplayEnabled;
    private final String shopPriceFormat;

    private final String actionKey;
    private final String listingKey;
    private final String actionTypeKey;
    private final ItemTagStorage itemTagStorage;

    private final ItemStack browserFiller;
    private final ItemStack confirmFiller;
    private final int listingsPerPage;

    private AuctionActivityMenu activityMenu;
    private volatile ClaimMenu claimMenu;
    private volatile LiveAuctionPickerMenu liveAuctionPickerMenu;

    // Helpers (created in constructor after all fields are initialised)
    private final BrowserSearchHandler searchHandler;
    private final BrowserMenuBuilder menuBuilder;
    private final BrowserClickHandler clickHandler;

    public AuctionMenu(JavaPlugin plugin, AuctionManager auctionManager,
            AuctionTransactionService transactionService, AuctionMenuConfiguration menuConfiguration,
            AuctionMessageConfiguration.BrowserMessages messages, AuctionValueConfiguration valueConfiguration,
            ItemValueProvider itemValueProvider, ItemValueProvider shopPriceProvider,
            boolean shopPriceDisplayEnabled, ItemTagStorage itemTagStorage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.menuConfiguration = Objects.requireNonNull(menuConfiguration, "menuConfiguration");
        this.browserConfig = Objects.requireNonNull(menuConfiguration.browser(), "browserConfig");
        this.messages = messages != null ? messages : AuctionMessageConfiguration.BrowserMessages.defaults();
        this.confirmConfig = Objects.requireNonNull(menuConfiguration.confirm(), "confirmConfig");
        this.listingsToggleConfig = this.browserConfig.listingsToggle();
        this.ordersToggleConfig = this.browserConfig.ordersToggle();
        this.teamListingsToggleConfig = this.browserConfig.teamListingsToggle();
        this.searchButtonConfig = this.browserConfig.searchButton();
        this.sortButtonConfig = this.browserConfig.sortButton();
        this.searchTipsButtonConfig = this.browserConfig.searchTipsButton();
        this.claimsButtonConfig = this.browserConfig.claimsButton();
        this.confirmButtonConfig = this.confirmConfig.confirmButton();
        this.cancelButtonConfig = this.confirmConfig.cancelButton();
        this.valueConfiguration = valueConfiguration != null ? valueConfiguration : AuctionValueConfiguration.defaults();
        this.itemValueProvider = itemValueProvider != null ? itemValueProvider : ItemValueProvider.none();
        this.shopPriceProvider = shopPriceProvider != null ? shopPriceProvider : ItemValueProvider.none();
        this.valueDisplayEnabled = this.valueConfiguration.enabled();
        this.valueDisplayFormat = this.valueConfiguration.format();
        AuctionValueConfiguration.ShopPriceConfiguration shopPriceConfiguration =
                this.valueConfiguration.shopPriceConfiguration();
        this.shopPriceDisplayEnabled = shopPriceDisplayEnabled;
        this.shopPriceFormat = shopPriceConfiguration != null ? shopPriceConfiguration.format() : null;
        this.actionKey = "auction_action";
        this.listingKey = "auction_listing";
        this.actionTypeKey = "auction_action_type";
        this.itemTagStorage = Objects.requireNonNull(itemTagStorage, "itemTagStorage");

        // Build a temporary context (without filler items) to create filler items themselves.
        BrowserMenuContext tmpCtx = buildContext(null, null, 1);
        BrowserMenuBuilder tmpBuilder = new BrowserMenuBuilder(tmpCtx, null);
        this.browserFiller = tmpBuilder.createConfiguredFiller(this.browserConfig.filler());
        this.confirmFiller = tmpBuilder.createConfiguredFiller(this.confirmConfig.filler());
        this.listingsPerPage = Math.max(1, Math.min(this.browserConfig.size(), this.browserConfig.size() - 9));

        BrowserMenuContext ctx = buildContext(this.browserFiller, this.confirmFiller, this.listingsPerPage);
        OpenBrowserCallback openCb = this::openBrowserInternal;
        this.searchHandler = new BrowserSearchHandler(ctx, openCb);
        this.menuBuilder = new BrowserMenuBuilder(ctx, this.searchHandler);
        this.clickHandler = new BrowserClickHandler(ctx, menuBuilder, searchHandler, openCb,
                () -> activityMenu, () -> claimMenu, () -> liveAuctionPickerMenu);
    }

    // -------------------------------------------------------------------------
    // Post-construction wiring
    // -------------------------------------------------------------------------

    /**
     * Sets the activity menu reference (to avoid circular dependency during construction).
     */
    public void setActivityMenu(AuctionActivityMenu activityMenu) {
        this.activityMenu = activityMenu;
    }

    public void setClaimMenu(ClaimMenu claimMenu) {
        this.claimMenu = claimMenu;
    }

    public void setLiveAuctionPickerMenu(LiveAuctionPickerMenu liveAuctionPickerMenu) {
        this.liveAuctionPickerMenu = liveAuctionPickerMenu;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens the first page of the auction browser menu for the given player.
     */
    public void openBrowser(Player player) {
        openBrowser(player, BrowserView.LISTINGS, 0);
    }

    public void openBrowser(Player player, BrowserView view, int page) {
        openBrowserInternal(player, view, page);
    }

    /**
     * Programmatically sets a pre-applied search query for the next browser open.
     * Used by command-line search (e.g. {@code /auction search <query>}).
     */
    public void setSearchQuery(UUID playerId, String query) {
        searchHandler.setSearchQuery(playerId, query);
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SearchPrompt prompt = searchHandler.getPendingSearchInput(playerId);
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        searchHandler.removePendingSearchInput(playerId);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> searchHandler.handleSearchInput(event.getPlayer(), prompt, event.getMessage()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        searchHandler.clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof AbstractAuctionHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        int topSize = topInventory.getSize();
        boolean affectsTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (affectsTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory topInventory = event.getInventory();
        if (!(topInventory.getHolder() instanceof PreviewMenuHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin,
                () -> clickHandler.reopenPreviewOrigin(player, holder));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof AbstractAuctionHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != holder) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String action = itemTagStorage.getString(clicked, actionKey);

        if (holder instanceof BrowserMenuHolder browserHolder) {
            clickHandler.handleBrowserClick(player, browserHolder, action, clicked, event);
        } else if (holder instanceof ConfirmMenuHolder confirmHolder) {
            if (BrowserActions.LISTING.equalsIgnoreCase(action) || BrowserActions.ORDER.equalsIgnoreCase(action)) {
                clickHandler.handlePreviewClick(player, confirmHolder, action, clicked, event);
            } else {
                clickHandler.handleConfirmClick(player, confirmHolder, action, clicked);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void openBrowserInternal(Player player, BrowserView view, int page) {
        player.openInventory(menuBuilder.buildBrowserInventory(player, view, page));
    }

    private BrowserMenuContext buildContext(ItemStack browserFiller, ItemStack confirmFiller, int listingsPerPage) {
        return new BrowserMenuContext(
                plugin, auctionManager, transactionService,
                menuConfiguration, browserConfig, messages, confirmConfig,
                listingsToggleConfig, ordersToggleConfig, teamListingsToggleConfig,
                searchButtonConfig, sortButtonConfig, searchTipsButtonConfig, claimsButtonConfig,
                confirmButtonConfig, cancelButtonConfig,
                valueConfiguration, itemValueProvider, shopPriceProvider,
                valueDisplayEnabled, valueDisplayFormat, shopPriceDisplayEnabled, shopPriceFormat,
                actionKey, listingKey, actionTypeKey, itemTagStorage,
                browserFiller, confirmFiller, listingsPerPage);
    }

    // -------------------------------------------------------------------------
    // Inner sort enums — kept for backward compatibility with any callers that
    // reference AuctionMenu.ListingSort / AuctionMenu.OrderSort by name.
    // They delegate entirely to the canonical gui.view enums.
    // -------------------------------------------------------------------------

    private enum ListingSort {
        ENDING_SOON, NEWLY_LISTED, PRICE_LOW_HIGH, PRICE_HIGH_LOW,
        QUANTITY_HIGH_LOW, QUANTITY_LOW_HIGH, ITEM_A_Z, ITEM_Z_A;

        public String label() {
            return com.skyblockexp.ezauction.gui.view.ListingSort.valueOf(name()).label();
        }

        public void sort(java.util.List<com.skyblockexp.ezauction.model.AuctionListing> listings) {
            com.skyblockexp.ezauction.gui.view.ListingSort.valueOf(name()).sort(listings);
        }

        public ListingSort next() {
            return ListingSort.valueOf(
                    com.skyblockexp.ezauction.gui.view.ListingSort.valueOf(name()).next().name());
        }

        public ListingSort previous() {
            return ListingSort.valueOf(
                    com.skyblockexp.ezauction.gui.view.ListingSort.valueOf(name()).previous().name());
        }
    }

    private enum OrderSort {
        ENDING_SOON, NEWLY_POSTED, PRICE_HIGH_LOW, PRICE_LOW_HIGH,
        QUANTITY_HIGH_LOW, QUANTITY_LOW_HIGH, ITEM_A_Z, ITEM_Z_A;

        public String label() {
            return com.skyblockexp.ezauction.gui.view.OrderSort.valueOf(name()).label();
        }

        public void sort(java.util.List<com.skyblockexp.ezauction.model.AuctionOrder> orders) {
            com.skyblockexp.ezauction.gui.view.OrderSort.valueOf(name()).sort(orders);
        }

        public OrderSort next() {
            return OrderSort.valueOf(
                    com.skyblockexp.ezauction.gui.view.OrderSort.valueOf(name()).next().name());
        }

        public OrderSort previous() {
            return OrderSort.valueOf(
                    com.skyblockexp.ezauction.gui.view.OrderSort.valueOf(name()).previous().name());
        }
    }
}
