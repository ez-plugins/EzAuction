package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration container for chat messages emitted by the EzAuction GUIs.
 */
public final class AuctionMessageConfiguration {

    private final BrowserMessages browser;
    private final SellMessages sell;
    private final OrderMessages order;
    private final LiveMessages live;

    public AuctionMessageConfiguration(BrowserMessages browser, SellMessages sell, OrderMessages order,
            LiveMessages live) {
        this.browser = browser != null ? browser : BrowserMessages.defaults();
        this.sell = sell != null ? sell : SellMessages.defaults();
        this.order = order != null ? order : OrderMessages.defaults();
        this.live = live != null ? live : LiveMessages.defaults();
    }

    public BrowserMessages browser() {
        return browser;
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

    public static AuctionMessageConfiguration defaults() {
        return new AuctionMessageConfiguration(BrowserMessages.defaults(), SellMessages.defaults(),
                OrderMessages.defaults(), LiveMessages.defaults());
    }

    public static AuctionMessageConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        BrowserMessages browserMessages = BrowserMessages.from(section.getConfigurationSection("browser"));
        SellMessages sellMessages = SellMessages.from(section.getConfigurationSection("sell"));
        OrderMessages orderMessages = OrderMessages.from(section.getConfigurationSection("order"));
        LiveMessages liveMessages = LiveMessages.from(section.getConfigurationSection("live"));
        return new AuctionMessageConfiguration(browserMessages, sellMessages, orderMessages, liveMessages);
    }

