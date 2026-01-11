package com.skyblockexp.ezauction.gui;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for handling auction history GUI interactions (tab switching, close, etc).
 * Maintains per-player GUI state for robust tab switching.
 *
 * @author Shadow48402
 * @since 1.2.0
 */
public class AuctionHistoryListener implements Listener {
    // Track open AuctionHistoryGUI per player (UUID)
    private final Map<UUID, AuctionHistoryGUI> guiMap = new ConcurrentHashMap<>();

    /**
     * Register a new AuctionHistoryGUI for a player.
     * Call this when opening the GUI for a player.
     * @param player The player
     * @param gui The AuctionHistoryGUI instance
     */
    public void registerGUI(Player player, AuctionHistoryGUI gui) {
        guiMap.put(player.getUniqueId(), gui);
    }

    /**
     * Unregister the GUI for a player (on close).
     * @param player The player
     */
    public void unregisterGUI(Player player) {
        guiMap.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null || inv.getHolder() != null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("Auction History")) return;
        int slot = event.getRawSlot();
        event.setCancelled(true);

        AuctionHistoryGUI gui = guiMap.get(player.getUniqueId());
        if (gui == null) return;

        if (slot == 48) {
            // Switch to Sales tab
            gui.open(AuctionHistoryGUI.Tab.SALES);
        } else if (slot == 50) {
            // Switch to Purchases tab
            gui.open(AuctionHistoryGUI.Tab.PURCHASES);
        } else if (slot == 49) {
            player.closeInventory();
        }
    }

    // Handle inventory close to clean up per-player GUI tracking
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (inv == null || inv.getHolder() != null) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("Auction History")) return;
        unregisterGUI(player);
    }
}
