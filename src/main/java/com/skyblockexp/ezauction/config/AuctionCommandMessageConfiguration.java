package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration container for command and hologram messages exposed by EzAuction.
 */
public final class AuctionCommandMessageConfiguration {

    private final GeneralMessages general;
    private final SellMessages sell;
    private final OrderMessages order;
    private final LiveMessages live;
    private final UsageMessages usage;
    private final CancelMessages cancel;
    private final HistoryMessages history;
    private final HologramMessages holograms;

    public AuctionCommandMessageConfiguration(GeneralMessages general, SellMessages sell, OrderMessages order,
            LiveMessages live, UsageMessages usage, CancelMessages cancel, HistoryMessages history,
            HologramMessages holograms) {
        this.general = general != null ? general : GeneralMessages.defaults();
        this.sell = sell != null ? sell : SellMessages.defaults();
        this.order = order != null ? order : OrderMessages.defaults();
        this.live = live != null ? live : LiveMessages.defaults();
        this.usage = usage != null ? usage : UsageMessages.defaults();
        this.cancel = cancel != null ? cancel : CancelMessages.defaults();
        this.history = history != null ? history : HistoryMessages.defaults();
        this.holograms = holograms != null ? holograms : HologramMessages.defaults();
    }

    public GeneralMessages general() {
        return general;
    }

    public SellMessages sell() {
        return sell;
    }

    public OrderMessages order() {
        return order;
    }

    public LiveMessages live() {
        return live;
    }

    public UsageMessages usage() {
        return usage;
    }

    public CancelMessages cancel() {
        return cancel;
    }

    public HistoryMessages history() {
        return history;
    }

    public HologramMessages holograms() {
        return holograms;
    }

    public static AuctionCommandMessageConfiguration defaults() {
        return new AuctionCommandMessageConfiguration(GeneralMessages.defaults(), SellMessages.defaults(),
                OrderMessages.defaults(), LiveMessages.defaults(), UsageMessages.defaults(), CancelMessages.defaults(),
                HistoryMessages.defaults(), HologramMessages.defaults());
    }