    public record BrowserMessages(
            String listingNotFound,
            String listingUnavailable,
            String orderNotFound,
            String orderUnavailable,
            String cancelListingNoPermission,
            String buyNoPermission,
            String ownListingPurchase,
            String fulfillNoPermission,
            String fulfillOwnOrder,
            String fulfillInsufficientItems,
            String searchCleared,
            String searchAlreadyClear,
            String searchPrompt,
            String searchClearPrompt,
            String searchCancelled,
            String searchInvalid,
            String searchApplied,
            String sortListings,
            String sortOrders,
            String shulkerPreviewHint,
            String shulkerPreviewTitle) {

        private static final String DEFAULT_LISTING_NOT_FOUND = "&cThat listing could not be found.";
        private static final String DEFAULT_LISTING_UNAVAILABLE = "&cThat auction listing is no longer available.";
        private static final String DEFAULT_ORDER_NOT_FOUND = "&cThat buy order could not be found.";
        private static final String DEFAULT_ORDER_UNAVAILABLE = "&cThat buy order is no longer available.";
        private static final String DEFAULT_CANCEL_LISTING_NO_PERMISSION =
                "&cYou do not have permission to cancel auction listings.";
        private static final String DEFAULT_BUY_NO_PERMISSION =
                "&cYou do not have permission to purchase auction listings.";
        private static final String DEFAULT_OWN_LISTING_PURCHASE = "&cYou cannot purchase your own listing.";
        private static final String DEFAULT_FULFILL_NO_PERMISSION =
                "&cYou do not have permission to fulfill buy orders.";
        private static final String DEFAULT_FULFILL_OWN_ORDER = "&cYou cannot fulfill your own buy order.";
        private static final String DEFAULT_FULFILL_INSUFFICIENT_ITEMS =
                "&cYou do not have enough items to fulfill this order.";
        private static final String DEFAULT_SEARCH_CLEARED = "&aCleared auction house search.";
        private static final String DEFAULT_SEARCH_ALREADY_CLEAR = "&7Search filter already clear.";
        private static final String DEFAULT_SEARCH_PROMPT = "&eEnter a search query in chat.&7 (type 'cancel' to abort)";
        private static final String DEFAULT_SEARCH_CLEAR_PROMPT = "&7Type 'clear' to remove the search filter.";
        private static final String DEFAULT_SEARCH_CANCELLED = "&7Cancelled search entry.";
        private static final String DEFAULT_SEARCH_INVALID = "&cPlease enter a valid search query.";
        private static final String DEFAULT_SEARCH_APPLIED = "&aSearching for \"&b{query}&a\".";
        private static final String DEFAULT_SORT_LISTINGS = "&aSorting listings by &b{sort}&a.";
        private static final String DEFAULT_SORT_ORDERS = "&aSorting orders by &b{sort}&a.";
        private static final String DEFAULT_SHULKER_PREVIEW_HINT = "&dShift-click to preview contents.";
        private static final String DEFAULT_SHULKER_PREVIEW_TITLE = "&5Shulker Contents";

        public BrowserMessages {
            listingNotFound = normalize(listingNotFound, DEFAULT_LISTING_NOT_FOUND);
            listingUnavailable = normalize(listingUnavailable, DEFAULT_LISTING_UNAVAILABLE);
            orderNotFound = normalize(orderNotFound, DEFAULT_ORDER_NOT_FOUND);
            orderUnavailable = normalize(orderUnavailable, DEFAULT_ORDER_UNAVAILABLE);
            cancelListingNoPermission = normalize(cancelListingNoPermission, DEFAULT_CANCEL_LISTING_NO_PERMISSION);
            buyNoPermission = normalize(buyNoPermission, DEFAULT_BUY_NO_PERMISSION);
            ownListingPurchase = normalize(ownListingPurchase, DEFAULT_OWN_LISTING_PURCHASE);
            fulfillNoPermission = normalize(fulfillNoPermission, DEFAULT_FULFILL_NO_PERMISSION);
            fulfillOwnOrder = normalize(fulfillOwnOrder, DEFAULT_FULFILL_OWN_ORDER);
            fulfillInsufficientItems = normalize(fulfillInsufficientItems, DEFAULT_FULFILL_INSUFFICIENT_ITEMS);
            searchCleared = normalize(searchCleared, DEFAULT_SEARCH_CLEARED);
            searchAlreadyClear = normalize(searchAlreadyClear, DEFAULT_SEARCH_ALREADY_CLEAR);
            searchPrompt = normalize(searchPrompt, DEFAULT_SEARCH_PROMPT);
            searchClearPrompt = normalize(searchClearPrompt, DEFAULT_SEARCH_CLEAR_PROMPT);
            searchCancelled = normalize(searchCancelled, DEFAULT_SEARCH_CANCELLED);
            searchInvalid = normalize(searchInvalid, DEFAULT_SEARCH_INVALID);
            searchApplied = normalize(searchApplied, DEFAULT_SEARCH_APPLIED);
            sortListings = normalize(sortListings, DEFAULT_SORT_LISTINGS);
            sortOrders = normalize(sortOrders, DEFAULT_SORT_ORDERS);
            shulkerPreviewHint = normalize(shulkerPreviewHint, DEFAULT_SHULKER_PREVIEW_HINT);
            shulkerPreviewTitle = normalize(shulkerPreviewTitle, DEFAULT_SHULKER_PREVIEW_TITLE);
        }

        public static BrowserMessages defaults() {
            return new BrowserMessages(DEFAULT_LISTING_NOT_FOUND, DEFAULT_LISTING_UNAVAILABLE, DEFAULT_ORDER_NOT_FOUND,
                    DEFAULT_ORDER_UNAVAILABLE, DEFAULT_CANCEL_LISTING_NO_PERMISSION, DEFAULT_BUY_NO_PERMISSION,
                    DEFAULT_OWN_LISTING_PURCHASE, DEFAULT_FULFILL_NO_PERMISSION, DEFAULT_FULFILL_OWN_ORDER,
                    DEFAULT_FULFILL_INSUFFICIENT_ITEMS, DEFAULT_SEARCH_CLEARED, DEFAULT_SEARCH_ALREADY_CLEAR,
                    DEFAULT_SEARCH_PROMPT, DEFAULT_SEARCH_CLEAR_PROMPT, DEFAULT_SEARCH_CANCELLED, DEFAULT_SEARCH_INVALID,
                    DEFAULT_SEARCH_APPLIED, DEFAULT_SORT_LISTINGS, DEFAULT_SORT_ORDERS, DEFAULT_SHULKER_PREVIEW_HINT,
                    DEFAULT_SHULKER_PREVIEW_TITLE);
        }

        public static BrowserMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new BrowserMessages(
                    section.getString("listing-not-found"),
                    section.getString("listing-unavailable"),
                    section.getString("order-not-found"),
                    section.getString("order-unavailable"),
                    section.getString("cancel-no-permission"),
                    section.getString("buy-no-permission"),
                    section.getString("own-listing"),
                    section.getString("fulfill-no-permission"),
                    section.getString("fulfill-own-order"),
                    section.getString("fulfill-not-enough-items"),
                    section.getString("search-cleared"),
                    section.getString("search-clear-already"),
                    section.getString("search-prompt"),
                    section.getString("search-clear-prompt"),
                    section.getString("search-cancelled"),
                    section.getString("search-invalid"),
                    section.getString("search-applied"),
                    section.getString("sort-listings"),
                    section.getString("sort-orders"),
                    section.getString("shulker-preview-hint"),
                    section.getString("shulker-preview-title"));
        }
    }

    public record SellMessages(
            String itemRequired,
            String pricePrompt,
            String priceMinimum,
            String priceEntryCancelled,
            String priceInvalidNumber,
            String priceMustBePositive,
            String priceUpdated) {

        private static final String DEFAULT_ITEM_REQUIRED =
                "&cYou must hold the item you want to list in your main hand.";
        private static final String DEFAULT_PRICE_PROMPT = "&eEnter the listing price in chat.&7 (type 'cancel' to abort)";
        private static final String DEFAULT_PRICE_MINIMUM = "&cListing price must be at least &6{minimum}&c.";
        private static final String DEFAULT_PRICE_ENTRY_CANCELLED = "&7Cancelled price entry.";
        private static final String DEFAULT_PRICE_INVALID_NUMBER = "&cPlease enter a valid number.";
        private static final String DEFAULT_PRICE_MUST_BE_POSITIVE = "&cPrice must be positive.";
        private static final String DEFAULT_PRICE_UPDATED = "&aSet listing price to &6{price}&a.";

        public SellMessages {
            itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
            pricePrompt = normalize(pricePrompt, DEFAULT_PRICE_PROMPT);
            priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
            priceEntryCancelled = normalize(priceEntryCancelled, DEFAULT_PRICE_ENTRY_CANCELLED);
            priceInvalidNumber = normalize(priceInvalidNumber, DEFAULT_PRICE_INVALID_NUMBER);
            priceMustBePositive = normalize(priceMustBePositive, DEFAULT_PRICE_MUST_BE_POSITIVE);
            priceUpdated = normalize(priceUpdated, DEFAULT_PRICE_UPDATED);
        }

        public static SellMessages defaults() {
            return new SellMessages(DEFAULT_ITEM_REQUIRED, DEFAULT_PRICE_PROMPT, DEFAULT_PRICE_MINIMUM,
                    DEFAULT_PRICE_ENTRY_CANCELLED, DEFAULT_PRICE_INVALID_NUMBER, DEFAULT_PRICE_MUST_BE_POSITIVE,
                    DEFAULT_PRICE_UPDATED);
        }

        public static SellMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new SellMessages(
                    section.getString("item-required"),
                    section.getString("price-prompt"),
                    section.getString("price-minimum"),
                    section.getString("price-entry-cancelled"),
                    section.getString("price-invalid-number"),
                    section.getString("price-positive"),
                    section.getString("price-updated"));
        }
    }

    public record OrderMessages(
            String noPermission,
            String itemRequired,
            String pricePrompt,
            String quantityPrompt,
            String quantityMinimum,
            String priceMinimum,
            String totalMustBePositive,
            String entryCancelled,
            String invalidNumber,
            String priceMustBePositive,
            String priceUpdated,
            String wholeNumberRequired,
            String quantityUpdated) {

        private static final String DEFAULT_NO_PERMISSION =
                "&cYou do not have permission to create buy orders.";
        private static final String DEFAULT_ITEM_REQUIRED =
                "&cYou must hold the item you want to request in your main hand.";
        private static final String DEFAULT_PRICE_PROMPT = "&eEnter the price per item in chat.&7 (type 'cancel' to abort)";
        private static final String DEFAULT_QUANTITY_PROMPT =
                "&eEnter the desired quantity in chat.&7 (type 'cancel' to abort)";
        private static final String DEFAULT_QUANTITY_MINIMUM = "&cQuantity must be at least 1.";
        private static final String DEFAULT_PRICE_MINIMUM = "&cPrice per item must be at least &6{minimum}&c.";
        private static final String DEFAULT_TOTAL_MUST_BE_POSITIVE = "&cTotal offer must be positive.";
        private static final String DEFAULT_ENTRY_CANCELLED = "&7Cancelled entry.";
        private static final String DEFAULT_INVALID_NUMBER = "&cPlease enter a valid number.";
        private static final String DEFAULT_PRICE_MUST_BE_POSITIVE = "&cPrice must be positive.";
        private static final String DEFAULT_PRICE_UPDATED = "&aSet price per item to &6{price}&a.";
        private static final String DEFAULT_WHOLE_NUMBER_REQUIRED = "&cPlease enter a whole number.";
        private static final String DEFAULT_QUANTITY_UPDATED = "&aSet requested quantity to &b{quantity}&a.";

        public OrderMessages {
            noPermission = normalize(noPermission, DEFAULT_NO_PERMISSION);
            itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
            pricePrompt = normalize(pricePrompt, DEFAULT_PRICE_PROMPT);
            quantityPrompt = normalize(quantityPrompt, DEFAULT_QUANTITY_PROMPT);
            quantityMinimum = normalize(quantityMinimum, DEFAULT_QUANTITY_MINIMUM);
            priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
            totalMustBePositive = normalize(totalMustBePositive, DEFAULT_TOTAL_MUST_BE_POSITIVE);
            entryCancelled = normalize(entryCancelled, DEFAULT_ENTRY_CANCELLED);
            invalidNumber = normalize(invalidNumber, DEFAULT_INVALID_NUMBER);
            priceMustBePositive = normalize(priceMustBePositive, DEFAULT_PRICE_MUST_BE_POSITIVE);
            priceUpdated = normalize(priceUpdated, DEFAULT_PRICE_UPDATED);
            wholeNumberRequired = normalize(wholeNumberRequired, DEFAULT_WHOLE_NUMBER_REQUIRED);
            quantityUpdated = normalize(quantityUpdated, DEFAULT_QUANTITY_UPDATED);
        }

        public static OrderMessages defaults() {
            return new OrderMessages(DEFAULT_NO_PERMISSION, DEFAULT_ITEM_REQUIRED, DEFAULT_PRICE_PROMPT,
                    DEFAULT_QUANTITY_PROMPT, DEFAULT_QUANTITY_MINIMUM, DEFAULT_PRICE_MINIMUM,
                    DEFAULT_TOTAL_MUST_BE_POSITIVE, DEFAULT_ENTRY_CANCELLED, DEFAULT_INVALID_NUMBER,
                    DEFAULT_PRICE_MUST_BE_POSITIVE, DEFAULT_PRICE_UPDATED, DEFAULT_WHOLE_NUMBER_REQUIRED,
                    DEFAULT_QUANTITY_UPDATED);
        }

        public static OrderMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new OrderMessages(
                    section.getString("no-permission"),
                    section.getString("item-required"),
                    section.getString("price-prompt"),
                    section.getString("quantity-prompt"),
                    section.getString("quantity-minimum"),
                    section.getString("price-minimum"),
                    section.getString("total-positive"),
                    section.getString("entry-cancelled"),
                    section.getString("invalid-number"),
                    section.getString("price-positive"),
                    section.getString("price-updated"),
                    section.getString("whole-number"),
                    section.getString("quantity-updated"));
        }
    }

    public record LiveMessages(String disabled, String queueInstant) {

        private static final String DEFAULT_DISABLED = "&cLive auctions are currently disabled.";
        private static final String DEFAULT_QUEUE_INSTANT =
                "&eLive auction announcements are broadcast instantly; no queue is available.";

        public LiveMessages {
            disabled = normalize(disabled, DEFAULT_DISABLED);
            queueInstant = normalize(queueInstant, DEFAULT_QUEUE_INSTANT);
        }

        public static LiveMessages defaults() {
            return new LiveMessages(DEFAULT_DISABLED, DEFAULT_QUEUE_INSTANT);
        }

        public static LiveMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new LiveMessages(
                    section.getString("disabled"),
                    section.getString("queue-instant"));
        }
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
