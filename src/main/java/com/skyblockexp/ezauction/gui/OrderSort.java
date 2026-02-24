package com.skyblockexp.ezauction.gui;

import com.skyblockexp.ezauction.AuctionOrder;

import java.util.Comparator;
import java.util.List;

public enum OrderSort {
    ENDING_SOON("Ending Soon", Comparator.comparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    NEWLY_POSTED("Newly Posted", Comparator.comparingLong(AuctionOrder::expiryEpochMillis)
            .reversed()
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    PRICE_HIGH_LOW("Highest Offer", Comparator.comparingDouble(AuctionOrder::offeredPrice)
            .reversed()
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    PRICE_LOW_HIGH("Lowest Offer", Comparator.comparingDouble(AuctionOrder::offeredPrice)
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    QUANTITY_HIGH_LOW("Quantity (High-Low)", Comparator.comparingInt(AuctionMenuUtils::orderQuantity)
            .reversed()
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    QUANTITY_LOW_HIGH("Quantity (Low-High)", Comparator.comparingInt(AuctionMenuUtils::orderQuantity)
            .thenComparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    ITEM_A_Z("Item Name (A-Z)", Comparator.comparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER)),
    ITEM_Z_A("Item Name (Z-A)", Comparator.comparing(AuctionMenuUtils::orderItemSortKey, String.CASE_INSENSITIVE_ORDER)
            .reversed()
            .thenComparingDouble(AuctionOrder::offeredPrice)
            .thenComparingLong(AuctionOrder::expiryEpochMillis)
            .thenComparing(AuctionOrder::id, String.CASE_INSENSITIVE_ORDER));

    private static final OrderSort[] VALUES = values();

    private final String label;
    private final Comparator<AuctionOrder> comparator;

    OrderSort(String label, Comparator<AuctionOrder> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label() {
        return label;
    }

    public void sort(List<AuctionOrder> orders) {
        if (orders == null || orders.size() <= 1) {
            return;
        }
        orders.sort(comparator);
    }

    public OrderSort next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public OrderSort previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }
}