    public static AuctionCommandMessageConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        GeneralMessages general = GeneralMessages.from(section.getConfigurationSection("general"));
        SellMessages sell = SellMessages.from(section.getConfigurationSection("sell"));
        OrderMessages order = OrderMessages.from(section.getConfigurationSection("order"));
        LiveMessages live = LiveMessages.from(section.getConfigurationSection("live"));
        UsageMessages usage = UsageMessages.from(section.getConfigurationSection("usage"));
        CancelMessages cancel = CancelMessages.from(section.getConfigurationSection("cancel"));
        HistoryMessages history = HistoryMessages.from(section.getConfigurationSection("history"));
        HologramMessages holograms = HologramMessages.from(section.getConfigurationSection("holograms"));
        return new AuctionCommandMessageConfiguration(general, sell, order, live, usage, cancel, history, holograms);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    public record GeneralMessages(
            String consoleOnly,
            String auctionNoPermission,
            String invalidDuration,
            String unknownCounterpart,
            String unknownItem,
            String unknownMaterial) {

        private static final String DEFAULT_CONSOLE_ONLY = "&cOnly players can use the auction house.";
        private static final String DEFAULT_AUCTION_NO_PERMISSION = "&cYou do not have permission to use the auction house.";
        private static final String DEFAULT_INVALID_DURATION = "&cInvalid duration. Use values such as 12h, 30m, or 2d.";
        private static final String DEFAULT_UNKNOWN_COUNTERPART = "Unknown";
        private static final String DEFAULT_UNKNOWN_ITEM = "Unknown item";
        private static final String DEFAULT_UNKNOWN_MATERIAL = "Unknown";

        public GeneralMessages {
            consoleOnly = normalize(consoleOnly, DEFAULT_CONSOLE_ONLY);
            auctionNoPermission = normalize(auctionNoPermission, DEFAULT_AUCTION_NO_PERMISSION);
            invalidDuration = normalize(invalidDuration, DEFAULT_INVALID_DURATION);
            unknownCounterpart = normalize(unknownCounterpart, DEFAULT_UNKNOWN_COUNTERPART);
            unknownItem = normalize(unknownItem, DEFAULT_UNKNOWN_ITEM);
            unknownMaterial = normalize(unknownMaterial, DEFAULT_UNKNOWN_MATERIAL);
        }

        public static GeneralMessages defaults() {
            return new GeneralMessages(DEFAULT_CONSOLE_ONLY, DEFAULT_AUCTION_NO_PERMISSION, DEFAULT_INVALID_DURATION,
                    DEFAULT_UNKNOWN_COUNTERPART, DEFAULT_UNKNOWN_ITEM, DEFAULT_UNKNOWN_MATERIAL);
        }

        public static GeneralMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new GeneralMessages(
                    section.getString("console-only"),
                    section.getString("auction-no-permission"),
                    section.getString("invalid-duration"),
                    section.getString("unknown-counterpart"),
                    section.getString("unknown-item"),
                    section.getString("unknown-material"));
        }
    }

    public record SellMessages(
            String noPermission,
            String itemRequired,
            String invalidPrice,
            String pricePositive,
            String priceMinimum) {

        private static final String DEFAULT_NO_PERMISSION = "&cYou do not have permission to create auction listings.";
        private static final String DEFAULT_ITEM_REQUIRED = "&cYou must hold the item you want to list in your main hand.";
        private static final String DEFAULT_INVALID_PRICE = "&cInvalid price: {price}";
        private static final String DEFAULT_PRICE_POSITIVE = "&cListing price must be positive.";
        private static final String DEFAULT_PRICE_MINIMUM = "&cListing price must be at least &6{minimum}&c.";

        public SellMessages {
            noPermission = normalize(noPermission, DEFAULT_NO_PERMISSION);
            itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
            invalidPrice = normalize(invalidPrice, DEFAULT_INVALID_PRICE);
            pricePositive = normalize(pricePositive, DEFAULT_PRICE_POSITIVE);
            priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
        }

        public static SellMessages defaults() {
            return new SellMessages(DEFAULT_NO_PERMISSION, DEFAULT_ITEM_REQUIRED, DEFAULT_INVALID_PRICE,
                    DEFAULT_PRICE_POSITIVE, DEFAULT_PRICE_MINIMUM);
        }

        public static SellMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new SellMessages(
                    section.getString("no-permission"),
                    section.getString("item-required"),
                    section.getString("invalid-price"),
                    section.getString("price-positive"),
                    section.getString("price-minimum"));
        }
    }

    public record OrderMessages(
            String noPermission,
            String itemRequired,
            String invalidPrice,
            String pricePositive,
            String priceMinimum,
            String invalidAmount,
            String amountMinimum,
            String amountMaxStack) {

        private static final String DEFAULT_NO_PERMISSION = "&cYou do not have permission to create buy orders.";
        private static final String DEFAULT_ITEM_REQUIRED = "&cYou must hold the item you want to request in your main hand.";
        private static final String DEFAULT_INVALID_PRICE = "&cInvalid price: {price}";
        private static final String DEFAULT_PRICE_POSITIVE = "&cPrice must be positive.";
        private static final String DEFAULT_PRICE_MINIMUM = "&cPrice per item must be at least &6{minimum}&c.";
        private static final String DEFAULT_INVALID_AMOUNT = "&cInvalid amount: {amount}";
        private static final String DEFAULT_AMOUNT_MINIMUM = "&cAmount must be at least 1.";
        private static final String DEFAULT_AMOUNT_MAX_STACK = "&cAmount cannot exceed the item's max stack size of {max}.";

        public OrderMessages {
            noPermission = normalize(noPermission, DEFAULT_NO_PERMISSION);
            itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
            invalidPrice = normalize(invalidPrice, DEFAULT_INVALID_PRICE);
            pricePositive = normalize(pricePositive, DEFAULT_PRICE_POSITIVE);
            priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
            invalidAmount = normalize(invalidAmount, DEFAULT_INVALID_AMOUNT);
            amountMinimum = normalize(amountMinimum, DEFAULT_AMOUNT_MINIMUM);
            amountMaxStack = normalize(amountMaxStack, DEFAULT_AMOUNT_MAX_STACK);
        }

        public static OrderMessages defaults() {
            return new OrderMessages(DEFAULT_NO_PERMISSION, DEFAULT_ITEM_REQUIRED, DEFAULT_INVALID_PRICE,
                    DEFAULT_PRICE_POSITIVE, DEFAULT_PRICE_MINIMUM, DEFAULT_INVALID_AMOUNT, DEFAULT_AMOUNT_MINIMUM,
                    DEFAULT_AMOUNT_MAX_STACK);
        }

        public static OrderMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new OrderMessages(
                    section.getString("no-permission"),
                    section.getString("item-required"),
                    section.getString("invalid-price"),
                    section.getString("price-positive"),
                    section.getString("price-minimum"),
                    section.getString("invalid-amount"),
                    section.getString("amount-minimum"),
                    section.getString("amount-max-stack"));
        }
    }

    public record LiveMessages(String noPermission, String disabled) {

        private static final String DEFAULT_NO_PERMISSION = "&cYou do not have permission to view live auctions.";
        private static final String DEFAULT_DISABLED = "&cLive auctions are currently disabled.";

        public LiveMessages {
            noPermission = normalize(noPermission, DEFAULT_NO_PERMISSION);
            disabled = normalize(disabled, DEFAULT_DISABLED);
        }

        public static LiveMessages defaults() {
            return new LiveMessages(DEFAULT_NO_PERMISSION, DEFAULT_DISABLED);
        }

        public static LiveMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new LiveMessages(
                    section.getString("no-permission"),
                    section.getString("disabled"));
        }
    }

        public record UsageMessages(
            String base,
            String live,
            String sell,
            String order,
            String cancel,
            String history,
            String claim,
            String search) {

        private static final String DEFAULT_BASE = "&eUsage: /{label} - Browse active auctions";
        private static final String DEFAULT_LIVE = "&eUsage: /{label} live - View upcoming live auctions";
        private static final String DEFAULT_SELL = "&eUsage: /{label} sell [price] [duration]";
        private static final String DEFAULT_ORDER = "&eUsage: /{label} order <price> <amount> [duration]";
        private static final String DEFAULT_CANCEL = "&eUsage: /{label} cancel [id]";
        private static final String DEFAULT_HISTORY = "&eUsage: /{label} history [buy|sell]";
        private static final String DEFAULT_CLAIM = "&eUsage: /{label} claim";
        private static final String DEFAULT_SEARCH = "&eUsage: /{label} search <query> [page]";

        public UsageMessages {
            base = normalize(base, DEFAULT_BASE);
            live = normalize(live, DEFAULT_LIVE);
            sell = normalize(sell, DEFAULT_SELL);
            order = normalize(order, DEFAULT_ORDER);
            cancel = normalize(cancel, DEFAULT_CANCEL);
            history = normalize(history, DEFAULT_HISTORY);
            claim = normalize(claim, DEFAULT_CLAIM);
            search = normalize(search, DEFAULT_SEARCH);
        }

        public static UsageMessages defaults() {
            return new UsageMessages(DEFAULT_BASE, DEFAULT_LIVE, DEFAULT_SELL, DEFAULT_ORDER, DEFAULT_CANCEL,
                DEFAULT_HISTORY, DEFAULT_CLAIM, DEFAULT_SEARCH);
        }

        public static UsageMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new UsageMessages(
                    section.getString("base"),
                    section.getString("live"),
                    section.getString("sell"),
                    section.getString("order"),
                    section.getString("cancel"),
                    section.getString("history"),
                    section.getString("claim"),
                    section.getString("search")
            );
        }
        public String search() {
            return search;
        }
    }

    public record CancelMessages(
            String nothingToCancel,
            String listingsHeader,
            String listingEntry,
            String ordersHeader,
            String orderEntry,
            String cancelHint) {

        private static final String DEFAULT_NOTHING_TO_CANCEL = "&eYou have no active auction listings or buy orders to cancel.";
        private static final String DEFAULT_LISTINGS_HEADER = "&6Your Active Listings:&7";
        private static final String DEFAULT_LISTING_ENTRY =
                "&7 - &b{id}&7: &b{item}&7 for &6{price}&7 (expires &e{expiry}&7)";
        private static final String DEFAULT_ORDERS_HEADER = "&6Your Active Buy Orders:&7";
        private static final String DEFAULT_ORDER_ENTRY =
                "&7 - &b{id}&7: &b{item}&7 offering &6{price}&7 (expires &e{expiry}&7)";
        private static final String DEFAULT_CANCEL_HINT =
                "&7Use &e/auction cancel <id>&7 to cancel a listing or buy order.";

        public CancelMessages {
            nothingToCancel = normalize(nothingToCancel, DEFAULT_NOTHING_TO_CANCEL);
            listingsHeader = normalize(listingsHeader, DEFAULT_LISTINGS_HEADER);
            listingEntry = normalize(listingEntry, DEFAULT_LISTING_ENTRY);
            ordersHeader = normalize(ordersHeader, DEFAULT_ORDERS_HEADER);
            orderEntry = normalize(orderEntry, DEFAULT_ORDER_ENTRY);
            cancelHint = normalize(cancelHint, DEFAULT_CANCEL_HINT);
        }

        public static CancelMessages defaults() {
            return new CancelMessages(DEFAULT_NOTHING_TO_CANCEL, DEFAULT_LISTINGS_HEADER, DEFAULT_LISTING_ENTRY,
                    DEFAULT_ORDERS_HEADER, DEFAULT_ORDER_ENTRY, DEFAULT_CANCEL_HINT);
        }

        public static CancelMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new CancelMessages(
                    section.getString("nothing-to-cancel"),
                    section.getString("listings-header"),
                    section.getString("listing-entry"),
                    section.getString("orders-header"),
                    section.getString("order-entry"),
                    section.getString("cancel-hint"));
        }
    }

    public record HistoryMessages(
            String noPermission,
            String unknownFilter,
            String noPurchases,
            String noSales,
            String noHistory,
            String headingPurchases,
            String headingSales,
            String headingActivity,
            String entry,
            String truncated,
            String actionBought,
            String actionSold,
            String counterpartFrom,
            String counterpartTo) {

        private static final String DEFAULT_NO_PERMISSION = "&cYou do not have permission to view auction history.";
        private static final String DEFAULT_UNKNOWN_FILTER = "&cUnknown history filter. Use buy or sell.";
        private static final String DEFAULT_NO_PURCHASES = "&eYou have no recent auction purchases.";
        private static final String DEFAULT_NO_SALES = "&eYou have no recent auction sales.";
        private static final String DEFAULT_NO_HISTORY = "&eYou have no recorded auction history yet.";
        private static final String DEFAULT_HEADING_PURCHASES = "Recent Auction Purchases";
        private static final String DEFAULT_HEADING_SALES = "Recent Auction Sales";
        private static final String DEFAULT_HEADING_ACTIVITY = "Recent Auction Activity";
        private static final String DEFAULT_ENTRY =
                "&7 - &a{action}&b {item}&7 {counterpart-preposition} &b{counterpart}&7 for &6{price}&7 on &e{date}";
        private static final String DEFAULT_TRUNCATED =
                "&7Showing {displayed} of {total} entries. Use the buy or sell filters to narrow results.";
        private static final String DEFAULT_ACTION_BOUGHT = "Bought";
        private static final String DEFAULT_ACTION_SOLD = "Sold";
        private static final String DEFAULT_COUNTERPART_FROM = "from";
        private static final String DEFAULT_COUNTERPART_TO = "to";

        public HistoryMessages {
            noPermission = normalize(noPermission, DEFAULT_NO_PERMISSION);
            unknownFilter = normalize(unknownFilter, DEFAULT_UNKNOWN_FILTER);
            noPurchases = normalize(noPurchases, DEFAULT_NO_PURCHASES);
            noSales = normalize(noSales, DEFAULT_NO_SALES);
            noHistory = normalize(noHistory, DEFAULT_NO_HISTORY);
            headingPurchases = normalize(headingPurchases, DEFAULT_HEADING_PURCHASES);
            headingSales = normalize(headingSales, DEFAULT_HEADING_SALES);
            headingActivity = normalize(headingActivity, DEFAULT_HEADING_ACTIVITY);
            entry = normalize(entry, DEFAULT_ENTRY);
            truncated = normalize(truncated, DEFAULT_TRUNCATED);
            actionBought = normalize(actionBought, DEFAULT_ACTION_BOUGHT);
            actionSold = normalize(actionSold, DEFAULT_ACTION_SOLD);
            counterpartFrom = normalize(counterpartFrom, DEFAULT_COUNTERPART_FROM);
            counterpartTo = normalize(counterpartTo, DEFAULT_COUNTERPART_TO);
        }

        public static HistoryMessages defaults() {
            return new HistoryMessages(DEFAULT_NO_PERMISSION, DEFAULT_UNKNOWN_FILTER, DEFAULT_NO_PURCHASES,
                    DEFAULT_NO_SALES, DEFAULT_NO_HISTORY, DEFAULT_HEADING_PURCHASES, DEFAULT_HEADING_SALES,
                    DEFAULT_HEADING_ACTIVITY, DEFAULT_ENTRY, DEFAULT_TRUNCATED, DEFAULT_ACTION_BOUGHT,
                    DEFAULT_ACTION_SOLD, DEFAULT_COUNTERPART_FROM, DEFAULT_COUNTERPART_TO);
        }

        public static HistoryMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new HistoryMessages(
                    section.getString("no-permission"),
                    section.getString("unknown-filter"),
                    section.getString("no-purchases"),
                    section.getString("no-sales"),
                    section.getString("no-history"),
                    section.getString("heading-purchases"),
                    section.getString("heading-sales"),
                    section.getString("heading-activity"),
                    section.getString("entry"),
                    section.getString("truncated"),
                    section.getString("action-bought"),
                    section.getString("action-sold"),
                    section.getString("counterpart-from"),
                    section.getString("counterpart-to"));
        }
    }

    public record HologramMessages(
            String playersOnly,
            String disabled,
            String unknownType,
            String placementRange,
            String placementFailed,
            String placementSuccess,
            String cleared,
            String noneFound,
            String usage,
            String typesHeading,
            String typesEntry) {

        private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may manage auction holograms.";
        private static final String DEFAULT_DISABLED = "&cAuction holograms are disabled in the configuration.";
        private static final String DEFAULT_UNKNOWN_TYPE = "&cUnknown hologram type '{type}'.";
        private static final String DEFAULT_PLACEMENT_RANGE =
                "&cLook at a block within {range} blocks to place the hologram.";
        private static final String DEFAULT_PLACEMENT_FAILED =
                "&cFailed to place the auction hologram. Ensure the chunk is loaded.";
        private static final String DEFAULT_PLACEMENT_SUCCESS =
                "&aPlaced an auction hologram for &6{display}&a.";
        private static final String DEFAULT_CLEARED = "&eRemoved the nearest auction hologram.";
        private static final String DEFAULT_NONE_FOUND = "&cNo auction hologram found nearby.";
        private static final String DEFAULT_USAGE = "&eUsage: /{label} <type|clear>";
        private static final String DEFAULT_TYPES_HEADING = "&6Available hologram types:";
        private static final String DEFAULT_TYPES_ENTRY = "&7- &6{name}&7 ({display})";

        public HologramMessages {
            playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
            disabled = normalize(disabled, DEFAULT_DISABLED);
            unknownType = normalize(unknownType, DEFAULT_UNKNOWN_TYPE);
            placementRange = normalize(placementRange, DEFAULT_PLACEMENT_RANGE);
            placementFailed = normalize(placementFailed, DEFAULT_PLACEMENT_FAILED);
            placementSuccess = normalize(placementSuccess, DEFAULT_PLACEMENT_SUCCESS);
            cleared = normalize(cleared, DEFAULT_CLEARED);
            noneFound = normalize(noneFound, DEFAULT_NONE_FOUND);
            usage = normalize(usage, DEFAULT_USAGE);
            typesHeading = normalize(typesHeading, DEFAULT_TYPES_HEADING);
            typesEntry = normalize(typesEntry, DEFAULT_TYPES_ENTRY);
        }

        public static HologramMessages defaults() {
            return new HologramMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_DISABLED, DEFAULT_UNKNOWN_TYPE,
                    DEFAULT_PLACEMENT_RANGE, DEFAULT_PLACEMENT_FAILED, DEFAULT_PLACEMENT_SUCCESS, DEFAULT_CLEARED,
                    DEFAULT_NONE_FOUND, DEFAULT_USAGE, DEFAULT_TYPES_HEADING, DEFAULT_TYPES_ENTRY);
        }

        public static HologramMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new HologramMessages(
                    section.getString("players-only"),
                    section.getString("disabled"),
                    section.getString("unknown-type"),
                    section.getString("placement-range"),
                    section.getString("placement-failed"),
                    section.getString("placement-success"),
                    section.getString("cleared"),
                    section.getString("none-found"),
                    section.getString("usage"),
                    section.getString("types-heading"),
                    section.getString("types-entry"));
        }
    }
}
