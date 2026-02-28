package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionListing;

import java.util.Comparator;
import java.util.List;

public enum ListingSort {
    ENDING_SOON("Ending Soon", Comparator.comparingLong(AuctionListing::expiryEpochMillis)
            .thenComparingDouble(AuctionListing::price)
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    NEWLY_LISTED("Newly Listed", Comparator.comparingLong(AuctionListing::expiryEpochMillis)
            .reversed()
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionListing::price)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    PRICE_LOW_HIGH("Lowest Price", Comparator.comparingDouble(AuctionListing::price)
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AuctionListing::expiryEpochMillis)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    PRICE_HIGH_LOW("Highest Price", Comparator.comparingDouble(AuctionListing::price)
            .reversed()
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AuctionListing::expiryEpochMillis)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    QUANTITY_HIGH_LOW("Quantity (High-Low)", Comparator.comparingInt(AuctionMenuUtils::listingQuantity)
            .reversed()
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionListing::price)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    QUANTITY_LOW_HIGH("Quantity (Low-High)", Comparator.comparingInt(AuctionMenuUtils::listingQuantity)
            .thenComparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionListing::price)
            .thenComparingLong(AuctionListing::expiryEpochMillis)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    ITEM_A_Z("Item Name (A-Z)", Comparator.comparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionListing::price)
            .thenComparingLong(AuctionListing::expiryEpochMillis)
            .thenComparing(AuctionListing::id, String.CASE_INSENSITIVE_ORDER)),
    ITEM_Z_A("Item Name (Z-A)", Comparator.comparing(AuctionMenuUtils::listingItemSortKey, String.CASE_INSENSITIVE_ORDER)
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
