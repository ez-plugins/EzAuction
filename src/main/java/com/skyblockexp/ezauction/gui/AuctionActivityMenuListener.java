package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.compat.ItemTagStorage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event listener for the My Activity menu interactions.
 */
public class AuctionActivityMenuListener implements Listener {
    
    private static final String ACTION_KEY = "activity_action";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_VIEW_LISTING = "view_listing";
    private static final String ACTION_VIEW_ORDER = "view_order";
    private static final String ACTION_CLAIM = "claim";
    private static final String ACTION_TAB_LISTINGS = "tab_listings";
    private static final String ACTION_TAB_ORDERS = "tab_orders";
    private static final String ACTION_TAB_RETURNS = "tab_returns";
    private static final String ACTION_TAB_HISTORY = "tab_history";
    
    private final AuctionActivityMenu activityMenu;
    private final AuctionMenu auctionMenu;
    private final ItemTagStorage itemTagStorage;
    
    public AuctionActivityMenuListener(AuctionActivityMenu activityMenu, AuctionMenu auctionMenu, ItemTagStorage itemTagStorage) {
        this.activityMenu = activityMenu;
        this.auctionMenu = auctionMenu;
        this.itemTagStorage = itemTagStorage;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ActivityMenuHolder holder)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String action = getPersistent(clicked, ACTION_KEY);
        if (action == null) return;

        switch (action) {
            case ACTION_BACK -> {
                player.closeInventory();
                if (auctionMenu != null) {
                    auctionMenu.openBrowser(player);
                }
            }
            case ACTION_TAB_LISTINGS -> activityMenu.openActivityMenu(player, ActivityTab.MY_LISTINGS);
            case ACTION_TAB_ORDERS -> activityMenu.openActivityMenu(player, ActivityTab.MY_ORDERS);
            case ACTION_TAB_RETURNS -> activityMenu.openActivityMenu(player, ActivityTab.PENDING_RETURNS);
            case ACTION_TAB_HISTORY -> activityMenu.openActivityMenu(player, ActivityTab.RECENT_HISTORY);
            case ACTION_CLAIM -> {
                player.closeInventory();
                player.performCommand("auction claim");
            }
            case ACTION_VIEW_LISTING, ACTION_VIEW_ORDER -> {
                // Could open detailed view or return to main browser
                player.sendMessage(ChatColor.GRAY + "Use the auction browser to manage this item.");
            }
        }
    }
    
    private String getPersistent(ItemStack item, String key) {
        if (itemTagStorage != null && item != null) {
            return itemTagStorage.get(item, key);
        }
        return null;
    }
}
