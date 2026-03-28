package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.compat.ItemTagStorage;
import com.skyblockexp.ezauction.config.AuctionMessageConfiguration;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.util.AuctionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consolidated "My Activity" menu showing all player auction activities:
 * - Active Listings
 * - Buy Orders
 * - Pending Returns
 * - Recent History
 */
public class AuctionActivityMenu {

    private static final String ACTION_KEY = "activity_action";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_VIEW_LISTING = "view_listing";
    private static final String ACTION_VIEW_ORDER = "view_order";
    private static final String ACTION_CLAIM = "claim";
    private static final String ACTION_TAB_LISTINGS = "tab_listings";
    private static final String ACTION_TAB_ORDERS = "tab_orders";
    private static final String ACTION_TAB_RETURNS = "tab_returns";
    private static final String ACTION_TAB_HISTORY = "tab_history";

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionTransactionHistory transactionHistory;
    private final AuctionMenu auctionMenu;
    private final ItemTagStorage itemTagStorage;
    private final AuctionMessageConfiguration.BrowserMessages messages;
    private final AuctionMessageConfiguration.ActivityMessages activityMessages;

    // Activity messages are supplied by AuctionMessageConfiguration.ActivityMessages

    public AuctionActivityMenu(
            JavaPlugin plugin,
            AuctionManager auctionManager,
            AuctionTransactionService transactionService,
            AuctionTransactionHistory transactionHistory,
            AuctionMenu auctionMenu,
            ItemTagStorage itemTagStorage,
            AuctionMessageConfiguration.BrowserMessages messages,
            AuctionMessageConfiguration.ActivityMessages activityMessages) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.transactionService = transactionService;
        this.transactionHistory = transactionHistory;
        this.auctionMenu = auctionMenu;
        this.itemTagStorage = itemTagStorage;
        this.messages = messages != null ? messages : AuctionMessageConfiguration.BrowserMessages.defaults();
        this.activityMessages = activityMessages != null ? activityMessages : AuctionMessageConfiguration.ActivityMessages.defaults();
    }

    /**
     * Opens the activity menu for the player at the specified tab.
     */
    public void openActivityMenu(Player player, ActivityTab tab) {
        UUID playerId = player.getUniqueId();
        String title = formatTitle(tab);
        ActivityMenuHolder holder = new ActivityMenuHolder(playerId, tab);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inventory);

        // Apply filler
        ItemStack filler = createFiller();
        for (int i = 45; i < 54; i++) {
            if (i != 45 && i != 46 && i != 48 && i != 49 && i != 50 && i != 52 && i != 53) {
                inventory.setItem(i, filler);
            }
        }

        // Add navigation buttons (bottom row)
        inventory.setItem(45, createBackButton());
        inventory.setItem(46, createTabButton(ActivityTab.MY_LISTINGS, tab == ActivityTab.MY_LISTINGS));
        inventory.setItem(48, createTabButton(ActivityTab.MY_ORDERS, tab == ActivityTab.MY_ORDERS));
        inventory.setItem(49, createCloseButton());
        inventory.setItem(50, createTabButton(ActivityTab.PENDING_RETURNS, tab == ActivityTab.PENDING_RETURNS));
        inventory.setItem(52, createTabButton(ActivityTab.RECENT_HISTORY, tab == ActivityTab.RECENT_HISTORY));
        inventory.setItem(53, createClaimButton(player));

        // Populate content based on tab
        switch (tab) {
            case MY_LISTINGS -> populateListings(inventory, playerId);
            case MY_ORDERS -> populateOrders(inventory, playerId);
            case PENDING_RETURNS -> populateReturns(inventory, playerId, player);
            case RECENT_HISTORY -> populateHistory(inventory, playerId);
        }

        player.openInventory(inventory);
    }

    private void populateListings(Inventory inventory, UUID playerId) {
        List<AuctionListing> myListings = auctionManager.listActiveListings().stream()
                .filter(listing -> playerId.equals(listing.seller()))
                .collect(Collectors.toList());

        if (myListings.isEmpty()) {
            inventory.setItem(22, createEmptyIndicator(activityMessages.noListings()));
            return;
        }

        int slot = 0;
        for (AuctionListing listing : myListings) {
            if (slot >= 45) break;
            ItemStack display = createListingDisplay(listing);
            inventory.setItem(slot++, display);
        }
    }

    private void populateOrders(Inventory inventory, UUID playerId) {
        List<AuctionOrder> myOrders = auctionManager.listActiveOrders().stream()
                .filter(order -> playerId.equals(order.buyer()))
                .collect(Collectors.toList());

        if (myOrders.isEmpty()) {
            inventory.setItem(22, createEmptyIndicator(activityMessages.noOrders()));
            return;
        }

        int slot = 0;
        for (AuctionOrder order : myOrders) {
            if (slot >= 45) break;
            ItemStack display = createOrderDisplay(order);
            inventory.setItem(slot++, display);
        }
    }

    private void populateReturns(Inventory inventory, UUID playerId, Player player) {
        int returnCount = auctionManager.countPendingReturnItems(playerId);
        
        if (returnCount == 0) {
            inventory.setItem(22, createEmptyIndicator(activityMessages.noReturns()));
            return;
        }

        // Show claim button prominently
        ItemStack claimPrompt = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = claimPrompt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + activityMessages.claimPromptTitle());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + activityMessages.claimPromptLine1().replace("{count}", String.valueOf(returnCount)));
            lore.add(ChatColor.GRAY + activityMessages.claimPromptLine2());
            meta.setLore(lore);
            claimPrompt.setItemMeta(meta);
        }
        inventory.setItem(22, claimPrompt);
    }

    private void populateHistory(Inventory inventory, UUID playerId) {
        List<AuctionTransactionHistoryEntry> history = transactionHistory != null
                ? transactionHistory.getHistory(playerId)
                : new ArrayList<>();

        if (history.isEmpty()) {
            inventory.setItem(22, createEmptyIndicator(activityMessages.noHistory()));
            return;
        }

        // Limit to 45 most recent entries for the inventory
        int maxEntries = Math.min(45, history.size());
        int slot = 0;
        for (int i = 0; i < maxEntries; i++) {
            if (slot >= 45) break;
            ItemStack display = createHistoryDisplay(history.get(i));
            inventory.setItem(slot++, display);
        }
    }

    /**
     * Creates a fallback barrier item for invalid/null items.
     */
    private ItemStack createInvalidItemFallback() {
        ItemStack itemStack = new ItemStack(Material.BARRIER);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + activityMessages.invalidItemTitle());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack createListingDisplay(AuctionListing listing) {
        ItemStack itemStack = listing.item();
        if (itemStack == null) {
            return createInvalidItemFallback();
        }
        ItemStack display = itemStack.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GOLD + activityMessages.labelPrice() + ChatColor.WHITE + transactionService.formatCurrency(listing.price()));
            lore.add(ChatColor.GRAY + activityMessages.labelQuantity() + ChatColor.WHITE + listing.item().getAmount());
            lore.add(ChatColor.GRAY + activityMessages.labelExpires() + ChatColor.WHITE + AuctionUtils.formatTimeRemaining(Instant.ofEpochMilli(listing.expiresAt())));
            lore.add("");
            lore.add(ChatColor.YELLOW + activityMessages.listingTag());
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        setPersistent(display, ACTION_KEY, ACTION_VIEW_LISTING);
        return display;
    }

    private ItemStack createOrderDisplay(AuctionOrder order) {
        ItemStack itemStack = order.itemTemplate();
        if (itemStack == null) {
            return createInvalidItemFallback();
        }
        ItemStack display = itemStack.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GOLD + activityMessages.labelPricePerItem() + ChatColor.WHITE + transactionService.formatCurrency(order.pricePerItem()));
            lore.add(ChatColor.GRAY + activityMessages.labelQuantityWanted() + ChatColor.WHITE + order.quantity());
            lore.add(ChatColor.GOLD + activityMessages.labelTotal() + ChatColor.WHITE + transactionService.formatCurrency(order.pricePerItem() * order.quantity()));
            lore.add(ChatColor.GRAY + activityMessages.labelExpires() + ChatColor.WHITE + AuctionUtils.formatTimeRemaining(Instant.ofEpochMilli(order.expiresAt())));
            lore.add("");
            lore.add(ChatColor.YELLOW + activityMessages.orderTag());
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        setPersistent(display, ACTION_KEY, ACTION_VIEW_ORDER);
        return display;
    }

    private ItemStack createHistoryDisplay(AuctionTransactionHistoryEntry entry) {
        ItemStack display = entry.item() != null ? entry.item().clone() : new ItemStack(Material.PAPER);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String itemName = "Unknown";
            if (entry.item() != null && entry.item().getItemMeta() != null) {
                ItemMeta entryMeta = entry.item().getItemMeta();
                if (entryMeta.hasDisplayName()) {
                    itemName = ChatColor.stripColor(entryMeta.getDisplayName());
                } else {
                    itemName = entry.item().getType().name();
                }
            } else if (entry.item() != null) {
                itemName = entry.item().getType().name();
            }
            
            meta.setDisplayName(ChatColor.WHITE + itemName);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + activityMessages.labelType() + ChatColor.WHITE + entry.type().name());
            lore.add(ChatColor.GOLD + activityMessages.labelPrice() + ChatColor.WHITE + transactionService.formatCurrency(entry.price()));
            lore.add(ChatColor.GRAY + activityMessages.labelTime() + ChatColor.WHITE + AuctionUtils.formatTimeAgo(Instant.ofEpochMilli(entry.timestamp())));
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + activityMessages.labelTransactionId() + entry.transactionId().substring(0, 8));
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createTabButton(ActivityTab tab, boolean active) {
        Material material;
        String displayName;
        String description;

        switch (tab) {
            case MY_LISTINGS -> {
                material = active ? Material.LIME_STAINED_GLASS_PANE : Material.CHEST;
                displayName = ChatColor.YELLOW + activityMessages.tabListingsLabel();
                description = activityMessages.tabListingsDesc();
            }
            case MY_ORDERS -> {
                material = active ? Material.LIME_STAINED_GLASS_PANE : Material.PAPER;
                displayName = ChatColor.YELLOW + activityMessages.tabOrdersLabel();
                description = activityMessages.tabOrdersDesc();
            }
            case PENDING_RETURNS -> {
                material = active ? Material.LIME_STAINED_GLASS_PANE : Material.ENDER_CHEST;
                displayName = ChatColor.GOLD + activityMessages.tabReturnsLabel();
                description = activityMessages.tabReturnsDesc();
            }
            case RECENT_HISTORY -> {
                material = active ? Material.LIME_STAINED_GLASS_PANE : Material.BOOK;
                displayName = ChatColor.AQUA + activityMessages.tabHistoryLabel();
                description = activityMessages.tabHistoryDesc();
            }
            default -> {
                material = Material.BARRIER;
                displayName = "Unknown";
                description = "";
            }
        }

        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            if (active) {
                lore.add("");
                lore.add(ChatColor.GREEN + activityMessages.tabActiveSuffix());
            } else {
                lore.add("");
                lore.add(ChatColor.YELLOW + activityMessages.tabClickText());
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }

        String action = switch (tab) {
            case MY_LISTINGS -> ACTION_TAB_LISTINGS;
            case MY_ORDERS -> ACTION_TAB_ORDERS;
            case PENDING_RETURNS -> ACTION_TAB_RETURNS;
            case RECENT_HISTORY -> ACTION_TAB_HISTORY;
            default -> ACTION_TAB_LISTINGS; // Fallback
        };
        
        setPersistent(button, ACTION_KEY, action);
        return button;
    }

    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + activityMessages.backLabel());
            meta.setLore(List.of(ChatColor.GRAY + activityMessages.backLore()));
            button.setItemMeta(meta);
        }
        setPersistent(button, ACTION_KEY, ACTION_BACK);
        return button;
    }

    private ItemStack createCloseButton() {
        ItemStack button = new ItemStack(Material.BARRIER);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + activityMessages.closeLabel());
            meta.setLore(List.of(ChatColor.GRAY + activityMessages.closeLore()));
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createClaimButton(Player player) {
        int count = auctionManager.countPendingReturnItems(player.getUniqueId());
        ItemStack button = new ItemStack(Material.CHEST);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + activityMessages.claimButtonLabel());
            List<String> lore = new ArrayList<>();
            if (count > 0) {
                lore.add(ChatColor.GRAY + activityMessages.claimButtonPendingLine().replace("{count}", String.valueOf(count)));
                lore.add(ChatColor.GREEN + activityMessages.tabClickText());
            } else {
                lore.add(ChatColor.GRAY + activityMessages.claimButtonNoPendingLine());
            }
            meta.setLore(lore);
            button.setItemMeta(meta);
        }
        setPersistent(button, ACTION_KEY, ACTION_CLAIM);
        return button;
    }

    private ItemStack createEmptyIndicator(String message) {
        ItemStack indicator = new ItemStack(Material.BARRIER);
        ItemMeta meta = indicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + activityMessages.emptyIndicatorTitle());
            meta.setLore(List.of(ChatColor.GRAY + message));
            indicator.setItemMeta(meta);
        }
        return indicator;
    }

    private ItemStack createFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + " ");
            filler.setItemMeta(meta);
        }
        return filler;
    }

    private String formatTitle(ActivityTab tab) {
        String tabName = switch (tab) {
            case MY_LISTINGS -> activityMessages.tabListingsLabel();
            case MY_ORDERS -> activityMessages.tabOrdersLabel();
            case PENDING_RETURNS -> activityMessages.tabReturnsLabel();
            case RECENT_HISTORY -> activityMessages.tabHistoryLabel();
            default -> activityMessages.titlePrefix(); // Fallback
        };
        return ChatColor.DARK_GREEN + activityMessages.titlePrefix() + " " + ChatColor.DARK_GRAY + "(" + tabName + ")";
    }

    private void setPersistent(ItemStack item, String key, String value) {
        if (itemTagStorage != null && item != null) {
            itemTagStorage.set(item, key, value);
        }
    }
}
