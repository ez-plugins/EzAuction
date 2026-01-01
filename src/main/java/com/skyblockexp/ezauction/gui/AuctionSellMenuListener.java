package com.skyblockexp.ezauction.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Thin listener that delegates events to AuctionSellMenu. */
public class AuctionSellMenuListener implements Listener {

    private final AuctionSellMenu menu;

    public AuctionSellMenuListener(AuctionSellMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        menu.handleInventoryDrag(event);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        menu.handleInventoryClick(event);
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        menu.handleAsyncPlayerChat(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        menu.handlePlayerQuit(event);
    }
}
