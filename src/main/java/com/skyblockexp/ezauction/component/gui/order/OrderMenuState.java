package com.skyblockexp.ezauction.component.gui.order;

import org.bukkit.inventory.ItemStack;

import com.skyblockexp.ezauction.EzAuctionPlugin;

import java.time.Duration;
import java.util.logging.Logger;

public class OrderMenuState {
    private final ItemStack template;
    private final int maxQuantity;
    private double pricePerItem;
    private int quantity;
    private int durationIndex;
    private final Double recommendedPricePerItem;
    private final Duration[] durationOptions;
    private final int defaultDurationIndex;

    // Debug logging support (instance-based)
    private final boolean debugEnabled;
    private static final Logger LOGGER = Logger.getLogger(OrderMenuState.class.getName());

    public OrderMenuState(ItemStack template, double pricePerItem, int quantity, int durationIndex,
                          Double recommendedPricePerItem, Duration[] durationOptions, int defaultDurationIndex) {
        if (template == null) {
            throw new IllegalArgumentException("template cannot be null");
        }
        this.template = template.clone();
        this.template.setAmount(1);
        this.maxQuantity = Math.max(1, template.getMaxStackSize());
        this.pricePerItem = pricePerItem;
        setQuantity(quantity);
        this.durationOptions = (durationOptions != null) ? durationOptions : new Duration[0];
        this.durationIndex = normalizeIndex(durationIndex);
        this.recommendedPricePerItem = recommendedPricePerItem;
        this.defaultDurationIndex = defaultDurationIndex;
        
        // Safely access debug configuration with null checks
        boolean debug = false;
        try {
            if (EzAuctionPlugin.getStaticRegistry() != null && EzAuctionPlugin.getStaticRegistry().getConfiguration() != null) {
                debug = EzAuctionPlugin.getStaticRegistry().getConfiguration().debug();
            }
        } catch (Exception e) {
            LOGGER.fine("[OrderMenuState] Debug disabled due to configuration access error: " + e.getMessage());
        }
        this.debugEnabled = debug;
        
        logDebug("OrderMenuState created: item=" + template.getType() + "x" + template.getAmount() 
            + ", pricePerItem=" + pricePerItem + ", quantity=" + quantity + ", durationIndex=" + durationIndex);
    }

    public ItemStack item() {
        ItemStack clone = template.clone();
        clone.setAmount(quantity);
        return clone;
    }

    public double pricePerItem() {
        return pricePerItem;
    }

    public void setPricePerItem(double pricePerItem) {
        this.pricePerItem = pricePerItem;
        logDebug("Price per item set: " + pricePerItem);
    }

    public Double recommendedPricePerItem() {
        return recommendedPricePerItem;
    }

    public int quantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        int clamped = Math.max(1, Math.min(quantity, maxQuantity));
        this.quantity = clamped;
        logDebug("Quantity set: " + clamped);
    }

    public void adjustQuantity(int delta) {
        setQuantity(quantity + delta);
        logDebug("Quantity adjusted by " + delta + ", new quantity: " + quantity);
    }

    public int maxQuantity() {
        return maxQuantity;
    }

    public double totalPrice() {
        return pricePerItem * quantity;
        // logDebug not needed here, calculation only
    }

    public Duration duration() {
        if (durationOptions.length == 0) {
            return null;
        }
        return durationOptions[durationIndex];
        // logDebug not needed here, getter only
    }

    public void cycleDuration() {
        if (durationOptions.length == 0) {
            return;
        }
        durationIndex = (durationIndex + 1) % durationOptions.length;
        logDebug("Duration cycled, new index: " + durationIndex);
    }

    private int normalizeIndex(int requested) {
        if (durationOptions.length == 0) {
            return 0;
        }
        if (requested < 0 || requested >= durationOptions.length) {
            return Math.min(defaultDurationIndex, durationOptions.length - 1);
        }
        return requested;
    }

    // No static setter; debugEnabled is set per instance from registry/config

    /**
     * Log debug messages if debug is enabled (instance-based).
     */
    private void logDebug(String message) {
        if (debugEnabled) {
            LOGGER.info("[OrderMenuState][DEBUG] " + message);
        }
    }
}
