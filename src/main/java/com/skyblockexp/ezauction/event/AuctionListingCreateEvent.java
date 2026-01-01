package com.skyblockexp.ezauction.event;

import com.skyblockexp.ezauction.AuctionListing;
import org.bukkit.event.Cancellable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Called when a new auction listing is about to be created.
 * This event is fired before the listing is finalized and can be cancelled.
 */
public class AuctionListingCreateEvent extends AuctionListingEvent implements Cancellable {
    private Player player;
    private ItemStack item;
    private double price;
    private boolean cancelled;

    // Constructor for pre-listing (intent phase)
    public AuctionListingCreateEvent(Player player, ItemStack item, double price) {
        super(null);
        this.player = player;
        this.item = item;
        this.price = price;
        this.cancelled = false;
    }

    // Constructor for when AuctionListing is available (post-listing phase, if needed)
    public AuctionListingCreateEvent(AuctionListing listing, Player player, ItemStack item, double price) {
        super(listing);
        this.player = player;
        this.item = item;
        this.price = price;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
