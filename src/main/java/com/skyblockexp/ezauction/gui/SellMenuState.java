package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.config.AuctionListingRules;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.inventory.ItemStack;

/** Value object holding the player's in-progress sell configuration. */
public final class SellMenuState {

    private final ItemStack item;
    private double price;
    private int durationIndex;
    private final Double recommendedPrice;

    private final Duration[] durationOptions;
    private final AuctionListingRules listingRules;
    private final int defaultDurationIndex;

    public SellMenuState(ItemStack item,
                         double price,
                         int defaultDurationIndex,
                         Double recommendedPrice,
                         Duration[] durationOptions,
                         AuctionListingRules listingRules) {
        this.item = Objects.requireNonNull(item, "item").clone();
        this.price = price;
        this.durationOptions = durationOptions != null ? durationOptions : new Duration[0];
        this.listingRules = Objects.requireNonNull(listingRules, "listingRules");
        this.defaultDurationIndex = Math.max(0, defaultDurationIndex);
        this.durationIndex = normalizeIndex(defaultDurationIndex);
        this.recommendedPrice = recommendedPrice;
    }

    public ItemStack item() {
        return item.clone();
    }

    public double price() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double recommendedPrice() {
        return recommendedPrice;
    }

    public Duration duration() {
        if (durationOptions.length == 0) {
            return listingRules.defaultDuration();
        }
        return durationOptions[durationIndex];
    }

    public int durationIndex() {
        return durationIndex;
    }

    public void cycleDuration() {
        if (durationOptions.length == 0) return;
        durationIndex = (durationIndex + 1) % durationOptions.length;
    }

    private int normalizeIndex(int requested) {
        if (durationOptions.length == 0) return 0;
        if (requested < 0 || requested >= durationOptions.length) {
            return Math.min(defaultDurationIndex, durationOptions.length - 1);
        }
        return requested;
    }
}
