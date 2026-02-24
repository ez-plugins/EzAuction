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
    private final ActivityMessages activity;

    public AuctionMessageConfiguration(BrowserMessages browser, SellMessages sell, OrderMessages order,
            LiveMessages live, ActivityMessages activity) {
        this.browser = browser != null ? browser : BrowserMessages.defaults();
        this.sell = sell != null ? sell : SellMessages.defaults();
        this.order = order != null ? order : OrderMessages.defaults();
        this.live = live != null ? live : LiveMessages.defaults();
        this.activity = activity != null ? activity : ActivityMessages.defaults();
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

    public ActivityMessages activity() {
        return activity;
    }

    public static AuctionMessageConfiguration defaults() {
        return new AuctionMessageConfiguration(BrowserMessages.defaults(), SellMessages.defaults(),
            OrderMessages.defaults(), LiveMessages.defaults(), ActivityMessages.defaults());
    }

    public static AuctionMessageConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        BrowserMessages browserMessages = BrowserMessages.from(section.getConfigurationSection("browser"));
        SellMessages sellMessages = SellMessages.from(section.getConfigurationSection("sell"));
        OrderMessages orderMessages = OrderMessages.from(section.getConfigurationSection("order"));
        LiveMessages liveMessages = LiveMessages.from(section.getConfigurationSection("live"));
        ActivityMessages activityMessages = ActivityMessages.from(section.getConfigurationSection("activity"));
        return new AuctionMessageConfiguration(browserMessages, sellMessages, orderMessages, liveMessages, activityMessages);
    }

    public record ActivityMessages(
            String titlePrefix,
            String tabListingsLabel,
            String tabListingsDesc,
            String tabOrdersLabel,
            String tabOrdersDesc,
            String tabReturnsLabel,
            String tabReturnsDesc,
            String tabHistoryLabel,
            String tabHistoryDesc,
            String tabActiveSuffix,
            String tabClickText,
            String backLabel,
            String backLore,
            String closeLabel,
            String closeLore,
            String claimButtonLabel,
            String claimButtonPendingLine,
            String claimButtonNoPendingLine,
            String claimPromptTitle,
            String noListings,
            String noOrders,
            String noReturns,
            String noHistory,
            String claimPromptLine1,
            String claimPromptLine2,
            String emptyIndicatorTitle,
            String invalidItemTitle,
            String labelPrice,
            String labelQuantity,
            String labelExpires,
            String labelPricePerItem,
            String labelQuantityWanted,
            String labelTotal,
            String labelType,
            String labelTime,
            String labelTransactionId,
            String listingTag,
            String orderTag) {

        private static final String DEFAULT_TITLE_PREFIX = "My Activity";
        private static final String DEFAULT_TAB_LISTINGS_LABEL = "My Listings";
        private static final String DEFAULT_TAB_LISTINGS_DESC = "View your active listings";
        private static final String DEFAULT_TAB_ORDERS_LABEL = "My Orders";
        private static final String DEFAULT_TAB_ORDERS_DESC = "View your buy orders";
        private static final String DEFAULT_TAB_RETURNS_LABEL = "Pending Returns";
        private static final String DEFAULT_TAB_RETURNS_DESC = "Items waiting to be claimed";
        private static final String DEFAULT_TAB_HISTORY_LABEL = "Recent History";
        private static final String DEFAULT_TAB_HISTORY_DESC = "Your transaction history";
        private static final String DEFAULT_TAB_ACTIVE_SUFFIX = "» Currently viewing";
        private static final String DEFAULT_TAB_CLICK_TEXT = "Click to view";
        private static final String DEFAULT_BACK_LABEL = "Back";
        private static final String DEFAULT_BACK_LORE = "Return to auction browser";
        private static final String DEFAULT_CLOSE_LABEL = "Close";
        private static final String DEFAULT_CLOSE_LORE = "Close this menu";
        private static final String DEFAULT_CLAIM_BUTTON_LABEL = "Claim Returns";
        private static final String DEFAULT_CLAIM_BUTTON_PENDING = "Pending: {count}";
        private static final String DEFAULT_CLAIM_BUTTON_NONE = "No pending returns";
        private static final String DEFAULT_CLAIM_PROMPT_TITLE = "Claim Pending Returns";
        private static final String DEFAULT_NO_LISTINGS = "You have no active listings.";
        private static final String DEFAULT_NO_ORDERS = "You have no active buy orders.";
        private static final String DEFAULT_NO_RETURNS = "You have no pending returns.";
        private static final String DEFAULT_NO_HISTORY = "No transaction history.";
        private static final String DEFAULT_CLAIM_LINE_1 = "You have {count} items waiting.";
        private static final String DEFAULT_CLAIM_LINE_2 = "Click the claim button below to retrieve them.";
        private static final String DEFAULT_EMPTY_TITLE = "Nothing to show";
        private static final String DEFAULT_INVALID_ITEM = "Invalid Item";
        private static final String DEFAULT_LABEL_PRICE = "Price: ";
        private static final String DEFAULT_LABEL_QUANTITY = "Quantity: ";
        private static final String DEFAULT_LABEL_EXPIRES = "Expires: ";
        private static final String DEFAULT_LABEL_PRICE_PER_ITEM = "Price per item: ";
        private static final String DEFAULT_LABEL_QUANTITY_WANTED = "Quantity wanted: ";
        private static final String DEFAULT_LABEL_TOTAL = "Total: ";
        private static final String DEFAULT_LABEL_TYPE = "Type: ";
        private static final String DEFAULT_LABEL_TIME = "Time: ";
        private static final String DEFAULT_LABEL_TX_ID = "Transaction ID: ";
        private static final String DEFAULT_LISTING_TAG = "Your active listing";
        private static final String DEFAULT_ORDER_TAG = "Your buy order";

        public ActivityMessages {
            titlePrefix = normalize(titlePrefix, DEFAULT_TITLE_PREFIX);
            tabListingsLabel = normalize(tabListingsLabel, DEFAULT_TAB_LISTINGS_LABEL);
            tabListingsDesc = normalize(tabListingsDesc, DEFAULT_TAB_LISTINGS_DESC);
            tabOrdersLabel = normalize(tabOrdersLabel, DEFAULT_TAB_ORDERS_LABEL);
            tabOrdersDesc = normalize(tabOrdersDesc, DEFAULT_TAB_ORDERS_DESC);
            tabReturnsLabel = normalize(tabReturnsLabel, DEFAULT_TAB_RETURNS_LABEL);
            tabReturnsDesc = normalize(tabReturnsDesc, DEFAULT_TAB_RETURNS_DESC);
            tabHistoryLabel = normalize(tabHistoryLabel, DEFAULT_TAB_HISTORY_LABEL);
            tabHistoryDesc = normalize(tabHistoryDesc, DEFAULT_TAB_HISTORY_DESC);
            tabActiveSuffix = normalize(tabActiveSuffix, DEFAULT_TAB_ACTIVE_SUFFIX);
            tabClickText = normalize(tabClickText, DEFAULT_TAB_CLICK_TEXT);
            backLabel = normalize(backLabel, DEFAULT_BACK_LABEL);
            backLore = normalize(backLore, DEFAULT_BACK_LORE);
            closeLabel = normalize(closeLabel, DEFAULT_CLOSE_LABEL);
            closeLore = normalize(closeLore, DEFAULT_CLOSE_LORE);
            claimButtonLabel = normalize(claimButtonLabel, DEFAULT_CLAIM_BUTTON_LABEL);
            claimButtonPendingLine = normalize(claimButtonPendingLine, DEFAULT_CLAIM_BUTTON_PENDING);
            claimButtonNoPendingLine = normalize(claimButtonNoPendingLine, DEFAULT_CLAIM_BUTTON_NONE);
            claimPromptTitle = normalize(claimPromptTitle, DEFAULT_CLAIM_PROMPT_TITLE);
            noListings = normalize(noListings, DEFAULT_NO_LISTINGS);
            noOrders = normalize(noOrders, DEFAULT_NO_ORDERS);
            noReturns = normalize(noReturns, DEFAULT_NO_RETURNS);
            noHistory = normalize(noHistory, DEFAULT_NO_HISTORY);
            claimPromptLine1 = normalize(claimPromptLine1, DEFAULT_CLAIM_LINE_1);
            claimPromptLine2 = normalize(claimPromptLine2, DEFAULT_CLAIM_LINE_2);
            emptyIndicatorTitle = normalize(emptyIndicatorTitle, DEFAULT_EMPTY_TITLE);
            invalidItemTitle = normalize(invalidItemTitle, DEFAULT_INVALID_ITEM);
            labelPrice = normalize(labelPrice, DEFAULT_LABEL_PRICE);
            labelQuantity = normalize(labelQuantity, DEFAULT_LABEL_QUANTITY);
            labelExpires = normalize(labelExpires, DEFAULT_LABEL_EXPIRES);
            labelPricePerItem = normalize(labelPricePerItem, DEFAULT_LABEL_PRICE_PER_ITEM);
            labelQuantityWanted = normalize(labelQuantityWanted, DEFAULT_LABEL_QUANTITY_WANTED);
            labelTotal = normalize(labelTotal, DEFAULT_LABEL_TOTAL);
            labelType = normalize(labelType, DEFAULT_LABEL_TYPE);
            labelTime = normalize(labelTime, DEFAULT_LABEL_TIME);
            labelTransactionId = normalize(labelTransactionId, DEFAULT_LABEL_TX_ID);
            listingTag = normalize(listingTag, DEFAULT_LISTING_TAG);
            orderTag = normalize(orderTag, DEFAULT_ORDER_TAG);
        }

        public static ActivityMessages defaults() {
            return new ActivityMessages(
                    DEFAULT_TITLE_PREFIX,
                    DEFAULT_TAB_LISTINGS_LABEL,
                    DEFAULT_TAB_LISTINGS_DESC,
                    DEFAULT_TAB_ORDERS_LABEL,
                    DEFAULT_TAB_ORDERS_DESC,
                    DEFAULT_TAB_RETURNS_LABEL,
                    DEFAULT_TAB_RETURNS_DESC,
                    DEFAULT_TAB_HISTORY_LABEL,
                    DEFAULT_TAB_HISTORY_DESC,
                    DEFAULT_TAB_ACTIVE_SUFFIX,
                    DEFAULT_TAB_CLICK_TEXT,
                    DEFAULT_BACK_LABEL,
                    DEFAULT_BACK_LORE,
                    DEFAULT_CLOSE_LABEL,
                    DEFAULT_CLOSE_LORE,
                    DEFAULT_CLAIM_BUTTON_LABEL,
                    DEFAULT_CLAIM_BUTTON_PENDING,
                    DEFAULT_CLAIM_BUTTON_NONE,
                    DEFAULT_CLAIM_PROMPT_TITLE,
                    DEFAULT_NO_LISTINGS,
                    DEFAULT_NO_ORDERS,
                    DEFAULT_NO_RETURNS,
                    DEFAULT_NO_HISTORY,
                    DEFAULT_CLAIM_LINE_1,
                    DEFAULT_CLAIM_LINE_2,
                    DEFAULT_EMPTY_TITLE,
                    DEFAULT_INVALID_ITEM,
                    DEFAULT_LABEL_PRICE,
                    DEFAULT_LABEL_QUANTITY,
                    DEFAULT_LABEL_EXPIRES,
                    DEFAULT_LABEL_PRICE_PER_ITEM,
                    DEFAULT_LABEL_QUANTITY_WANTED,
                    DEFAULT_LABEL_TOTAL,
                    DEFAULT_LABEL_TYPE,
                    DEFAULT_LABEL_TIME,
                    DEFAULT_LABEL_TX_ID,
                    DEFAULT_LISTING_TAG,
                    DEFAULT_ORDER_TAG);
        }

        public static ActivityMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new ActivityMessages(
                    section.getString("title-prefix"),
                    section.getString("tab-listings-label"),
                    section.getString("tab-listings-desc"),
                    section.getString("tab-orders-label"),
                    section.getString("tab-orders-desc"),
                    section.getString("tab-returns-label"),
                    section.getString("tab-returns-desc"),
                    section.getString("tab-history-label"),
                    section.getString("tab-history-desc"),
                    section.getString("tab-active-suffix"),
                    section.getString("tab-click-text"),
                    section.getString("back-label"),
                    section.getString("back-lore"),
                    section.getString("close-label"),
                    section.getString("close-lore"),
                    section.getString("claim-button-label"),
                    section.getString("claim-button-pending"),
                    section.getString("claim-button-none"),
                    section.getString("claim-prompt-title"),
                    section.getString("no-listings"),
                    section.getString("no-orders"),
                    section.getString("no-returns"),
                    section.getString("no-history"),
                    section.getString("claim-line-1"),
                    section.getString("claim-line-2"),
                    section.getString("empty-title"),
                    section.getString("invalid-item-title"),
                    section.getString("label-price"),
                    section.getString("label-quantity"),
                    section.getString("label-expires"),
                    section.getString("label-price-per-item"),
                    section.getString("label-quantity-wanted"),
                    section.getString("label-total"),
                    section.getString("label-type"),
                    section.getString("label-time"),
                    section.getString("label-transaction-id"),
                    section.getString("listing-tag"),
                    section.getString("order-tag"));
        }
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
            String shulkerPreviewTitle,
            String previousPageTitle,
            String previousPageLore,
            String nextPageTitle,
            String nextPageLore,
            String listingsViewing,
            String listingsClick,
            String ordersViewing,
            String ordersClick,
            String closeTitle,
            String closeLore,
            String activityTitle,
            String activityLore) {

        // additional UI/button labels
        // (kept after chat messages to preserve backwards-compat ordering in code)

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
        private static final String DEFAULT_PREVIOUS_TITLE = "Previous Page";
        private static final String DEFAULT_PREVIOUS_LORE = "View earlier listings.";
        private static final String DEFAULT_NEXT_TITLE = "Next Page";
        private static final String DEFAULT_NEXT_LORE = "View more listings.";
        private static final String DEFAULT_LISTINGS_VIEWING = "Currently viewing listings.";
        private static final String DEFAULT_LISTINGS_CLICK = "Click to view listings.";
        private static final String DEFAULT_ORDERS_VIEWING = "Currently viewing orders.";
        private static final String DEFAULT_ORDERS_CLICK = "Click to view orders.";
        private static final String DEFAULT_CLOSE_TITLE = "Close";
        private static final String DEFAULT_CLOSE_LORE = "Exit the auction.";
        private static final String DEFAULT_ACTIVITY_TITLE = "Activity";
        private static final String DEFAULT_ACTIVITY_LORE = "Open consolidated activity menu.";

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
            // set defaults for new UI labels if absent
            previousPageTitle = normalize(previousPageTitle, DEFAULT_PREVIOUS_TITLE);
            previousPageLore = normalize(previousPageLore, DEFAULT_PREVIOUS_LORE);
            nextPageTitle = normalize(nextPageTitle, DEFAULT_NEXT_TITLE);
            nextPageLore = normalize(nextPageLore, DEFAULT_NEXT_LORE);
            listingsViewing = normalize(listingsViewing, DEFAULT_LISTINGS_VIEWING);
            listingsClick = normalize(listingsClick, DEFAULT_LISTINGS_CLICK);
            ordersViewing = normalize(ordersViewing, DEFAULT_ORDERS_VIEWING);
            ordersClick = normalize(ordersClick, DEFAULT_ORDERS_CLICK);
            closeTitle = normalize(closeTitle, DEFAULT_CLOSE_TITLE);
            closeLore = normalize(closeLore, DEFAULT_CLOSE_LORE);
            activityTitle = normalize(activityTitle, DEFAULT_ACTIVITY_TITLE);
            activityLore = normalize(activityLore, DEFAULT_ACTIVITY_LORE);
            // Note: preserve existing behavior for callers that only used earlier fields
        }

        public static BrowserMessages defaults() {
            return new BrowserMessages(DEFAULT_LISTING_NOT_FOUND, DEFAULT_LISTING_UNAVAILABLE, DEFAULT_ORDER_NOT_FOUND,
                DEFAULT_ORDER_UNAVAILABLE, DEFAULT_CANCEL_LISTING_NO_PERMISSION, DEFAULT_BUY_NO_PERMISSION,
                DEFAULT_OWN_LISTING_PURCHASE, DEFAULT_FULFILL_NO_PERMISSION, DEFAULT_FULFILL_OWN_ORDER,
                DEFAULT_FULFILL_INSUFFICIENT_ITEMS, DEFAULT_SEARCH_CLEARED, DEFAULT_SEARCH_ALREADY_CLEAR,
                DEFAULT_SEARCH_PROMPT, DEFAULT_SEARCH_CLEAR_PROMPT, DEFAULT_SEARCH_CANCELLED, DEFAULT_SEARCH_INVALID,
                DEFAULT_SEARCH_APPLIED, DEFAULT_SORT_LISTINGS, DEFAULT_SORT_ORDERS, DEFAULT_SHULKER_PREVIEW_HINT,
                DEFAULT_SHULKER_PREVIEW_TITLE,
                DEFAULT_PREVIOUS_TITLE,
                DEFAULT_PREVIOUS_LORE,
                DEFAULT_NEXT_TITLE,
                DEFAULT_NEXT_LORE,
                DEFAULT_LISTINGS_VIEWING,
                DEFAULT_LISTINGS_CLICK,
                DEFAULT_ORDERS_VIEWING,
                DEFAULT_ORDERS_CLICK,
                DEFAULT_CLOSE_TITLE,
                DEFAULT_CLOSE_LORE,
                DEFAULT_ACTIVITY_TITLE,
                DEFAULT_ACTIVITY_LORE);
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
                    section.getString("shulker-preview-title"),
                    section.getString("previous-page-title"),
                    section.getString("previous-page-lore"),
                    section.getString("next-page-title"),
                    section.getString("next-page-lore"),
                    section.getString("listings-viewing"),
                    section.getString("listings-click"),
                    section.getString("orders-viewing"),
                    section.getString("orders-click"),
                    section.getString("close-title"),
                    section.getString("close-lore"),
                    section.getString("activity-title"),
                    section.getString("activity-lore"));
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

    public record LiveMessages(String disabled, String queueInstant, String inventoryTitle, String closeLabel,
            String closeLore, String refreshLabel, String refreshLore, String browseLabel, String browseLore,
            String backLabel, String backLore, String infoTitle, String infoLoreLine1, String infoLoreLine2,
            String emptyQueueTitle, String emptyQueueLore) {

        private static final String DEFAULT_DISABLED = "&cLive auctions are currently disabled.";
        private static final String DEFAULT_QUEUE_INSTANT =
                "&eLive auction announcements are broadcast instantly; no queue is available.";
        private static final String DEFAULT_INVENTORY_TITLE = "Live Auctions";
        private static final String DEFAULT_CLOSE_LABEL = "Close";
        private static final String DEFAULT_CLOSE_LORE = "Exit the menu.";
        private static final String DEFAULT_REFRESH_LABEL = "Refresh";
        private static final String DEFAULT_REFRESH_LORE = "Update the live auction queue.";
        private static final String DEFAULT_BROWSE_LABEL = "Browse Auctions";
        private static final String DEFAULT_BROWSE_LORE = "Open the main auction house.";
        private static final String DEFAULT_BACK_LABEL = "Back to Browser";
        private static final String DEFAULT_BACK_LORE = "Return to the auction browser.";
        private static final String DEFAULT_INFO_TITLE = "Live Auction Queue";
        private static final String DEFAULT_INFO_LORE_1 = "View upcoming live announcements.";
        private static final String DEFAULT_INFO_LORE_2 = "Listings are broadcast from first to last.";
        private static final String DEFAULT_EMPTY_QUEUE_TITLE = "No Queued Auctions";
        private static final String DEFAULT_EMPTY_QUEUE_LORE = "The live auction queue is currently empty.";

        public LiveMessages {
            disabled = normalize(disabled, DEFAULT_DISABLED);
            queueInstant = normalize(queueInstant, DEFAULT_QUEUE_INSTANT);
            inventoryTitle = normalize(inventoryTitle, DEFAULT_INVENTORY_TITLE);
            closeLabel = normalize(closeLabel, DEFAULT_CLOSE_LABEL);
            closeLore = normalize(closeLore, DEFAULT_CLOSE_LORE);
            refreshLabel = normalize(refreshLabel, DEFAULT_REFRESH_LABEL);
            refreshLore = normalize(refreshLore, DEFAULT_REFRESH_LORE);
            browseLabel = normalize(browseLabel, DEFAULT_BROWSE_LABEL);
            browseLore = normalize(browseLore, DEFAULT_BROWSE_LORE);
            backLabel = normalize(backLabel, DEFAULT_BACK_LABEL);
            backLore = normalize(backLore, DEFAULT_BACK_LORE);
            infoTitle = normalize(infoTitle, DEFAULT_INFO_TITLE);
            infoLoreLine1 = normalize(infoLoreLine1, DEFAULT_INFO_LORE_1);
            infoLoreLine2 = normalize(infoLoreLine2, DEFAULT_INFO_LORE_2);
            emptyQueueTitle = normalize(emptyQueueTitle, DEFAULT_EMPTY_QUEUE_TITLE);
            emptyQueueLore = normalize(emptyQueueLore, DEFAULT_EMPTY_QUEUE_LORE);
        }

        public static LiveMessages defaults() {
            return new LiveMessages(DEFAULT_DISABLED, DEFAULT_QUEUE_INSTANT, DEFAULT_INVENTORY_TITLE, DEFAULT_CLOSE_LABEL,
                    DEFAULT_CLOSE_LORE, DEFAULT_REFRESH_LABEL, DEFAULT_REFRESH_LORE, DEFAULT_BROWSE_LABEL,
                    DEFAULT_BROWSE_LORE, DEFAULT_BACK_LABEL, DEFAULT_BACK_LORE, DEFAULT_INFO_TITLE,
                    DEFAULT_INFO_LORE_1, DEFAULT_INFO_LORE_2, DEFAULT_EMPTY_QUEUE_TITLE, DEFAULT_EMPTY_QUEUE_LORE);
        }

        public static LiveMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new LiveMessages(
                    section.getString("disabled"),
                    section.getString("queue-instant"),
                    section.getString("inventory-title"),
                    section.getString("close-label"),
                    section.getString("close-lore"),
                    section.getString("refresh-label"),
                    section.getString("refresh-lore"),
                    section.getString("browse-label"),
                    section.getString("browse-lore"),
                    section.getString("back-label"),
                    section.getString("back-lore"),
                    section.getString("info-title"),
                    section.getString("info-lore-1"),
                    section.getString("info-lore-2"),
                    section.getString("empty-title"),
                    section.getString("empty-lore"));
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
