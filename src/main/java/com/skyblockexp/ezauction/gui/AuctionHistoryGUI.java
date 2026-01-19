package com.skyblockexp.ezauction.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.Bukkit;

import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistoryEntry;
import com.skyblockexp.ezauction.transaction.AuctionTransactionType;
import com.skyblockexp.ezauction.util.DateUtil;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;

/**
 * Tabbed GUI for viewing auction history (sales and purchases).
 * <p>
 * Use this class to display a player's auction transaction history in a modern, user-friendly interface.
 * Integrates with EzAuction's transaction history service and supports tab switching and permission checks.
 * </p>
 *
 * <p>To use:</p>
 * <ul>
 *   <li>Instantiate with the viewer, target, permission flag, and history service.</li>
 *   <li>Call {@link #open(Tab)} to display the GUI for a specific tab.</li>
 *   <li>Register an inventory click listener to handle tab switching and closing.</li>
 * </ul>
 *
 * @author Shadow48402
 * @since 1.2.0
 */
public class AuctionHistoryGUI {
    private final Player viewer;
    private final Player target;
    private final boolean canViewOthers;
    private final AuctionTransactionHistory historyService;
    private Inventory gui;
    private Tab currentTab = Tab.SALES;

    /**
     * Tabs for the auction history GUI.
     */
    public enum Tab {
        /** Player's sales (items sold to others). */
        SALES,
        /** Player's purchases (items bought from others). */
        PURCHASES
    }

    /**
     * Constructs a new AuctionHistoryGUI.
     *
     * @param viewer         The player viewing the GUI
     * @param target         The player whose history is being viewed
     * @param canViewOthers  Whether the viewer can view other players' history
     * @param historyService The transaction history service
     */
    public AuctionHistoryGUI(Player viewer, Player target, boolean canViewOthers, AuctionTransactionHistory historyService) {
        this.viewer = viewer;
        this.target = target;
        this.canViewOthers = canViewOthers;
        this.historyService = historyService;
        createGUI(Tab.SALES);
    }

    /**
     * Creates and populates the GUI for the given tab.
     * @param tab The tab to display (SALES or PURCHASES)
     */
    private void createGUI(Tab tab) {
        String tabLabel = tab == Tab.SALES ? "Sales" : "Purchases";
        String title = "Auction History (" + tabLabel + ")";
        gui = Bukkit.createInventory(null, 54, title);
        currentTab = tab;
        List<AuctionTransactionHistoryEntry> entries = historyService.getHistory(target.getUniqueId());
        AuctionTransactionType filterType = (tab == Tab.SALES) ? AuctionTransactionType.SELL : AuctionTransactionType.BUY;
        int slot = 0;
        for (AuctionTransactionHistoryEntry entry : entries) {
            if (entry.type() != filterType) continue;
            if (slot >= 45) break; // Only show up to 45 entries
            ItemStack item = entry.item() != null ? entry.item().clone() : new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((tab == Tab.SALES ? "Sold to " : "Bought from ") + entry.counterpartName());
                meta.setLore(List.of(
                    "Price: " + entry.price(),
                    "Date: " + DateUtil.formatDate(entry.timestamp())
                ));
                item.setItemMeta(meta);
            }
            gui.setItem(slot++, item);
        }
        // Tab switcher items
        ItemStack salesTab = new ItemStack(Material.CHEST);
        ItemMeta salesMeta = salesTab.getItemMeta();
        if (salesMeta != null) {
            salesMeta.setDisplayName("§eSales");
            salesTab.setItemMeta(salesMeta);
        }
        gui.setItem(48, salesTab);

        ItemStack purchasesTab = new ItemStack(Material.EMERALD);
        ItemMeta purchasesMeta = purchasesTab.getItemMeta();
        if (purchasesMeta != null) {
            purchasesMeta.setDisplayName("§aPurchases");
            purchasesTab.setItemMeta(purchasesMeta);
        }
        gui.setItem(50, purchasesTab);

        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§cClose");
            close.setItemMeta(closeMeta);
        }
        gui.setItem(49, close);

        // Back button to auction browser
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§eBack to Browser");
            backMeta.setLore(List.of("§7Return to the auction house."));
            back.setItemMeta(backMeta);
        }
        gui.setItem(45, back);
    }

    /**
     * Opens the auction history GUI for the viewer, displaying the specified tab.
     * @param tab The tab to display (SALES or PURCHASES)
     */
    public void open(Tab tab) {
        createGUI(tab);
        viewer.openInventory(gui);
    }

    /*
     * Note: Inventory click event handling (tab switching, close, etc.) should be implemented in a listener class.
     * Example:
     *   if (clickedSlot == 48) open(Tab.SALES);
     *   if (clickedSlot == 50) open(Tab.PURCHASES);
     *   if (clickedSlot == 49) viewer.closeInventory();
     * Permission checks for viewing others should be handled before constructing this GUI.
     */
}

