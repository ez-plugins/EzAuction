package com.skyblockexp.ezauction.gui.menu.browser;

import com.skyblockexp.ezauction.config.AuctionMenuConfiguration;
import com.skyblockexp.ezauction.gui.holder.BrowserMenuHolder;
import com.skyblockexp.ezauction.gui.holder.ConfirmMenuHolder;
import com.skyblockexp.ezauction.gui.util.AuctionMenuUtils;
import com.skyblockexp.ezauction.gui.view.BrowserView;
import com.skyblockexp.ezauction.gui.view.ConfirmAction;
import com.skyblockexp.ezauction.gui.view.ListingSort;
import com.skyblockexp.ezauction.gui.view.OrderSort;
import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.model.AuctionOrder;
import com.skyblockexp.ezauction.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Builds browser and confirm-menu inventories and creates all button/decorator items.
 */
public final class BrowserMenuBuilder {

    private final BrowserMenuContext ctx;
    private final BrowserSearchHandler searchHandler;

    public BrowserMenuBuilder(BrowserMenuContext ctx, BrowserSearchHandler searchHandler) {
        this.ctx = ctx;
        this.searchHandler = searchHandler;
    }

    // -------------------------------------------------------------------------
    // Inventory builders
    // -------------------------------------------------------------------------

    /**
     * Builds the browser inventory for the given player, view and page.
     * The caller is responsible for calling {@code player.openInventory(result)}.
     */
    public Inventory buildBrowserInventory(Player player, BrowserView view, int page) {
        UUID playerId = player.getUniqueId();
        String searchQuery = searchHandler.getSearchQuery(playerId);
        String normalizedQuery = searchQuery != null ? searchQuery.toLowerCase(Locale.ENGLISH) : null;

        List<AuctionListing> listings;
        if (view == BrowserView.LISTINGS) {
            listings = new ArrayList<>(ctx.auctionManager.listActiveListings());
        } else if (view == BrowserView.TEAM_LISTINGS) {
            listings = new ArrayList<>(ctx.auctionManager.listActiveTeamListings(player.getUniqueId()));
        } else {
            listings = Collections.emptyList();
        }
        if (view == BrowserView.LISTINGS && ctx.auctionManager != null
                && ctx.auctionManager.getConfiguration() != null
                && ctx.auctionManager.getConfiguration().debug()) {
            System.out.println("[EzAuction][DEBUG] AuctionMenu.openBrowser: listings.size() = "
                    + listings.size() + ", listings = " + listings);
        }
        if (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS) {
            searchHandler.filterListings(listings, normalizedQuery);
        }
        List<AuctionOrder> orders = view == BrowserView.ORDERS
                ? new ArrayList<>(ctx.auctionManager.listActiveOrders())
                : Collections.emptyList();
        if (view == BrowserView.ORDERS) {
            searchHandler.filterOrders(orders, normalizedQuery);
        }

        ListingSort listingSort = searchHandler.getListingSort(playerId);
        OrderSort orderSort = searchHandler.getOrderSort(playerId);
        if (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS) {
            listingSort.sort(listings);
        }
        if (view == BrowserView.ORDERS) {
            orderSort.sort(orders);
        }

        int totalEntries = (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS)
                ? listings.size() : orders.size();
        int entriesPerPage = Math.max(1, ctx.listingsPerPage);
        int totalPages = Math.max(1, (int) Math.ceil(totalEntries / (double) entriesPerPage));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        BrowserMenuHolder holder = new BrowserMenuHolder(playerId, currentPage, view);
        String title = formatBrowserTitle(player, view, currentPage, totalPages);
        Inventory inventory = Bukkit.createInventory(holder, ctx.browserConfig.size(), title);
        holder.setInventory(inventory);

        applyFiller(inventory, ctx.browserFiller);

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
                        + ((view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS) ? "listings" : "orders")
                        + " matched \"" + ChatColor.AQUA + searchHandler.formatSearchQueryForLore(searchQuery)
                        + ChatColor.GRAY + "\".");
                tempLore.add(ChatColor.YELLOW + "Right-click the search button to clear.");
                lore = tempLore;
            } else {
                emptyTitle = (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS)
                        ? ChatColor.RED + "No Listings" : ChatColor.RED + "No Orders";
                lore = (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS)
                        ? List.of(ChatColor.GRAY + "No items are currently listed.")
                        : List.of(ChatColor.GRAY + "No buy orders are currently active.");
            }
            int emptySlot = ctx.browserConfig.emptyListingSlot();
            if (emptySlot >= 0 && emptySlot < inventory.getSize()) {
                inventory.setItem(emptySlot, createButton(Material.BARRIER, emptyTitle, lore));
            }
        } else {
            int slot = 0;
            for (int index = startIndex; index < endIndex; index++) {
                if (view == BrowserView.LISTINGS || view == BrowserView.TEAM_LISTINGS) {
                    AuctionListing listing = listings.get(index);
                    ItemStack icon = decorateListing(listing, playerId);
                    if (icon == null) {
                        continue;
                    }
                    setPersistent(icon, ctx.actionKey, BrowserActions.LISTING);
                    setPersistent(icon, ctx.actionTypeKey, view.name());
                    setPersistent(icon, ctx.listingKey, listing.id());
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
                    setPersistent(icon, ctx.actionKey, BrowserActions.ORDER);
                    setPersistent(icon, ctx.actionTypeKey, BrowserView.ORDERS.name());
                    setPersistent(icon, ctx.listingKey, order.id());
                    if (slot < inventory.getSize()) {
                        inventory.setItem(slot, icon);
                    }
                    slot++;
                }
            }
        }

        if (currentPage > 0) {
            ItemStack previous = createButton(Material.ARROW,
                    ChatColor.YELLOW + ctx.messages.previousPageTitle(),
                    List.of(ChatColor.GRAY + ctx.messages.previousPageLore()));
            setPersistent(previous, ctx.actionKey, BrowserActions.PREVIOUS);
            int slot = ctx.browserConfig.previousSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, previous);
            }
        }

        ItemStack close = createButton(Material.BARRIER,
                ChatColor.RED + ctx.messages.closeTitle(),
                List.of(ChatColor.GRAY + ctx.messages.closeLore()));
        setPersistent(close, ctx.actionKey, BrowserActions.CLOSE);
        int closeSlot = ctx.browserConfig.closeSlot();
        if (closeSlot >= 0 && closeSlot < inventory.getSize()) {
            inventory.setItem(closeSlot, close);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = createButton(Material.ARROW,
                    ChatColor.YELLOW + ctx.messages.nextPageTitle(),
                    List.of(ChatColor.GRAY + ctx.messages.nextPageLore()));
            setPersistent(next, ctx.actionKey, BrowserActions.NEXT);
            int slot = ctx.browserConfig.nextSlot();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, next);
            }
        }

        ItemStack listingsToggle = createToggleButton(view == BrowserView.LISTINGS, ctx.listingsToggleConfig,
                ChatColor.YELLOW + ctx.messages.listingsViewing(),
                ChatColor.GREEN + ctx.messages.listingsClick());
        setPersistent(listingsToggle, ctx.actionKey, BrowserActions.TOGGLE_LISTINGS);
        int listingsToggleSlot = ctx.listingsToggleConfig != null ? ctx.listingsToggleConfig.slot() : 46;
        if (listingsToggleSlot >= 0 && listingsToggleSlot < inventory.getSize()) {
            inventory.setItem(listingsToggleSlot, listingsToggle);
        }

        ItemStack ordersToggle = createToggleButton(view == BrowserView.ORDERS, ctx.ordersToggleConfig,
                ChatColor.YELLOW + ctx.messages.ordersViewing(),
                ChatColor.GREEN + ctx.messages.ordersClick());
        setPersistent(ordersToggle, ctx.actionKey, BrowserActions.TOGGLE_ORDERS);
        int ordersToggleSlot = ctx.ordersToggleConfig != null ? ctx.ordersToggleConfig.slot() : 52;
        if (ordersToggleSlot >= 0 && ordersToggleSlot < inventory.getSize()) {
            inventory.setItem(ordersToggleSlot, ordersToggle);
        }

        boolean teamAvailable = ctx.auctionManager.isTeamAuctionsAvailable()
                && player.hasPermission("ezauction.auction.team");
        if (teamAvailable && ctx.teamListingsToggleConfig != null) {
            ItemStack teamToggle = createToggleButton(view == BrowserView.TEAM_LISTINGS,
                    ctx.teamListingsToggleConfig,
                    ChatColor.AQUA + "Viewing Team Listings",
                    ChatColor.YELLOW + "Click to browse your team's listings.");
            setPersistent(teamToggle, ctx.actionKey, BrowserActions.TOGGLE_TEAM_LISTINGS);
            int teamToggleSlot = ctx.teamListingsToggleConfig.slot();
            if (teamToggleSlot >= 0 && teamToggleSlot < inventory.getSize()) {
                inventory.setItem(teamToggleSlot, teamToggle);
            }
        } else if (ctx.searchTipsButtonConfig != null) {
            ItemStack searchTipsButton = createSearchTipsButton();
            setPersistent(searchTipsButton, ctx.actionKey, BrowserActions.SEARCH_TIPS);
            int searchTipsSlot = ctx.searchTipsButtonConfig.slot();
            if (searchTipsSlot >= 0 && searchTipsSlot < inventory.getSize()) {
                inventory.setItem(searchTipsSlot, searchTipsButton);
            }
        }

        if (ctx.searchButtonConfig != null) {
            ItemStack searchButton = createSearchButton(view, searchQuery);
            setPersistent(searchButton, ctx.actionKey, BrowserActions.SEARCH);
            int searchSlot = ctx.searchButtonConfig.slot();
            if (searchSlot >= 0 && searchSlot < inventory.getSize()) {
                inventory.setItem(searchSlot, searchButton);
            }
        }

        if (ctx.sortButtonConfig != null) {
            ItemStack sortButton = createSortButton(view, listingSort, orderSort);
            setPersistent(sortButton, ctx.actionKey, BrowserActions.SORT);
            int sortSlot = ctx.sortButtonConfig.slot();
            if (sortSlot >= 0 && sortSlot < inventory.getSize()) {
                inventory.setItem(sortSlot, sortButton);
            }
        }

        if (ctx.claimsButtonConfig != null) {
            ItemStack claimsButton = createClaimsButton(player);
            setPersistent(claimsButton, ctx.actionKey, BrowserActions.CLAIMS);
            int claimsSlot = ctx.claimsButtonConfig.slot();
            if (claimsSlot >= 0 && claimsSlot < inventory.getSize()) {
                inventory.setItem(claimsSlot, claimsButton);
            }
        }

        ItemStack activityButton = createActivityButton();
        setPersistent(activityButton, ctx.actionKey, BrowserActions.ACTIVITY);
        int activitySlot = 44;
        if (activitySlot >= 0 && activitySlot < inventory.getSize()) {
            inventory.setItem(activitySlot, activityButton);
        }

        ItemStack liveAuctionButton = createButton(Material.BEACON,
                "&b&lLive Auctions",
                List.of("&7Watch and bid on live auctions.", "", "&e▶ Click to open!"));
        setPersistent(liveAuctionButton, ctx.actionKey, BrowserActions.LIVE_AUCTION);
        inventory.setItem(47, liveAuctionButton);

        return inventory;
    }

    /**
     * Builds the listing purchase confirm inventory.
     * Returns {@code null} if the listing no longer exists.
     */
    public Inventory buildListingConfirmInventory(Player player, BrowserMenuHolder previousHolder, String listingId) {
        AuctionListing listing = findListing(listingId);
        if (listing == null) {
            return null;
        }

        ConfirmMenuHolder holder = new ConfirmMenuHolder(player.getUniqueId(), previousHolder.page(),
                previousHolder.view(), ConfirmAction.PURCHASE_LISTING);
        Inventory inventory = Bukkit.createInventory(holder, ctx.confirmConfig.size(),
                colorize(ctx.confirmConfig.title()));
        holder.setInventory(inventory);

        applyFiller(inventory, ctx.confirmFiller);

        ItemStack confirm = createConfiguredButton(
                ctx.confirmButtonConfig != null ? ctx.confirmButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Buy for " + ChatColor.GOLD
                        + ctx.transactionService.formatCurrency(listing.price())));
        setPersistent(confirm, ctx.actionKey, BrowserActions.CONFIRM);
        setPersistent(confirm, ctx.listingKey, listing.id());
        setPersistent(confirm, ctx.actionTypeKey, BrowserView.LISTINGS.name());
        int confirmSlot = ctx.confirmButtonConfig != null
                ? ctx.confirmButtonConfig.slot()
                : ctx.confirmConfig.confirmButton().slot();
        if (confirmSlot >= 0 && confirmSlot < inventory.getSize()) {
            inventory.setItem(confirmSlot, confirm);
        }

        ItemStack icon = decorateListing(listing, player.getUniqueId());
        if (icon != null) {
            setPersistent(icon, ctx.actionKey, BrowserActions.LISTING);
            setPersistent(icon, ctx.listingKey, listing.id());
            setPersistent(icon, ctx.actionTypeKey, BrowserView.LISTINGS.name());
            int listingSlot = ctx.confirmConfig.listingSlot();
            if (listingSlot >= 0 && listingSlot < inventory.getSize()) {
                inventory.setItem(listingSlot, icon);
            }
        }

        ItemStack cancel = createConfiguredButton(
                ctx.cancelButtonConfig != null ? ctx.cancelButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Return to the listings."));
        setPersistent(cancel, ctx.actionKey, BrowserActions.CANCEL);
        int cancelSlot = ctx.cancelButtonConfig != null
                ? ctx.cancelButtonConfig.slot()
                : ctx.confirmConfig.cancelButton().slot();
        if (cancelSlot >= 0 && cancelSlot < inventory.getSize()) {
            inventory.setItem(cancelSlot, cancel);
        }

        return inventory;
    }

    /**
     * Builds the order fulfillment confirm inventory.
     * Returns {@code null} if the order no longer exists.
     */
    public Inventory buildOrderConfirmInventory(Player player, BrowserMenuHolder previousHolder, String orderId) {
        AuctionOrder order = findOrder(orderId);
        if (order == null) {
            return null;
        }

        ConfirmMenuHolder holder = new ConfirmMenuHolder(player.getUniqueId(), previousHolder.page(),
                previousHolder.view(), ConfirmAction.FULFILL_ORDER);
        Inventory inventory = Bukkit.createInventory(holder, ctx.confirmConfig.size(),
                colorize(ctx.confirmConfig.title()));
        holder.setInventory(inventory);

        applyFiller(inventory, ctx.confirmFiller);

        ItemStack confirm = createConfiguredButton(
                ctx.confirmButtonConfig != null ? ctx.confirmButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Receive " + ChatColor.GOLD
                        + ctx.transactionService.formatCurrency(order.offeredPrice())));
        setPersistent(confirm, ctx.actionKey, BrowserActions.CONFIRM);
        setPersistent(confirm, ctx.listingKey, order.id());
        setPersistent(confirm, ctx.actionTypeKey, BrowserView.ORDERS.name());
        int confirmSlot = ctx.confirmButtonConfig != null
                ? ctx.confirmButtonConfig.slot()
                : ctx.confirmConfig.confirmButton().slot();
        if (confirmSlot >= 0 && confirmSlot < inventory.getSize()) {
            inventory.setItem(confirmSlot, confirm);
        }

        ItemStack icon = decorateOrder(order, player.getUniqueId());
        if (icon != null) {
            setPersistent(icon, ctx.actionKey, BrowserActions.ORDER);
            setPersistent(icon, ctx.listingKey, order.id());
            setPersistent(icon, ctx.actionTypeKey, BrowserView.ORDERS.name());
            int listingSlot = ctx.confirmConfig.listingSlot();
            if (listingSlot >= 0 && listingSlot < inventory.getSize()) {
                inventory.setItem(listingSlot, icon);
            }
        }

        ItemStack cancel = createConfiguredButton(
                ctx.cancelButtonConfig != null ? ctx.cancelButtonConfig.button() : null,
                List.of(ChatColor.GRAY + "Return to the browser."));
        setPersistent(cancel, ctx.actionKey, BrowserActions.CANCEL);
        int cancelSlot = ctx.cancelButtonConfig != null
                ? ctx.cancelButtonConfig.slot()
                : ctx.confirmConfig.cancelButton().slot();
        if (cancelSlot >= 0 && cancelSlot < inventory.getSize()) {
            inventory.setItem(cancelSlot, cancel);
        }

        return inventory;
    }

    // -------------------------------------------------------------------------
    // Item decoration
    // -------------------------------------------------------------------------

    public ItemStack decorateListing(AuctionListing listing, UUID viewer) {
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

        OfflinePlayer seller = ctx.plugin.getServer().getOfflinePlayer(listing.sellerId());
        String sellerName = seller != null && seller.getName() != null ? seller.getName() : "Unknown";
        lore.add(ChatColor.GRAY + "Seller: " + ChatColor.AQUA + sellerName
                + (listing.sellerId().equals(viewer) ? ChatColor.GRAY + " (You)" : ""));
        lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD
                + ctx.transactionService.formatCurrency(listing.price()));
        appendEstimatedValue(display, lore);
        appendShopPrice(display, lore);
        lore.add(ChatColor.GRAY + "Expires: " + ChatColor.YELLOW
                + DateUtil.formatDate(listing.expiryEpochMillis()));
        if (listing.isTeamListing()) {
            lore.add(ChatColor.AQUA + "[Team Listing]");
        }
        lore.add(" ");
        if (isShulkerBox(display)) {
            lore.add(colorize(ctx.messages.shulkerPreviewHint()));
        }
        if (listing.sellerId().equals(viewer)) {
            lore.add(ChatColor.YELLOW + "Right-click to cancel this listing.");
        } else {
            lore.add(ChatColor.GREEN + "Left-click to buy this item.");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    public ItemStack decorateOrder(AuctionOrder order, UUID viewer) {
        ItemStack requested = order.requestedItem();
        if (requested == null || requested.getType() == Material.AIR) {
            return null;
        }

        ItemStack icon = requested.clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) {
            return icon;
        }

        OfflinePlayer buyer = ctx.plugin.getServer().getOfflinePlayer(order.buyerId());
        String buyerName = buyer != null && buyer.getName() != null ? buyer.getName() : "Unknown";

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Buyer: " + ChatColor.AQUA + buyerName
                + (order.buyerId().equals(viewer) ? ChatColor.GRAY + " (You)" : ""));
        lore.add(ChatColor.GRAY + "Offer: " + ChatColor.GOLD
                + ctx.transactionService.formatCurrency(order.offeredPrice()));
        appendEstimatedValue(requested, lore);
        appendShopPrice(requested, lore);
        lore.add(ChatColor.GRAY + "Expires: " + ChatColor.YELLOW
                + DateUtil.formatDate(order.expiryEpochMillis()));
        lore.add(" ");
        if (isShulkerBox(requested)) {
            lore.add(colorize(ctx.messages.shulkerPreviewHint()));
        }
        if (order.buyerId().equals(viewer)) {
            lore.add(ChatColor.YELLOW + "Right-click to cancel this order.");
        } else {
            lore.add(ChatColor.GREEN + "Left-click to fulfill this order.");
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    // -------------------------------------------------------------------------
    // Data helpers
    // -------------------------------------------------------------------------

    public AuctionListing findListing(String listingId) {
        if (listingId == null || listingId.isEmpty()) {
            return null;
        }
        return ctx.auctionManager.findListingById(listingId);
    }

    public AuctionOrder findOrder(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }
        for (AuctionOrder order : ctx.auctionManager.listActiveOrders()) {
            if (order.id().equalsIgnoreCase(orderId)) {
                return order;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Item / button factories
    // -------------------------------------------------------------------------

    public ItemStack createConfiguredFiller(AuctionMenuConfiguration.MenuButtonConfiguration configuration) {
        if (configuration == null) {
            return createButton(Material.GRAY_STAINED_GLASS_PANE, "&8 ", List.of());
        }
        return createButton(configuration.material(), configuration.displayName(), configuration.lore());
    }

    public ItemStack createConfiguredButton(AuctionMenuConfiguration.MenuButtonConfiguration configuration,
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

    public ItemStack createButton(Material material, String name, List<String> lore) {
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

    public void setPersistent(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null) {
            return;
        }
        ctx.itemTagStorage.setString(item, key, value);
    }

    public String colorize(String value) {
        if (value == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', value);
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
        if (!ctx.valueDisplayEnabled || item == null || lore == null) {
            return;
        }
        OptionalDouble estimate = ctx.itemValueProvider != null
                ? ctx.itemValueProvider.estimate(item) : OptionalDouble.empty();
        if (estimate.isEmpty()) {
            return;
        }
        String format = ctx.valueDisplayFormat != null && !ctx.valueDisplayFormat.isEmpty()
                ? ctx.valueDisplayFormat : "&7Value: &6{value}";
        String formatted = format.replace("{value}", ctx.transactionService.formatCurrency(estimate.getAsDouble()));
        lore.add(colorize(formatted));
    }

    private void appendShopPrice(ItemStack item, List<String> lore) {
        if (!ctx.shopPriceDisplayEnabled || item == null || lore == null) {
            return;
        }
        OptionalDouble shopPrice = ctx.shopPriceProvider != null
                ? ctx.shopPriceProvider.estimate(item) : OptionalDouble.empty();
        if (shopPrice.isEmpty()) {
            return;
        }
        String format = ctx.shopPriceFormat != null && !ctx.shopPriceFormat.isEmpty()
                ? ctx.shopPriceFormat : "&7Shop Price: &6{value}";
        String formatted = format.replace("{value}", ctx.transactionService.formatCurrency(shopPrice.getAsDouble()));
        lore.add(colorize(formatted));
    }

    // -------------------------------------------------------------------------
    // Button creation helpers
    // -------------------------------------------------------------------------

    private String formatBrowserTitle(Player player, BrowserView view, int page, int totalPages) {
        String rawTitle = ctx.browserConfig.title();
        if (rawTitle == null || rawTitle.isEmpty()) {
            rawTitle = "&2Auction House &7({page}/{total_pages})";
        }
        String formatted = rawTitle.replace("{page}", String.valueOf(page + 1))
                .replace("{total_pages}", String.valueOf(Math.max(1, totalPages)));
        if (formatted.contains("{view}")) {
            formatted = formatted.replace("{view}", view == BrowserView.ORDERS ? "Orders" : "Listings");
        }
        if (player != null) {
            String searchQuery = searchHandler.getSearchQuery(player.getUniqueId());
            if (searchQuery != null && !searchQuery.isEmpty()) {
                formatted = formatted + ChatColor.YELLOW + " [Searching]";
            }
        }
        return colorize(formatted);
    }

    private ItemStack createSearchButton(BrowserView view, String activeQuery) {
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = ctx.searchButtonConfig != null
                ? ctx.searchButtonConfig.button() : null;
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
            lore.add(ChatColor.GRAY + "Searching: " + ChatColor.AQUA
                    + searchHandler.formatSearchQueryForLore(activeQuery));
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

    private ItemStack createSortButton(BrowserView view, ListingSort listingSort, OrderSort orderSort) {
        AuctionMenuConfiguration.MenuButtonConfiguration buttonConfiguration = ctx.sortButtonConfig != null
                ? ctx.sortButtonConfig.button() : null;
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
                    ? buttonConfiguration.displayName() : "&bSort";
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
            AuctionMenuConfiguration.ToggleButtonConfiguration configuration,
            String activeStatusLine,
            String inactiveStatusLine) {
        AuctionMenuConfiguration.MenuButtonConfiguration buttonConfiguration = configuration != null
                ? configuration.button() : null;
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
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = ctx.searchTipsButtonConfig != null
                ? ctx.searchTipsButtonConfig.button() : null;
        Material material = configuration != null ? configuration.material() : Material.KNOWLEDGE_BOOK;
        String displayName = configuration != null ? configuration.displayName() : "&eSearch Tips";
        List<String> lore = configuration != null && configuration.lore() != null
                ? new ArrayList<>(configuration.lore()) : new ArrayList<>();
        return createButton(material, displayName, lore);
    }

    private ItemStack createClaimsButton(Player player) {
        AuctionMenuConfiguration.MenuButtonConfiguration configuration = ctx.claimsButtonConfig != null
                ? ctx.claimsButtonConfig.button() : null;
        Material material = configuration != null ? configuration.material() : Material.ENDER_CHEST;
        String displayName = configuration != null ? configuration.displayName() : "&6Pending Returns";
        List<String> lore = configuration != null && configuration.lore() != null
                ? new ArrayList<>(configuration.lore()) : new ArrayList<>();
        int claimCount = ctx.auctionManager.countPendingReturnItems(player.getUniqueId());
        if (claimCount > 0) {
            lore.add(ChatColor.GRAY + "Pending items: " + ChatColor.YELLOW + claimCount);
        }
        return createButton(material, displayName, lore);
    }

    private ItemStack createActivityButton() {
        String displayName = ChatColor.LIGHT_PURPLE + ctx.messages.activityTitle();
        List<String> lore = List.of(
                ChatColor.GRAY + ctx.messages.activityLore(),
                "",
                ChatColor.GREEN + "Click to open!"
        );
        return createButton(Material.NETHER_STAR, displayName, lore);
    }
}
