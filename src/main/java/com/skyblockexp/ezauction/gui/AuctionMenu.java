package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOperationResult;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMenuConfiguration;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.config.AuctionValueConfiguration;
import com.skyblockexp.ezauction.util.ItemValueProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Method;

/**
 * Displays the active auction listings in an interactive GUI.
 */
public class AuctionMenu implements Listener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withLocale(Locale.ENGLISH);

    private static final String ACTION_LISTING = "listing";
    private static final String ACTION_ORDER = "order";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_CANCEL = "cancel";
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CLOSE = "close";
    private static final String ACTION_TOGGLE_LISTINGS = "toggle_listings";
    private static final String ACTION_TOGGLE_ORDERS = "toggle_orders";
    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_SORT = "sort";
    private static final String ACTION_SEARCH_TIPS = "search_tips";
    private static final String ACTION_CLAIMS = "claims";
    private static final String ACTION_ACTIVITY = "activity";

    private static final int MAX_SEARCH_LENGTH = 48;

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionMenuConfiguration menuConfiguration;
    private final AuctionMenuConfiguration.BrowserMenuConfiguration browserConfig;
    private final AuctionMessageConfiguration.BrowserMessages messages;
    private final AuctionMenuConfiguration.ConfirmMenuConfiguration confirmConfig;
    private final AuctionMenuConfiguration.ToggleButtonConfiguration listingsToggleConfig;
    private final AuctionMenuConfiguration.ToggleButtonConfiguration ordersToggleConfig;
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

    private final ConcurrentMap<UUID, String> activeSearchQueries;
    private final ConcurrentMap<UUID, SearchPrompt> pendingSearchInputs;
    private final ConcurrentMap<UUID, ListingSort> activeListingSorts;
    private final ConcurrentMap<UUID, OrderSort> activeOrderSorts;
    
    private AuctionActivityMenu activityMenu;

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
        this.browserFiller = createConfiguredFiller(browserConfig.filler());
        this.confirmFiller = createConfiguredFiller(confirmConfig.filler());
        this.listingsPerPage = Math.max(1, Math.min(browserConfig.size(), browserConfig.size() - 9));
        this.activeSearchQueries = new ConcurrentHashMap<>();
        this.pendingSearchInputs = new ConcurrentHashMap<>();
        this.activeListingSorts = new ConcurrentHashMap<>();
        this.activeOrderSorts = new ConcurrentHashMap<>();
    }

    /**
     * Sets the activity menu reference (to avoid circular dependency during construction).
     */
    public void setActivityMenu(AuctionActivityMenu activityMenu) {
        this.activityMenu = activityMenu;
    }

    /**
     * Opens the first page of the auction browser menu for the given player.
     */
    public void openBrowser(Player player) {
        openBrowser(player, BrowserView.LISTINGS, 0);
    }

    private void openBrowser(Player player, BrowserView view, int page) {
        UUID playerId = player.getUniqueId();
        String searchQuery = getSearchQuery(playerId);
        String normalizedQuery = searchQuery != null ? searchQuery.toLowerCase(Locale.ENGLISH) : null;

        List<AuctionListing> listings = view == BrowserView.LISTINGS
            ? new ArrayList<>(auctionManager.listActiveListings())
            : Collections.emptyList();
        if (view == BrowserView.LISTINGS && auctionManager != null && auctionManager.getConfiguration() != null && auctionManager.getConfiguration().debug()) {
            System.out.println("[EzAuction][DEBUG] AuctionMenu.openBrowser: listings.size() = " + listings.size() + ", listings = " + listings);
        }
        if (view == BrowserView.LISTINGS) {
            filterListings(listings, normalizedQuery);
        }
        List<AuctionOrder> orders = view == BrowserView.ORDERS
                ? new ArrayList<>(auctionManager.listActiveOrders())
                : Collections.emptyList();
        if (view == BrowserView.ORDERS) {
            filterOrders(orders, normalizedQuery);
        }

        ListingSort listingSort = getListingSort(playerId);
        OrderSort orderSort = getOrderSort(playerId);
        if (view == BrowserView.LISTINGS) {
            listingSort.sort(listings);
        }
        if (view == BrowserView.ORDERS) {
            orderSort.sort(orders);
        }

        int totalEntries = view == BrowserView.LISTINGS ? listings.size() : orders.size();
        int entriesPerPage = Math.max(1, listingsPerPage);
        int totalPages = Math.max(1, (int) Math.ceil(totalEntries / (double) entriesPerPage));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        BrowserMenuHolder holder = new BrowserMenuHolder(playerId, currentPage, view);
        String title = formatBrowserTitle(player, view, currentPage, totalPages);
        Inventory inventory = Bukkit.createInventory(holder, browserConfig.size(), title);
        holder.setInventory(inventory);

        applyFiller(inventory, browserFiller);

        int startIndex = currentPage * entriesPerPage;
        int endIndex = Math.min(startIndex + entriesPerPage, totalEntries);
        if (startIndex >= endIndex) {
            boolean searching = normalizedQuery != null;
            String emptyTitle;
            List<String> lore;
            if (searching) {
                emptyTitle = ChatColor.RED + "No Matches";
                List<String> tempLore = new ArrayList<>();
                tempLore.add(ChatColor.GRAY + "No "
                        + (view == BrowserView.LISTINGS ? "listings" : "orders")
                        + " matched \"" + ChatColor.AQUA + formatSearchQueryForLore(searchQuery) + ChatColor.GRAY + "\".");
                tempLore.add(ChatColor.YELLOW + "Right-click the search button to clear.");
                lore = tempLore;
            } else {
                emptyTitle = view == BrowserView.LISTINGS ? ChatColor.RED + "No Listings" : ChatColor.RED + "No Orders";
                lore = view == BrowserView.LISTINGS
                        ? List.of(ChatColor.GRAY + "No items are currently listed.")
                        : List.of(ChatColor.GRAY + "No buy orders are currently active.");
            }
            int emptySlot = browserConfig.emptyListingSlot();
            if (emptySlot >= 0 && emptySlot < inventory.getSize()) {
                inventory.setItem(emptySlot, createButton(Material.BARRIER, emptyTitle, lore));
            }
        } else {
            int slot = 0;
            for (int index = startIndex; index < endIndex; index++) {
                if (view == BrowserView.LISTINGS) {
                    AuctionListing listing = listings.get(index);
                    ItemStack icon = decorateListing(listing, playerId);
                    if (icon == null) {
                        continue;
                    }
                    setPersistent(icon, actionKey, ACTION_LISTING);
                    setPersistent(icon, actionTypeKey, BrowserView.LISTINGS.name());
                    setPersistent(icon, listingKey, listing.id());
                    if (slot < inventory.getSize()) {
                        inventory.setItem(slot, icon);
                    }
                    slot++;
                } else {
                    AuctionOrder order = orders.get(index);
                    ItemStack icon = decorateOrder(order, playerId);
                    if (icon == null) {
                        continue;
                    }
                    setPersistent(icon, actionKey, ACTION_ORDER);
                    setPersistent(icon, actionTypeKey, BrowserView.ORDERS.name());
                    setPersistent(icon, listingKey, order.id());
                    if (slot < inventory.getSize()) {
                        inventory.setItem(slot, icon);
                    }
                    slot++;
                }
            }
        }

        if (currentPage > 0) {
            ItemStack previous = createButton(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                    List.of(ChatColor.GRAY + "View earlier listings."));
            setPersistent(previous, actionKey, ACTION_PREVIOUS);
            int slot = browserConfig.previousSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, previous);
            }
        }

        ItemStack close = createButton(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Exit the auction."));
        setPersistent(close, actionKey, ACTION_CLOSE);
        int closeSlot = browserConfig.closeSlot();
        if (closeSlot >= 0 && closeSlot < inventory.getSize()) {
            inventory.setItem(closeSlot, close);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = createButton(Material.ARROW, ChatColor.YELLOW + "Next Page",
                    List.of(ChatColor.GRAY + "View more listings."));
            setPersistent(next, actionKey, ACTION_NEXT);
            int slot = browserConfig.nextSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, next);
            }
        }

        ItemStack listingsToggle = createToggleButton(view == BrowserView.LISTINGS, listingsToggleConfig,
                ChatColor.YELLOW + "Currently viewing listings.", ChatColor.GREEN + "Click to view listings.");
        setPersistent(listingsToggle, actionKey, ACTION_TOGGLE_LISTINGS);
        int listingsToggleSlot = listingsToggleConfig != null ? listingsToggleConfig.slot() : 46;
        if (listingsToggleSlot >= 0 && listingsToggleSlot < inventory.getSize()) {
            inventory.setItem(listingsToggleSlot, listingsToggle);
        }

        ItemStack ordersToggle = createToggleButton(view == BrowserView.ORDERS, ordersToggleConfig,
                ChatColor.YELLOW + "Currently viewing orders.", ChatColor.GREEN + "Click to view orders.");
        setPersistent(ordersToggle, actionKey, ACTION_TOGGLE_ORDERS);
        int ordersToggleSlot = ordersToggleConfig != null ? ordersToggleConfig.slot() : 52;
        if (ordersToggleSlot >= 0 && ordersToggleSlot < inventory.getSize()) {
            inventory.setItem(ordersToggleSlot, ordersToggle);
        }

        if (searchButtonConfig != null) {
            ItemStack searchButton = createSearchButton(view, searchQuery);
            setPersistent(searchButton, actionKey, ACTION_SEARCH);
            int searchSlot = searchButtonConfig.slot();
            if (searchSlot >= 0 && searchSlot < inventory.getSize()) {
                inventory.setItem(searchSlot, searchButton);
            }
        }

        if (sortButtonConfig != null) {
            ItemStack sortButton = createSortButton(view, listingSort, orderSort);
            setPersistent(sortButton, actionKey, ACTION_SORT);
            int sortSlot = sortButtonConfig.slot();
            if (sortSlot >= 0 && sortSlot < inventory.getSize()) {
                inventory.setItem(sortSlot, sortButton);
            }
        }

        if (searchTipsButtonConfig != null) {
            ItemStack searchTipsButton = createSearchTipsButton();
            setPersistent(searchTipsButton, actionKey, ACTION_SEARCH_TIPS);
            int searchTipsSlot = searchTipsButtonConfig.slot();
            if (searchTipsSlot >= 0 && searchTipsSlot < inventory.getSize()) {
                inventory.setItem(searchTipsSlot, searchTipsButton);
            }
        }

        if (claimsButtonConfig != null) {
            ItemStack claimsButton = createClaimsButton(player);
            setPersistent(claimsButton, actionKey, ACTION_CLAIMS);
            int claimsSlot = claimsButtonConfig.slot();
            if (claimsSlot >= 0 && claimsSlot < inventory.getSize()) {
                inventory.setItem(claimsSlot, claimsButton);
            }
        }

        // Add activity menu button (consolidated view)
        ItemStack activityButton = createActivityButton();
        setPersistent(activityButton, actionKey, ACTION_ACTIVITY);
        int activitySlot = 44; // Bottom left corner before prev button
        if (activitySlot >= 0 && activitySlot < inventory.getSize()) {
            inventory.setItem(activitySlot, activityButton);
        }

        player.openInventory(inventory);
    }

    private void openListingConfirmMenu(Player player, BrowserMenuHolder previousHolder, String listingId) {
        AuctionListing listing = findListing(listingId);
        if (listing == null) {
            sendMessage(player, messages.listingUnavailable());
            runSync(() -> openBrowser(player, previousHolder.view(), previousHolder.page()));
            return;
        }

        ConfirmMenuHolder holder = new ConfirmMenuHolder(player.getUniqueId(), previousHolder.page(),
                previousHolder.view(), ConfirmAction.PURCHASE_LISTING);
        Inventory inventory = Bukkit.createInventory(holder, confirmConfig.size(), colorize(confirmConfig.title()));
        holder.setInventory(inventory);

        applyFiller(inventory, confirmFiller);

        ItemStack confirm = createConfiguredButton(
                confirmButtonConfig != null ? confirmButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Buy for " + ChatColor.GOLD + transactionService.formatCurrency(listing.price())));
        setPersistent(confirm, actionKey, ACTION_CONFIRM);
        setPersistent(confirm, listingKey, listing.id());
        setPersistent(confirm, actionTypeKey, BrowserView.LISTINGS.name());
        int confirmSlot = confirmButtonConfig != null
                ? confirmButtonConfig.slot()
                : confirmConfig.confirmButton().slot();
        if (confirmSlot >= 0 && confirmSlot < inventory.getSize()) {
            inventory.setItem(confirmSlot, confirm);
        }

        ItemStack icon = decorateListing(listing, player.getUniqueId());
        if (icon != null) {
            setPersistent(icon, actionKey, ACTION_LISTING);
            setPersistent(icon, listingKey, listing.id());
            setPersistent(icon, actionTypeKey, BrowserView.LISTINGS.name());
            int listingSlot = confirmConfig.listingSlot();
            if (listingSlot >= 0 && listingSlot < inventory.getSize()) {
                inventory.setItem(listingSlot, icon);
            }
        }

        ItemStack cancel = createConfiguredButton(
                cancelButtonConfig != null ? cancelButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Return to the listings."));
        setPersistent(cancel, actionKey, ACTION_CANCEL);
        int cancelSlot = cancelButtonConfig != null
                ? cancelButtonConfig.slot()
                : confirmConfig.cancelButton().slot();
        if (cancelSlot >= 0 && cancelSlot < inventory.getSize()) {
            inventory.setItem(cancelSlot, cancel);
        }

        player.openInventory(inventory);
    }

    private void openOrderConfirmMenu(Player player, BrowserMenuHolder previousHolder, String orderId) {
        AuctionOrder order = findOrder(orderId);
        if (order == null) {
            sendMessage(player, messages.orderUnavailable());
            runSync(() -> openBrowser(player, previousHolder.view(), previousHolder.page()));
            return;
        }

        ConfirmMenuHolder holder = new ConfirmMenuHolder(player.getUniqueId(), previousHolder.page(),
                previousHolder.view(), ConfirmAction.FULFILL_ORDER);
        Inventory inventory = Bukkit.createInventory(holder, confirmConfig.size(), colorize(confirmConfig.title()));
        holder.setInventory(inventory);

        applyFiller(inventory, confirmFiller);

        ItemStack confirm = createConfiguredButton(
                confirmButtonConfig != null ? confirmButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Receive " + ChatColor.GOLD
                        + transactionService.formatCurrency(order.offeredPrice())));
        setPersistent(confirm, actionKey, ACTION_CONFIRM);
        setPersistent(confirm, listingKey, order.id());
        setPersistent(confirm, actionTypeKey, BrowserView.ORDERS.name());
        int confirmSlot = confirmButtonConfig != null
                ? confirmButtonConfig.slot()
                : confirmConfig.confirmButton().slot();
        if (confirmSlot >= 0 && confirmSlot < inventory.getSize()) {
            inventory.setItem(confirmSlot, confirm);
        }

        ItemStack icon = decorateOrder(order, player.getUniqueId());
        if (icon != null) {
            setPersistent(icon, actionKey, ACTION_ORDER);
            setPersistent(icon, listingKey, order.id());
            setPersistent(icon, actionTypeKey, BrowserView.ORDERS.name());
            int listingSlot = confirmConfig.listingSlot();
            if (listingSlot >= 0 && listingSlot < inventory.getSize()) {
                inventory.setItem(listingSlot, icon);
            }
        }

        ItemStack cancel = createConfiguredButton(
                cancelButtonConfig != null ? cancelButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Return to the browser."));
        setPersistent(cancel, actionKey, ACTION_CANCEL);
        int cancelSlot = cancelButtonConfig != null
                ? cancelButtonConfig.slot()
                : confirmConfig.cancelButton().slot();
        if (cancelSlot >= 0 && cancelSlot < inventory.getSize()) {
            inventory.setItem(cancelSlot, cancel);
        }

        player.openInventory(inventory);
    }

    private void applyFiller(Inventory inventory, ItemStack filler) {
        if (inventory == null || filler == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    private void appendEstimatedValue(ItemStack item, List<String> lore) {
        if (!valueDisplayEnabled || item == null || lore == null) {
            return;
        }
        OptionalDouble estimate = itemValueProvider != null ? itemValueProvider.estimate(item) : OptionalDouble.empty();
        if (estimate.isEmpty()) {
            return;
        }
        String format = valueDisplayFormat != null && !valueDisplayFormat.isEmpty()
                ? valueDisplayFormat
                : "&7Value: &6{value}";
        String formatted = format.replace("{value}", transactionService.formatCurrency(estimate.getAsDouble()));
        lore.add(colorize(formatted));
    }

    private void appendShopPrice(ItemStack item, List<String> lore) {
        if (!shopPriceDisplayEnabled || item == null || lore == null) {
            return;
        }
        OptionalDouble shopPrice = shopPriceProvider != null
                ? shopPriceProvider.estimate(item)
                : OptionalDouble.empty();
        if (shopPrice.isEmpty()) {
            return;
        }
        String format = shopPriceFormat != null && !shopPriceFormat.isEmpty()
                ? shopPriceFormat
                : "&7Shop Price: &6{value}";
        String formatted = format.replace("{value}", transactionService.formatCurrency(shopPrice.getAsDouble()));
        lore.add(colorize(formatted));
    }

    private ItemStack decorateListing(AuctionListing listing, UUID viewer) {
        ItemStack display = listing.item();
        if (display == null || display.getType() == Material.AIR) {
            return null;
        }
        ItemStack icon = display.clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.isEmpty()) {
            lore.add(" ");
        }

        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.sellerId());
        String sellerName = seller != null && seller.getName() != null ? seller.getName() : "Unknown";
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.AQUA + sellerName
                + (listing.sellerId().equals(viewer) ? ChatColor.GRAY + " (You)" : ""));
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + transactionService.formatCurrency(listing.price()));
        appendEstimatedValue(display, lore);
        appendShopPrice(display, lore);
        lore.add(ChatColor.GRAY + "Expires: " + ChatColor.YELLOW + formatExpiry(listing.expiryEpochMillis()));
        lore.add(" ");
        if (isShulkerBox(display)) {
            lore.add(colorize(messages.shulkerPreviewHint()));
        }
        if (listing.sellerId().equals(viewer)) {
            lore.add(ChatColor.YELLOW + "Right-click to cancel this listing.");
        } else {
            lore.add(ChatColor.GREEN + "Left-click to buy this item.");
        }
        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack decorateOrder(AuctionOrder order, UUID viewer) {
        ItemStack requested = order.requestedItem();
        if (requested == null || requested.getType() == Material.AIR) {
            return null;
        }

        ItemStack icon = requested.clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        OfflinePlayer buyer = plugin.getServer().getOfflinePlayer(order.buyerId());
        String buyerName = buyer != null && buyer.getName() != null ? buyer.getName() : "Unknown";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Buyer: " + ChatColor.AQUA + buyerName
                + (order.buyerId().equals(viewer) ? ChatColor.GRAY + " (You)" : ""));
        lore.add(ChatColor.GRAY + "Offer: " + ChatColor.GOLD + transactionService.formatCurrency(order.offeredPrice()));
        appendEstimatedValue(requested, lore);
        appendShopPrice(requested, lore);
        lore.add(ChatColor.GRAY + "Expires: " + ChatColor.YELLOW + formatExpiry(order.expiryEpochMillis()));
        lore.add(" ");
        if (isShulkerBox(requested)) {
            lore.add(colorize(messages.shulkerPreviewHint()));
        }
        if (order.buyerId().equals(viewer)) {
            lore.add(ChatColor.YELLOW + "Right-click to cancel this order.");
        } else {
            lore.add(ChatColor.GREEN + "Left-click to fulfill this order.");
        }
        meta.setLore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private String formatExpiry(long expiryEpochMillis) {
        long now = System.currentTimeMillis();
        if (expiryEpochMillis <= now) {
            return "Expired";
        }
        Duration remaining = Duration.ofMillis(expiryEpochMillis - now);
        long days = remaining.toDays();
        remaining = remaining.minusDays(days);
        long hours = remaining.toHours();
        remaining = remaining.minusHours(hours);
        long minutes = remaining.toMinutes();

        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        builder.append(ChatColor.GRAY).append(" (")
                .append(DATE_FORMAT.format(Instant.ofEpochMilli(expiryEpochMillis).atZone(ZoneId.systemDefault())))
                .append(")");
        return builder.toString();
    }

    private AuctionListing findListing(String listingId) {
        if (listingId == null || listingId.isEmpty()) {
            return null;
        }
        for (AuctionListing listing : auctionManager.listActiveListings()) {
            if (listing.id().equalsIgnoreCase(listingId)) {
                return listing;
            }
        }
        return null;
    }

    private AuctionOrder findOrder(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }
        for (AuctionOrder order : auctionManager.listActiveOrders()) {
            if (order.id().equalsIgnoreCase(orderId)) {
                return order;
            }
        }
        return null;
    }

    private ItemStack createConfiguredFiller(AuctionMenuConfiguration.MenuButtonConfiguration configuration) {
        if (configuration == null) {
            return createButton(Material.GRAY_STAINED_GLASS_PANE, "&8 ", List.of());
        }
        return createButton(configuration.material(), configuration.displayName(), configuration.lore());
    }

    private ItemStack createConfiguredButton(AuctionMenuConfiguration.MenuButtonConfiguration configuration,
            List<String> additionalLore) {
        List<String> lore = new ArrayList<>();
        if (configuration != null && configuration.lore() != null) {
            lore.addAll(configuration.lore());
        }
        if (additionalLore != null && !additionalLore.isEmpty()) {
            lore.addAll(additionalLore);
        }
        if (configuration == null) {
            return createButton(Material.STONE_BUTTON, "&7Button", lore);
        }
        return createButton(configuration.material(), configuration.displayName(), lore);
    }

    private String formatBrowserTitle(Player player, BrowserView view, int page, int totalPages) {
        String rawTitle = browserConfig.title();
        if (rawTitle == null || rawTitle.isEmpty()) {
            rawTitle = "&2Auction House &7({page}/{total_pages})";
        }
        String formatted = rawTitle.replace("{page}", String.valueOf(page + 1))
                .replace("{total_pages}", String.valueOf(Math.max(1, totalPages)));
        if (formatted.contains("{view}")) {
            formatted = formatted.replace("{view}", view == BrowserView.ORDERS ? "Orders" : "Listings");
        }
        // Add search indicator if this player has an active search
        if (player != null) {
            String searchQuery = getSearchQuery(player.getUniqueId());
            if (searchQuery != null && !searchQuery.isEmpty()) {
                formatted = formatted + ChatColor.YELLOW + " [Searching]";
            }
        }
        return colorize(formatted);
    }

    private String colorize(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null) {
            return false;
        }
        Material type = item.getType();
        if (type == null) {
            return false;
        }
        String name = type.name();
        return name != null && name.endsWith("SHULKER_BOX");
    }

    private List<String> colorizeList(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return null;
        }
        List<String> colored = new ArrayList<>(lore.size());
        for (String line : lore) {
            colored.add(colorize(line));
        }
        return colored;
    }

    private void sendMessage(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(colorize(message));
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = material != null ? new ItemStack(material) : new ItemStack(Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            List<String> coloredLore = colorizeList(lore);
            if (coloredLore != null && !coloredLore.isEmpty()) {
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setPersistent(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null) {
            return;
        }
        itemTagStorage.setString(item, key, value);
    }

    private void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SearchPrompt prompt = pendingSearchInputs.get(playerId);
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        pendingSearchInputs.remove(playerId);
        plugin.getServer().getScheduler().runTask(plugin,
                () -> handleSearchInput(event.getPlayer(), prompt, event.getMessage()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingSearchInputs.remove(playerId);
        activeSearchQueries.remove(playerId);
        activeListingSorts.remove(playerId);
        activeOrderSorts.remove(playerId);
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
        runSync(() -> reopenPreviewOrigin(player, holder));
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
            handleBrowserClick(player, browserHolder, action, clicked, event);
        } else if (holder instanceof ConfirmMenuHolder confirmHolder) {
            if (ACTION_LISTING.equalsIgnoreCase(action) || ACTION_ORDER.equalsIgnoreCase(action)) {
                handlePreviewClick(player, confirmHolder, action, clicked, event);
            } else {
                handleConfirmClick(player, confirmHolder, action, clicked);
            }
        }
    }

    private void handleBrowserClick(Player player, BrowserMenuHolder holder, String action,
            ItemStack clicked, InventoryClickEvent event) {
        if (ACTION_PREVIOUS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowser(player, holder.view(), holder.page() - 1));
            return;
        }
        if (ACTION_NEXT.equalsIgnoreCase(action)) {
            runSync(() -> openBrowser(player, holder.view(), holder.page() + 1));
            return;
        }
        if (action == null) {
            return;
        }
        if (ACTION_CLOSE.equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        if (ACTION_TOGGLE_LISTINGS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowser(player, BrowserView.LISTINGS, 0));
            return;
        }
        if (ACTION_TOGGLE_ORDERS.equalsIgnoreCase(action)) {
            runSync(() -> openBrowser(player, BrowserView.ORDERS, 0));
            return;
        }
        if (ACTION_SEARCH.equalsIgnoreCase(action)) {
            handleSearchClick(player, holder, event);
            return;
        }
        if (ACTION_SORT.equalsIgnoreCase(action)) {
            handleSortClick(player, holder, event);
            return;
        }
        if (ACTION_SEARCH_TIPS.equalsIgnoreCase(action)) {
            return;
        }
        if (ACTION_CLAIMS.equalsIgnoreCase(action)) {
            player.closeInventory();
            player.performCommand("auction claim");
            return;
        }
        if (ACTION_ACTIVITY.equalsIgnoreCase(action)) {
            player.closeInventory();
            if (activityMenu != null) {
                activityMenu.openActivityMenu(player, ActivityTab.MY_LISTINGS);
            } else {
                player.sendMessage(ChatColor.RED + "Activity menu is not available.");
            }
            return;
        }
        if (!ACTION_LISTING.equalsIgnoreCase(action)) {
            if (!ACTION_ORDER.equalsIgnoreCase(action)) {
                return;
            }
            handleOrderSelection(player, holder, clicked, event);
            return;
        }

        String listingId = itemTagStorage.getString(clicked, listingKey);
        if (listingId == null) {
            sendMessage(player, messages.listingNotFound());
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        AuctionListing listing = findListing(listingId);
        if (listing == null) {
            sendMessage(player, messages.listingUnavailable());
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (tryOpenShulkerPreview(player, listing.item(), holder.view(), holder.page(), null, null)) {
                return;
            }
        }

        if (event.isRightClick()) {
            if (!player.hasPermission("ezauction.auction.sell")) {
                sendMessage(player, messages.cancelListingNoPermission());
                return;
            }

            AuctionOperationResult result = auctionManager.cancelListing(player.getUniqueId(), listingId);
            if (result.message() != null && !result.message().isEmpty()) {
                player.sendMessage(result.message());
            }
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        if (!player.hasPermission("ezauction.auction.buy")) {
            sendMessage(player, messages.buyNoPermission());
            return;
        }

        if (listing.sellerId().equals(player.getUniqueId())) {
            sendMessage(player, messages.ownListingPurchase());
            return;
        }

        runSync(() -> openListingConfirmMenu(player, holder, listingId));
    }

    private void handleConfirmClick(Player player, ConfirmMenuHolder holder, String action, ItemStack clicked) {
        if (ACTION_CANCEL.equalsIgnoreCase(action)) {
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }
        if (!ACTION_CONFIRM.equalsIgnoreCase(action)) {
            return;
        }
        if (holder.action() == ConfirmAction.PURCHASE_LISTING) {
            handleListingConfirm(player, holder, clicked);
        } else if (holder.action() == ConfirmAction.FULFILL_ORDER) {
            handleOrderConfirm(player, holder, clicked);
        }
    }

    private void handlePreviewClick(Player player, ConfirmMenuHolder holder, String action,
            ItemStack clicked, InventoryClickEvent event) {
        if (!event.isShiftClick() || !event.isLeftClick()) {
            return;
        }
        String targetId = itemTagStorage.getString(clicked, listingKey);
        if (targetId == null) {
            return;
        }
        if (ACTION_LISTING.equalsIgnoreCase(action)) {
            AuctionListing listing = findListing(targetId);
            if (listing == null) {
                sendMessage(player, messages.listingUnavailable());
                runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
                return;
            }
            if (tryOpenShulkerPreview(player, listing.item(), holder.view(), holder.previousPage(), holder.action(), targetId)) {
                return;
            }
        } else if (ACTION_ORDER.equalsIgnoreCase(action)) {
            AuctionOrder order = findOrder(targetId);
            if (order == null) {
                sendMessage(player, messages.orderUnavailable());
                runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
                return;
            }
            if (tryOpenShulkerPreview(player, order.requestedItem(), holder.view(), holder.previousPage(), holder.action(),
                    targetId)) {
                return;
            }
        }
    }

    private void handleSearchClick(Player player, BrowserMenuHolder holder, InventoryClickEvent event) {
        UUID playerId = player.getUniqueId();
        if (event.isRightClick()) {
            if (activeSearchQueries.remove(playerId) != null) {
                sendMessage(player, messages.searchCleared());
            } else {
                sendMessage(player, messages.searchAlreadyClear());
            }
            runSync(() -> openBrowser(player, holder.view(), 0));
            return;
        }
        pendingSearchInputs.put(playerId, new SearchPrompt(holder.view(), holder.page()));
        player.closeInventory();
        sendMessage(player, messages.searchPrompt());
        sendMessage(player, messages.searchClearPrompt());
    }

    private void handleSortClick(Player player, BrowserMenuHolder holder, InventoryClickEvent event) {
        UUID playerId = player.getUniqueId();
        boolean backwards = event.isRightClick();
        if (holder.view() == BrowserView.LISTINGS) {
            ListingSort current = getListingSort(playerId);
            ListingSort updated = backwards ? current.previous() : current.next();
            activeListingSorts.put(playerId, updated);
            sendMessage(player, messages.sortListings().replace("{sort}", updated.label()));
        } else {
            OrderSort current = getOrderSort(playerId);
            OrderSort updated = backwards ? current.previous() : current.next();
            activeOrderSorts.put(playerId, updated);
            sendMessage(player, messages.sortOrders().replace("{sort}", updated.label()));
        }
        runSync(() -> openBrowser(player, holder.view(), holder.page()));
    }

    private boolean tryOpenShulkerPreview(Player player, ItemStack item, BrowserView view, int page,
            ConfirmAction confirmAction, String targetId) {
        if (player == null || item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!isShulkerBox(item)) {
            return false;
        }
        ItemStack source = item.clone();
        runSync(() -> openShulkerPreview(player, source, view, page, confirmAction, targetId));
        return true;
    }

    private void openShulkerPreview(Player player, ItemStack shulker, BrowserView view, int page,
            ConfirmAction confirmAction, String targetId) {
        if (player == null || shulker == null) {
            return;
        }

        ItemStack[] contents = readShulkerContents(shulker);
        int size = contents.length > 0 ? contents.length : 27;

        int normalizedSize = Math.max(9, Math.min(54, ((size + 8) / 9) * 9));
        PreviewMenuHolder holder = new PreviewMenuHolder(player.getUniqueId(), view, page, confirmAction, targetId);
        String title = colorize(messages.shulkerPreviewTitle());
        if (title == null || title.isEmpty()) {
            title = ChatColor.DARK_PURPLE + "Shulker Contents";
        }
        Inventory preview = Bukkit.createInventory(holder, normalizedSize, title);
        holder.setInventory(preview);
        if (contents != null) {
            for (int i = 0; i < Math.min(contents.length, preview.getSize()); i++) {
                ItemStack content = contents[i];
                if (content != null && content.getType() != Material.AIR && content.getAmount() > 0) {
                    preview.setItem(i, content);
                }
            }
        }

        player.openInventory(preview);
    }

    private ItemStack[] readShulkerContents(ItemStack shulker) {
        if (shulker == null) {
            return new ItemStack[0];
        }
        ItemMeta meta = shulker.getItemMeta();
        if (meta == null) {
            return new ItemStack[0];
        }
        try {
            Class<?> blockStateMetaClass = Class.forName("org.bukkit.inventory.meta.BlockStateMeta");
            if (!blockStateMetaClass.isInstance(meta)) {
                return new ItemStack[0];
            }
            Method hasBlockState = blockStateMetaClass.getMethod("hasBlockState");
            if (!Boolean.TRUE.equals(hasBlockState.invoke(meta))) {
                return new ItemStack[0];
            }
            Method getBlockState = blockStateMetaClass.getMethod("getBlockState");
            Object blockState = getBlockState.invoke(meta);
            if (blockState == null) {
                return new ItemStack[0];
            }
            Class<?> shulkerClass = Class.forName("org.bukkit.block.ShulkerBox");
            if (!shulkerClass.isInstance(blockState)) {
                return new ItemStack[0];
            }
            Method getInventory = shulkerClass.getMethod("getInventory");
            Object inventory = getInventory.invoke(blockState);
            if (!(inventory instanceof Inventory snapshot)) {
                return new ItemStack[0];
            }
            int size = snapshot.getSize();
            ItemStack[] contents = new ItemStack[size];
            ItemStack[] original = snapshot.getContents();
            for (int i = 0; i < size; i++) {
                ItemStack content = original != null && i < original.length ? original[i] : null;
                if (content == null || content.getType() == Material.AIR || content.getAmount() <= 0) {
                    contents[i] = null;
                } else {
                    contents[i] = content.clone();
                }
            }
            return contents;
        } catch (ReflectiveOperationException ex) {
            return new ItemStack[0];
        }
    }

    private void reopenPreviewOrigin(Player player, PreviewMenuHolder holder) {
        if (player == null || holder == null) {
            return;
        }
        if (holder.confirmAction() == null || holder.targetId() == null) {
            openBrowser(player, holder.originView(), holder.originPage());
            return;
        }
        BrowserMenuHolder previousHolder = new BrowserMenuHolder(player.getUniqueId(), holder.originPage(),
                holder.originView());
        if (holder.confirmAction() == ConfirmAction.PURCHASE_LISTING) {
            openListingConfirmMenu(player, previousHolder, holder.targetId());
        } else if (holder.confirmAction() == ConfirmAction.FULFILL_ORDER) {
            openOrderConfirmMenu(player, previousHolder, holder.targetId());
        } else {
            openBrowser(player, holder.originView(), holder.originPage());
        }
    }

    private ListingSort getListingSort(UUID playerId) {
        return activeListingSorts.computeIfAbsent(playerId, id -> ListingSort.ENDING_SOON);
    }

    private OrderSort getOrderSort(UUID playerId) {
        return activeOrderSorts.computeIfAbsent(playerId, id -> OrderSort.ENDING_SOON);
    }

    private void handleOrderSelection(Player player, BrowserMenuHolder holder, ItemStack clicked,
            InventoryClickEvent event) {
        String orderId = itemTagStorage.getString(clicked, listingKey);
        if (orderId == null) {
            sendMessage(player, messages.orderNotFound());
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        AuctionOrder order = findOrder(orderId);
        if (order == null) {
            sendMessage(player, messages.orderUnavailable());
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (tryOpenShulkerPreview(player, order.requestedItem(), holder.view(), holder.page(), null, null)) {
                return;
            }
        }

        if (event.isRightClick()) {
            if (!order.buyerId().equals(player.getUniqueId())) {
                return;
            }
            AuctionOperationResult result = auctionManager.cancelOrder(player.getUniqueId(), orderId);
            if (result.message() != null && !result.message().isEmpty()) {
                player.sendMessage(result.message());
            }
            runSync(() -> openBrowser(player, holder.view(), holder.page()));
            return;
        }

        if (!player.hasPermission("ezauction.auction.fulfill")) {
            sendMessage(player, messages.fulfillNoPermission());
            return;
        }

        if (order.buyerId().equals(player.getUniqueId())) {
            sendMessage(player, messages.fulfillOwnOrder());
            return;
        }

        if (!player.getInventory().containsAtLeast(order.requestedItem(), order.requestedItem().getAmount())) {
            sendMessage(player, messages.fulfillInsufficientItems());
            return;
        }

        runSync(() -> openOrderConfirmMenu(player, holder, orderId));
    }

    private void handleListingConfirm(Player player, ConfirmMenuHolder holder, ItemStack clicked) {
        if (!player.hasPermission("ezauction.auction.buy")) {
            sendMessage(player, messages.buyNoPermission());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }
        String listingId = itemTagStorage.getString(clicked, listingKey);
        if (listingId == null) {
            sendMessage(player, messages.listingNotFound());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOperationResult result = auctionManager.purchaseListing(player, listingId);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }

        runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
    }

    private void handleOrderConfirm(Player player, ConfirmMenuHolder holder, ItemStack clicked) {
        if (!player.hasPermission("ezauction.auction.fulfill")) {
            sendMessage(player, messages.fulfillNoPermission());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }
        String orderId = itemTagStorage.getString(clicked, listingKey);
        if (orderId == null) {
            sendMessage(player, messages.orderNotFound());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOrder order = findOrder(orderId);
        if (order == null) {
            sendMessage(player, messages.orderUnavailable());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }

        if (!player.getInventory().containsAtLeast(order.requestedItem(), order.requestedItem().getAmount())) {
            sendMessage(player, messages.fulfillInsufficientItems());
            runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
            return;
        }

        AuctionOperationResult result = auctionManager.fulfillOrder(player, orderId);
        if (result.message() != null && !result.message().isEmpty()) {
            player.sendMessage(result.message());
        }

        runSync(() -> openBrowser(player, holder.view(), holder.previousPage()));
    }

    private void handleSearchInput(Player player, SearchPrompt prompt, String message) {
        if (player == null || prompt == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        String input = message != null ? message.trim() : "";
        if (input.equalsIgnoreCase("cancel")) {
            sendMessage(player, messages.searchCancelled());
            openBrowser(player, prompt.view(), prompt.page());
            return;
        }
        if (input.equalsIgnoreCase("clear") || input.isEmpty()) {
            boolean removed = activeSearchQueries.remove(playerId) != null;
            if (removed) {
                sendMessage(player, messages.searchCleared());
            } else {
                sendMessage(player, messages.searchAlreadyClear());
            }
            openBrowser(player, prompt.view(), 0);
            return;
        }
        String sanitized = sanitizeSearchInput(input);
        if (sanitized == null) {
            sendMessage(player, messages.searchInvalid());
            openBrowser(player, prompt.view(), prompt.page());
            return;
        }
        activeSearchQueries.put(playerId, sanitized);
        sendMessage(player, messages.searchApplied().replace("{query}", sanitized));
        openBrowser(player, prompt.view(), 0);
    }

    private String sanitizeSearchInput(String input) {
        if (input == null) {
            return null;
        }
        String stripped = ChatColor.stripColor(input);
        if (stripped == null) {
            stripped = input;
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            trimmed = trimmed.substring(0, MAX_SEARCH_LENGTH);
        }
        return trimmed;
    }

    private String getSearchQuery(UUID playerId) {
        String query = activeSearchQueries.get(playerId);
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            activeSearchQueries.remove(playerId);
            return null;
        }
        return trimmed;
    }

    private void filterListings(List<AuctionListing> listings, String normalizedQuery) {
        if (listings == null || listings.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return;
        }
        listings.removeIf(listing -> listing == null || !itemMatchesQuery(listing.item(), normalizedQuery));
    }

    private void filterOrders(List<AuctionOrder> orders, String normalizedQuery) {
        if (orders == null || orders.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return;
        }
        orders.removeIf(order -> order == null || !itemMatchesQuery(order.requestedItem(), normalizedQuery));
    }

    private boolean itemMatchesQuery(ItemStack item, String normalizedQuery) {
        if (item == null || item.getType() == Material.AIR || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = meta.hasDisplayName() ? normalizeText(meta.getDisplayName()) : null;
            if (displayName != null && displayName.contains(normalizedQuery)) {
                return true;
            }
            String localized = normalizeText(resolveLocalizedName(meta));
            if (localized != null && localized.contains(normalizedQuery)) {
                return true;
            }
            if (enchantmentsMatch(meta, normalizedQuery)) {
                return true;
            }
        }
        if (!item.getEnchantments().isEmpty() && enchantmentMatches(item.getEnchantments(), normalizedQuery)) {
            return true;
        }
        String materialName = normalizeMaterialName(item.getType());
        return materialName.contains(normalizedQuery);
    }

    private boolean enchantmentsMatch(ItemMeta meta, String normalizedQuery) {
        if (meta == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        if (meta.hasEnchants() && enchantmentMatches(meta.getEnchants(), normalizedQuery)) {
            return true;
        }
        if (meta instanceof EnchantmentStorageMeta storage
                && storage.hasStoredEnchants()
                && enchantmentMatches(storage.getStoredEnchants(), normalizedQuery)) {
            return true;
        }
        return false;
    }

    private boolean enchantmentMatches(Map<Enchantment, Integer> enchantments, String normalizedQuery) {
        if (enchantments == null || enchantments.isEmpty() || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            if (enchantmentMatches(entry.getKey(), entry.getValue(), normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean enchantmentMatches(Enchantment enchantment, int level, String normalizedQuery) {
        if (enchantment == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        String keyName = resolveEnchantmentKey(enchantment);
        if (matchesEnchantmentNameVariant(keyName, level, normalizedQuery)) {
            return true;
        }
        if (matchesEnchantmentNameVariant(keyName != null ? keyName.replace('_', ' ') : null, level, normalizedQuery)) {
            return true;
        }
        return false;
    }

    private String resolveEnchantmentKey(Enchantment enchantment) {
        if (enchantment == null) {
            return null;
        }
        try {
            Method getKey = Enchantment.class.getMethod("getKey");
            Object key = getKey.invoke(enchantment);
            return key != null ? key.toString() : null;
        } catch (NoSuchMethodException ex) {
            // Fallback to legacy getName
        } catch (Exception ex) {
            return null;
        }
        try {
            Method getName = Enchantment.class.getMethod("getName");
            Object name = getName.invoke(enchantment);
            return name != null ? name.toString() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String resolveLocalizedName(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        try {
            Method hasLocalizedName = meta.getClass().getMethod("hasLocalizedName");
            Object has = hasLocalizedName.invoke(meta);
            if (!(has instanceof Boolean) || !(Boolean) has) {
                return null;
            }
            Method getLocalizedName = meta.getClass().getMethod("getLocalizedName");
            Object name = getLocalizedName.invoke(meta);
            return name != null ? name.toString() : null;
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean matchesEnchantmentNameVariant(String name, int level, String normalizedQuery) {
        if (name == null || normalizedQuery == null || normalizedQuery.isEmpty()) {
            return false;
        }
        String normalizedName = name.toLowerCase(Locale.ENGLISH).trim();
        if (normalizedName.isEmpty()) {
            return false;
        }
        if (normalizedName.contains(normalizedQuery)) {
            return true;
        }
        if (level > 0) {
            String numeric = normalizedName + " " + level;
            if (numeric.contains(normalizedQuery)) {
                return true;
            }
            String roman = toRomanNumeral(level);
            if (!roman.isEmpty()) {
                String romanTerm = normalizedName + " " + roman.toLowerCase(Locale.ENGLISH);
                if (romanTerm.contains(normalizedQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String toRomanNumeral(int number) {
        if (number <= 0) {
            return "";
        }
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int remaining = number;
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.length && remaining > 0; index++) {
            while (remaining >= values[index]) {
                builder.append(numerals[index]);
                remaining -= values[index];
            }
        }
        return builder.toString();
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String stripped = ChatColor.stripColor(value);
        if (stripped == null) {
            stripped = value;
        }
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ENGLISH);
    }

    private static String normalizeMaterialName(Material material) {
        if (material == null) {
            return "";
        }
        return material.name().replace('_', ' ').toLowerCase(Locale.ENGLISH);
    }

    private static String listingItemSortKey(AuctionListing listing) {
        return listing == null ? "" : itemSortKey(listing.item());
    }

    private static String orderItemSortKey(AuctionOrder order) {
        return order == null ? "" : itemSortKey(order.requestedItem());
    }

    private static int listingQuantity(AuctionListing listing) {
        if (listing == null) {
            return 0;
        }
        ItemStack item = listing.item();
        return item != null ? item.getAmount() : 0;
    }

    private static int orderQuantity(AuctionOrder order) {
        if (order == null) {
            return 0;
        }
        ItemStack item = order.requestedItem();
        return item != null ? item.getAmount() : 0;
    }

    private static String itemSortKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String normalized = normalizeText(meta.getDisplayName());
                if (normalized != null) {
                    return normalized;
                }
            }
            String localizedName = resolveLocalizedName(meta);
            if (localizedName != null) {
                String normalized = normalizeText(localizedName);
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return normalizeMaterialName(item.getType());
    }

    private String formatSearchQueryForLore(String query) {
        if (query == null) {
            return "";
        }
        if (query.length() <= 32) {
            return query;
        }
        return query.substring(0, 29) + "...";
    }

    private ItemStack createSearchButton(BrowserView view, String activeQuery) {
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = searchButtonConfig != null
                ? searchButtonConfig.button()
                : null;
        Material material = configuration != null ? configuration.material() : Material.COMPASS;
        String displayName = configuration != null ? configuration.displayName() : "&bSearch";
        List<String> lore = new ArrayList<>();
        if (configuration != null && configuration.lore() != null) {
            lore.addAll(configuration.lore());
        }
        String target = view == BrowserView.ORDERS ? "orders" : "listings";
        if (activeQuery != null && !activeQuery.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add(" ");
            }
            lore.add(ChatColor.GRAY + "Searching: " + ChatColor.AQUA + formatSearchQueryForLore(activeQuery));
            lore.add(ChatColor.YELLOW + "Left-click to update search.");
            lore.add(ChatColor.RED + "Right-click to clear search.");
        } else {
            if (!lore.isEmpty()) {
                lore.add(" ");
            }
            lore.add(ChatColor.YELLOW + "Left-click to search " + target + ".");
        }
        return createButton(material, displayName, lore);
    }

    private static final class SearchPrompt {

        private final BrowserView view;
        private final int page;

        private SearchPrompt(BrowserView view, int page) {
            this.view = view;
            this.page = page;
        }

        public BrowserView view() {
            return view;
        }

        public int page() {
            return page;
        }
    }

    private abstract static class AbstractAuctionHolder implements InventoryHolder {

        private final UUID owner;
        private Inventory inventory;

        protected AbstractAuctionHolder(UUID owner) {
            this.owner = owner;
        }

        public UUID owner() {
            return owner;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class BrowserMenuHolder extends AbstractAuctionHolder {

        private final int page;
        private final BrowserView view;

        private BrowserMenuHolder(UUID owner, int page, BrowserView view) {
            super(owner);
            this.page = page;
            this.view = view;
        }

        public int page() {
            return page;
        }

        public BrowserView view() {
            return view;
        }
    }

    private static final class ConfirmMenuHolder extends AbstractAuctionHolder {

        private final int previousPage;
        private final BrowserView view;
        private final ConfirmAction action;

        private ConfirmMenuHolder(UUID owner, int previousPage, BrowserView view, ConfirmAction action) {
            super(owner);
            this.previousPage = previousPage;
            this.view = view;
            this.action = action;
        }

        public int previousPage() {
            return previousPage;
        }

        public BrowserView view() {
            return view;
        }

        public ConfirmAction action() {
            return action;
        }
    }

    private static final class PreviewMenuHolder extends AbstractAuctionHolder {

        private final BrowserView originView;
        private final int originPage;
        private final ConfirmAction confirmAction;
        private final String targetId;

        private PreviewMenuHolder(UUID owner, BrowserView originView, int originPage,
                ConfirmAction confirmAction, String targetId) {
            super(owner);
            this.originView = originView;
            this.originPage = originPage;
            this.confirmAction = confirmAction;
            this.targetId = targetId;
        }

        public BrowserView originView() {
            return originView;
        }

        public int originPage() {
            return originPage;
        }

        public ConfirmAction confirmAction() {
            return confirmAction;
        }

        public String targetId() {
            return targetId;
        }
    }

    private enum BrowserView {
        LISTINGS,
        ORDERS
    }

    private enum ConfirmAction {
        PURCHASE_LISTING,
        FULFILL_ORDER
    }

    private enum ListingSort {
        ENDING_SOON("Ending Soon", Comparator.comparingLong(AuctionListing::expiryEpochMillis)
                .thenComparingDouble(AuctionListing::price)
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        NEWLY_LISTED("Newly Listed", Comparator.comparingLong(AuctionListing::expiryEpochMillis)
                .reversed()
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionListing::price)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        PRICE_LOW_HIGH("Lowest Price", Comparator.comparingDouble(AuctionListing::price)
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(AuctionListing::expiryEpochMillis)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        PRICE_HIGH_LOW("Highest Price", Comparator.comparingDouble(AuctionListing::price)
                .reversed()
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(AuctionListing::expiryEpochMillis)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        QUANTITY_HIGH_LOW("Quantity (High-Low)", Comparator.comparingInt(AuctionMenu::listingQuantity)
                .reversed()
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionListing::price)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        QUANTITY_LOW_HIGH("Quantity (Low-High)", Comparator.comparingInt(AuctionMenu::listingQuantity)
                .thenComparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionListing::price)
                .thenComparingLong(AuctionListing::expiryEpochMillis)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        ITEM_A_Z("Item Name (A-Z)", Comparator.comparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionListing::price)
                .thenComparingLong(AuctionListing::expiryEpochMillis)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
        ITEM_Z_A("Item Name (Z-A)", Comparator.comparing(AuctionMenu::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .reversed()
                .thenComparingDouble(AuctionListing::price)
                .thenComparingLong(AuctionListing::expiryEpochMillis)
                .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER));

        private static final ListingSort[] VALUES = values();

        private final String label;
        private final Comparator<AuctionListing> comparator;

        ListingSort(String label, Comparator<AuctionListing> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        public String label() {
            return label;
        }

        public void sort(List<AuctionListing> listings) {
            if (listings == null || listings.size() <= 1) {
                return;
            }
            listings.sort(comparator);
        }

        public ListingSort next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public ListingSort previous() {
            return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
        }
    }

    private enum OrderSort {
        ENDING_SOON("Ending Soon", Comparator.comparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        NEWLY_POSTED("Newly Posted", Comparator.comparingLong(AuctionOrder::expiryEpochMillis)
                .reversed()
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        PRICE_HIGH_LOW("Highest Offer", Comparator.comparingDouble(AuctionOrder::offeredPrice)
                .reversed()
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        PRICE_LOW_HIGH("Lowest Offer", Comparator.comparingDouble(AuctionOrder::offeredPrice)
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        QUANTITY_HIGH_LOW("Quantity (High-Low)", Comparator.comparingInt(AuctionMenu::orderQuantity)
                .reversed()
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        QUANTITY_LOW_HIGH("Quantity (Low-High)", Comparator.comparingInt(AuctionMenu::orderQuantity)
                .thenComparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        ITEM_A_Z("Item Name (A-Z)", Comparator.comparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
        ITEM_Z_A("Item Name (Z-A)", Comparator.comparing(AuctionMenu::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
                .reversed()
                .thenComparingDouble(AuctionOrder::offeredPrice)
                .thenComparingLong(AuctionOrder::expiryEpochMillis)
                .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER));

        private static final OrderSort[] VALUES = values();

        private final String label;
        private final Comparator<AuctionOrder> comparator;

        OrderSort(String label, Comparator<AuctionOrder> comparator) {
            this.label = label;
            this.comparator = comparator;
        }

        public String label() {
            return label;
        }

        public void sort(List<AuctionOrder> orders) {
            if (orders == null || orders.size() <= 1) {
                return;
            }
            orders.sort(comparator);
        }

        public OrderSort next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public OrderSort previous() {
            return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
        }
    }

    private ItemStack createSortButton(BrowserView view, ListingSort listingSort, OrderSort orderSort) {
        AuctionMenuConfiguration.MenuButtonConfiguration buttonConfiguration = sortButtonConfig != null
                ? sortButtonConfig.button()
                : null;
        List<String> lore = new ArrayList<>();
        String label = view == BrowserView.ORDERS ? orderSort.label() : listingSort.label();
        lore.add("&7Current: &e" + label);
        lore.add(" ");
        lore.add("&7Sort Options:");
        if (view == BrowserView.ORDERS) {
            for (OrderSort sort : OrderSort.values()) {
                boolean current = sort == orderSort;
                String color = current ? "&a" : "&7";
                String prefix = current ? "&a➤ " : "&7• ";
                lore.add(prefix + color + sort.label());
            }
        } else {
            for (ListingSort sort : ListingSort.values()) {
                boolean current = sort == listingSort;
                String color = current ? "&a" : "&7";
                String prefix = current ? "&a➤ " : "&7• ";
                lore.add(prefix + color + sort.label());
            }
        }
        lore.add(" ");
        lore.add("&aLeft-click to cycle forward.");
        lore.add("&cRight-click to cycle backward.");
        ItemStack button = createConfiguredButton(buttonConfiguration, lore);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            String baseName = buttonConfiguration != null && buttonConfiguration.displayName() != null
                    ? buttonConfiguration.displayName()
                    : "&bSort";
            String coloredSort = "&e" + label + "&7";
            String displayName;
            if (baseName.contains("{sort}")) {
                displayName = baseName.replace("{sort}", coloredSort);
            } else {
                displayName = baseName + " &7(" + coloredSort + ")";
            }
            meta.setDisplayName(colorize(displayName));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createToggleButton(boolean active,
            AuctionMenuConfiguration.ToggleButtonConfiguration configuration, String activeStatusLine,
            String inactiveStatusLine) {
        AuctionMenuConfiguration.MenuButtonConfiguration buttonConfiguration = configuration != null
                ? configuration.button()
                : null;
        Material material = buttonConfiguration != null ? buttonConfiguration.material() : Material.STONE_BUTTON;
        String displayName = buttonConfiguration != null ? buttonConfiguration.displayName() : "&aToggle";
        List<String> baseLore = buttonConfiguration != null ? buttonConfiguration.lore() : List.of();
        List<String> lore = new ArrayList<>(baseLore);
        if (active) {
            if (activeStatusLine != null && !activeStatusLine.isEmpty()) {
                lore.add(activeStatusLine);
            }
        } else if (inactiveStatusLine != null && !inactiveStatusLine.isEmpty()) {
            lore.add(inactiveStatusLine);
        }
        ItemStack button = createButton(material, displayName, lore);
        if (!active) {
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                String coloredName = colorize(displayName);
                meta.setDisplayName(ChatColor.GRAY + ChatColor.stripColor(coloredName));
                button.setItemMeta(meta);
            }
        }
        return button;
    }

    private ItemStack createSearchTipsButton() {
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = searchTipsButtonConfig != null
                ? searchTipsButtonConfig.button()
                : null;
        Material material = configuration != null ? configuration.material() : Material.KNOWLEDGE_BOOK;
        String displayName = configuration != null ? configuration.displayName() : "&eSearch Tips";
        List<String> lore = configuration != null && configuration.lore() != null
                ? new ArrayList<>(configuration.lore())
                : new ArrayList<>();
        return createButton(material, displayName, lore);
    }

    private ItemStack createClaimsButton(Player player) {
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = claimsButtonConfig != null
                ? claimsButtonConfig.button()
                : null;
        Material material = configuration != null ? configuration.material() : Material.ENDER_CHEST;
        String displayName = configuration != null ? configuration.displayName() : "&6Pending Returns";
        List<String> lore = configuration != null && configuration.lore() != null
                ? new ArrayList<>(configuration.lore())
                : new ArrayList<>();
        int claimCount = auctionManager.countPendingReturnItems(player.getUniqueId());
        if (claimCount > 0) {
            lore.add(ChatColor.GRAY + "Pending items: " + ChatColor.YELLOW + claimCount);
        }
        return createButton(material, displayName, lore);
    }

    private ItemStack createActivityButton() {
        Material material = Material.NETHER_STAR;
        String displayName = "&d&lMy Activity";
        List<String> lore = List.of(
                ChatColor.GRAY + "View all your auction activities:",
                ChatColor.YELLOW + "• Active Listings",
                ChatColor.YELLOW + "• Buy Orders", 
                ChatColor.YELLOW + "• Pending Returns",
                ChatColor.YELLOW + "• Recent History",
                "",
                ChatColor.GREEN + "Click to open!"
        );
        return createButton(material, displayName, lore);
    }
}
