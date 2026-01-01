package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Structured configuration for backend auction messages used outside of GUIs.
 */
public final class AuctionBackendMessages {

    private final ListingMessages listing;
    private final OrderMessages order;
    private final ClaimMessages claim;
    private final EconomyMessages economy;
    private final NotificationMessages notifications;
    private final LiveMessages live;
    private final FallbackMessages fallback;

    public AuctionBackendMessages(ListingMessages listing, OrderMessages order, ClaimMessages claim,
            EconomyMessages economy, NotificationMessages notifications, LiveMessages live,
            FallbackMessages fallback) {
        this.listing = listing != null ? listing : ListingMessages.defaults();
        this.order = order != null ? order : OrderMessages.defaults();
        this.claim = claim != null ? claim : ClaimMessages.defaults();
        this.economy = economy != null ? economy : EconomyMessages.defaults();
        this.notifications = notifications != null ? notifications : NotificationMessages.defaults();
        this.live = live != null ? live : LiveMessages.defaults();
        this.fallback = fallback != null ? fallback : FallbackMessages.defaults();
    }

    public ListingMessages listing() {
        return listing;
    }

    public OrderMessages order() {
        return order;
    }

    public ClaimMessages claim() {
        return claim;
    }

    public EconomyMessages economy() {
        return economy;
    }

    public NotificationMessages notifications() {
        return notifications;
    }

    public LiveMessages live() {
        return live;
    }

    public FallbackMessages fallback() {
        return fallback;
    }

    public static AuctionBackendMessages defaults() {
        return new AuctionBackendMessages(ListingMessages.defaults(), OrderMessages.defaults(),
                ClaimMessages.defaults(), EconomyMessages.defaults(), NotificationMessages.defaults(),
                LiveMessages.defaults(), FallbackMessages.defaults());
    }

    public static AuctionBackendMessages from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        ListingMessages listingMessages = ListingMessages.from(section.getConfigurationSection("listing"));
        OrderMessages orderMessages = OrderMessages.from(section.getConfigurationSection("order"));
        ClaimMessages claimMessages = ClaimMessages.from(section.getConfigurationSection("claim"));
        EconomyMessages economyMessages = EconomyMessages.from(section.getConfigurationSection("economy"));
        NotificationMessages notificationMessages =
                NotificationMessages.from(section.getConfigurationSection("notifications"));
        LiveMessages liveMessages = LiveMessages.from(section.getConfigurationSection("live"));
        FallbackMessages fallbackMessages = FallbackMessages.from(section.getConfigurationSection("fallback"));
        return new AuctionBackendMessages(listingMessages, orderMessages, claimMessages, economyMessages,
                notificationMessages, liveMessages, fallbackMessages);
    }

    public record ListingMessages(CreationMessages creation, PurchaseMessages purchase, CancelMessages cancel) {

        public ListingMessages {
            creation = creation != null ? creation : CreationMessages.defaults();
            purchase = purchase != null ? purchase : PurchaseMessages.defaults();
            cancel = cancel != null ? cancel : CancelMessages.defaults();
        }

        public static ListingMessages defaults() {
            return new ListingMessages(CreationMessages.defaults(), PurchaseMessages.defaults(),
                    CancelMessages.defaults());
        }

        public static ListingMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            CreationMessages creation = CreationMessages.from(section.getConfigurationSection("create"));
            PurchaseMessages purchase = PurchaseMessages.from(section.getConfigurationSection("purchase"));
            CancelMessages cancel = CancelMessages.from(section.getConfigurationSection("cancel"));
            return new ListingMessages(creation, purchase, cancel);
        }

        public record CreationMessages(
                String playersOnly,
                String itemRequired,
                String durationPositive,
                String pricePositive,
                String priceMinimum,
                String limitReached,
                String limitSingularNoun,
                String limitPluralNoun,
                String inventoryMissing,
                String removalFailed,
                String success,
                String depositCharged,
                String durationClamped) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may create auction listings.";
            private static final String DEFAULT_ITEM_REQUIRED = "&cYou must provide a valid item to list.";
            private static final String DEFAULT_DURATION_POSITIVE = "&cListings must have a positive duration.";
            private static final String DEFAULT_PRICE_POSITIVE = "&cListings must have a positive price.";
            private static final String DEFAULT_PRICE_MINIMUM =
                    "&cListings must have a price of at least &6{minimum}&c.";
            private static final String DEFAULT_LIMIT_REACHED =
                    "&cYour island level allows up to &e{limit} &cactive auction {noun}.";
            private static final String DEFAULT_LIMIT_SINGULAR_NOUN = "listing";
            private static final String DEFAULT_LIMIT_PLURAL_NOUN = "listings";
            private static final String DEFAULT_INVENTORY_MISSING = "&cYou do not have enough of that item.";
            private static final String DEFAULT_REMOVAL_FAILED =
                    "&cFailed to remove the item from your inventory.";
            private static final String DEFAULT_SUCCESS = "&aListed &b{item}&a for &6{price}&a.";
            private static final String DEFAULT_DEPOSIT_CHARGED = "&eDeposit charged: &6{amount}&e{percent}.";
            private static final String DEFAULT_DURATION_CLAMPED = "&eDuration limited to &6{duration}&e.";

            public CreationMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
                durationPositive = normalize(durationPositive, DEFAULT_DURATION_POSITIVE);
                pricePositive = normalize(pricePositive, DEFAULT_PRICE_POSITIVE);
                priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
                limitReached = normalize(limitReached, DEFAULT_LIMIT_REACHED);
                limitSingularNoun = normalize(limitSingularNoun, DEFAULT_LIMIT_SINGULAR_NOUN);
                limitPluralNoun = normalize(limitPluralNoun, DEFAULT_LIMIT_PLURAL_NOUN);
                inventoryMissing = normalize(inventoryMissing, DEFAULT_INVENTORY_MISSING);
                removalFailed = normalize(removalFailed, DEFAULT_REMOVAL_FAILED);
                success = normalize(success, DEFAULT_SUCCESS);
                depositCharged = normalize(depositCharged, DEFAULT_DEPOSIT_CHARGED);
                durationClamped = normalize(durationClamped, DEFAULT_DURATION_CLAMPED);
            }

            public static CreationMessages defaults() {
                return new CreationMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_ITEM_REQUIRED, DEFAULT_DURATION_POSITIVE,
                        DEFAULT_PRICE_POSITIVE, DEFAULT_PRICE_MINIMUM, DEFAULT_LIMIT_REACHED,
                        DEFAULT_LIMIT_SINGULAR_NOUN, DEFAULT_LIMIT_PLURAL_NOUN, DEFAULT_INVENTORY_MISSING,
                        DEFAULT_REMOVAL_FAILED, DEFAULT_SUCCESS, DEFAULT_DEPOSIT_CHARGED,
                        DEFAULT_DURATION_CLAMPED);
            }

            public static CreationMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new CreationMessages(
                        section.getString("players-only"),
                        section.getString("item-required"),
                        section.getString("duration-positive"),
                        section.getString("price-positive"),
                        section.getString("price-minimum"),
                        section.getString("limit-reached"),
                        section.getString("limit-singular-noun"),
                        section.getString("limit-plural-noun"),
                        section.getString("inventory-missing"),
                        section.getString("removal-failed"),
                        section.getString("success"),
                        section.getString("deposit-charged"),
                        section.getString("duration-clamped"));
            }
        }

        public record PurchaseMessages(
                String playersOnly,
                String notFound,
                String expired,
                String ownListing,
                String noSpace,
                String addFailed,
                String success) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may purchase auction listings.";
            private static final String DEFAULT_NOT_FOUND = "&cThat auction listing could not be found.";
            private static final String DEFAULT_EXPIRED = "&cThat auction listing has already expired.";
            private static final String DEFAULT_OWN_LISTING = "&cYou cannot purchase your own listing.";
            private static final String DEFAULT_NO_SPACE = "&cYou do not have enough inventory space.";
            private static final String DEFAULT_ADD_FAILED = "&cFailed to add the item to your inventory.";
            private static final String DEFAULT_SUCCESS = "&aPurchased &b{item}&a for &6{price}&a.";

            public PurchaseMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                notFound = normalize(notFound, DEFAULT_NOT_FOUND);
                expired = normalize(expired, DEFAULT_EXPIRED);
                ownListing = normalize(ownListing, DEFAULT_OWN_LISTING);
                noSpace = normalize(noSpace, DEFAULT_NO_SPACE);
                addFailed = normalize(addFailed, DEFAULT_ADD_FAILED);
                success = normalize(success, DEFAULT_SUCCESS);
            }

            public static PurchaseMessages defaults() {
                return new PurchaseMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_NOT_FOUND, DEFAULT_EXPIRED,
                        DEFAULT_OWN_LISTING, DEFAULT_NO_SPACE, DEFAULT_ADD_FAILED, DEFAULT_SUCCESS);
            }

            public static PurchaseMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new PurchaseMessages(
                        section.getString("players-only"),
                        section.getString("not-found"),
                        section.getString("expired"),
                        section.getString("own-listing"),
                        section.getString("no-space"),
                        section.getString("add-failed"),
                        section.getString("success"));
            }
        }

        public record CancelMessages(
                String playersOnly,
                String notFound,
                String notOwner,
                String success,
                String depositRefunded) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may cancel auction listings.";
            private static final String DEFAULT_NOT_FOUND = "&cThat auction listing could not be found.";
            private static final String DEFAULT_NOT_OWNER = "&cYou do not own that auction listing.";
            private static final String DEFAULT_SUCCESS = "&aCancelled auction for &b{item}&a.";
            private static final String DEFAULT_DEPOSIT_REFUNDED = "&aDeposit refunded: &6{amount}&a.";

            public CancelMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                notFound = normalize(notFound, DEFAULT_NOT_FOUND);
                notOwner = normalize(notOwner, DEFAULT_NOT_OWNER);
                success = normalize(success, DEFAULT_SUCCESS);
                depositRefunded = normalize(depositRefunded, DEFAULT_DEPOSIT_REFUNDED);
            }

            public static CancelMessages defaults() {
                return new CancelMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_NOT_FOUND, DEFAULT_NOT_OWNER,
                        DEFAULT_SUCCESS, DEFAULT_DEPOSIT_REFUNDED);
            }

            public static CancelMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new CancelMessages(
                        section.getString("players-only"),
                        section.getString("not-found"),
                        section.getString("not-owner"),
                        section.getString("success"),
                        section.getString("deposit-refunded"));
            }
        }
    }

    public record OrderMessages(CreationMessages creation, FulfillmentMessages fulfillment, CancelMessages cancel) {

        public OrderMessages {
            creation = creation != null ? creation : CreationMessages.defaults();
            fulfillment = fulfillment != null ? fulfillment : FulfillmentMessages.defaults();
            cancel = cancel != null ? cancel : CancelMessages.defaults();
        }

        public static OrderMessages defaults() {
            return new OrderMessages(CreationMessages.defaults(), FulfillmentMessages.defaults(),
                    CancelMessages.defaults());
        }

        public static OrderMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            CreationMessages creation = CreationMessages.from(section.getConfigurationSection("create"));
            FulfillmentMessages fulfillment = FulfillmentMessages.from(section.getConfigurationSection("fulfill"));
            CancelMessages cancel = CancelMessages.from(section.getConfigurationSection("cancel"));
            return new OrderMessages(creation, fulfillment, cancel);
        }

        public record CreationMessages(
                String playersOnly,
                String itemRequired,
                String durationPositive,
                String pricePositive,
                String priceMinimum,
                String success,
                String durationClamped) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may create buy orders.";
            private static final String DEFAULT_ITEM_REQUIRED = "&cYou must provide a valid item template to request.";
            private static final String DEFAULT_DURATION_POSITIVE = "&cOrders must have a positive duration.";
            private static final String DEFAULT_PRICE_POSITIVE = "&cOrders must offer a positive price.";
            private static final String DEFAULT_PRICE_MINIMUM =
                    "&cOrders must offer at least &6{minimum}&c per item. Your offer is &6{offered}&c per item.";
            private static final String DEFAULT_SUCCESS = "&aCreated buy order for &b{item}&a offering &6{price}&a.";
            private static final String DEFAULT_DURATION_CLAMPED = "&eDuration limited to &6{duration}&e.";

            public CreationMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                itemRequired = normalize(itemRequired, DEFAULT_ITEM_REQUIRED);
                durationPositive = normalize(durationPositive, DEFAULT_DURATION_POSITIVE);
                pricePositive = normalize(pricePositive, DEFAULT_PRICE_POSITIVE);
                priceMinimum = normalize(priceMinimum, DEFAULT_PRICE_MINIMUM);
                success = normalize(success, DEFAULT_SUCCESS);
                durationClamped = normalize(durationClamped, DEFAULT_DURATION_CLAMPED);
            }

            public static CreationMessages defaults() {
                return new CreationMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_ITEM_REQUIRED, DEFAULT_DURATION_POSITIVE,
                        DEFAULT_PRICE_POSITIVE, DEFAULT_PRICE_MINIMUM, DEFAULT_SUCCESS, DEFAULT_DURATION_CLAMPED);
            }

            public static CreationMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new CreationMessages(
                        section.getString("players-only"),
                        section.getString("item-required"),
                        section.getString("duration-positive"),
                        section.getString("price-positive"),
                        section.getString("price-minimum"),
                        section.getString("success"),
                        section.getString("duration-clamped"));
            }
        }

        public record FulfillmentMessages(
                String playersOnly,
                String notFound,
                String expired,
                String ownOrder,
                String invalid,
                String missingItems,
                String removalFailed,
                String success) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may fulfill buy orders.";
            private static final String DEFAULT_NOT_FOUND = "&cThat buy order could not be found.";
            private static final String DEFAULT_EXPIRED = "&cThat buy order has already expired.";
            private static final String DEFAULT_OWN_ORDER = "&cYou cannot fulfill your own buy order.";
            private static final String DEFAULT_INVALID = "&cThat buy order is no longer valid.";
            private static final String DEFAULT_MISSING_ITEMS =
                    "&cYou do not have the required items to fulfill this order.";
            private static final String DEFAULT_REMOVAL_FAILED =
                    "&cFailed to remove the required items from your inventory.";
            private static final String DEFAULT_SUCCESS =
                    "&aFulfilled buy order for &b{item}&a and earned &6{price}&a.";

            public FulfillmentMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                notFound = normalize(notFound, DEFAULT_NOT_FOUND);
                expired = normalize(expired, DEFAULT_EXPIRED);
                ownOrder = normalize(ownOrder, DEFAULT_OWN_ORDER);
                invalid = normalize(invalid, DEFAULT_INVALID);
                missingItems = normalize(missingItems, DEFAULT_MISSING_ITEMS);
                removalFailed = normalize(removalFailed, DEFAULT_REMOVAL_FAILED);
                success = normalize(success, DEFAULT_SUCCESS);
            }

            public static FulfillmentMessages defaults() {
                return new FulfillmentMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_NOT_FOUND, DEFAULT_EXPIRED,
                        DEFAULT_OWN_ORDER, DEFAULT_INVALID, DEFAULT_MISSING_ITEMS, DEFAULT_REMOVAL_FAILED,
                        DEFAULT_SUCCESS);
            }

            public static FulfillmentMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new FulfillmentMessages(
                        section.getString("players-only"),
                        section.getString("not-found"),
                        section.getString("expired"),
                        section.getString("own-order"),
                        section.getString("invalid"),
                        section.getString("missing-items"),
                        section.getString("removal-failed"),
                        section.getString("success"));
            }
        }

        public record CancelMessages(
                String playersOnly,
                String notFound,
                String notOwner,
                String success,
                String fundsRefunded) {

            private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may cancel buy orders.";
            private static final String DEFAULT_NOT_FOUND = "&cThat buy order could not be found.";
            private static final String DEFAULT_NOT_OWNER = "&cYou do not own that buy order.";
            private static final String DEFAULT_SUCCESS = "&aCancelled buy order for &b{item}&a.";
            private static final String DEFAULT_FUNDS_REFUNDED = "&aFunds refunded: &6{amount}&a.";

            public CancelMessages {
                playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
                notFound = normalize(notFound, DEFAULT_NOT_FOUND);
                notOwner = normalize(notOwner, DEFAULT_NOT_OWNER);
                success = normalize(success, DEFAULT_SUCCESS);
                fundsRefunded = normalize(fundsRefunded, DEFAULT_FUNDS_REFUNDED);
            }

            public static CancelMessages defaults() {
                return new CancelMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_NOT_FOUND, DEFAULT_NOT_OWNER,
                        DEFAULT_SUCCESS, DEFAULT_FUNDS_REFUNDED);
            }

            public static CancelMessages from(ConfigurationSection section) {
                if (section == null) {
                    return defaults();
                }
                return new CancelMessages(
                        section.getString("players-only"),
                        section.getString("not-found"),
                        section.getString("not-owner"),
                        section.getString("success"),
                        section.getString("funds-refunded"));
            }
        }
    }

    public record ClaimMessages(
            String playersOnly,
            String noneAvailable,
            String inventoryFull,
            String partial,
            String complete,
            String reminder) {

        private static final String DEFAULT_PLAYERS_ONLY = "&cOnly players may claim returned auction items.";
        private static final String DEFAULT_NONE_AVAILABLE = "&eYou have no returned auction items to claim.";
        private static final String DEFAULT_INVENTORY_FULL =
                "&cYour inventory is full. Clear space and try claiming your returned items again.";
        private static final String DEFAULT_PARTIAL =
                "&eClaimed &b{claimed}&e returned auction item{claimed-suffix}. &6{remaining}&e item{remaining-suffix} remain in storage.";
        private static final String DEFAULT_COMPLETE =
                "&aClaimed &b{claimed}&a returned auction item{claimed-suffix}.";
        private static final String DEFAULT_REMINDER =
                "&6You have &b{total}&6 returned auction item{returned-suffix} waiting. Use &e{command}&6 to retrieve them.";

        public ClaimMessages {
            playersOnly = normalize(playersOnly, DEFAULT_PLAYERS_ONLY);
            noneAvailable = normalize(noneAvailable, DEFAULT_NONE_AVAILABLE);
            inventoryFull = normalize(inventoryFull, DEFAULT_INVENTORY_FULL);
            partial = normalize(partial, DEFAULT_PARTIAL);
            complete = normalize(complete, DEFAULT_COMPLETE);
            reminder = normalize(reminder, DEFAULT_REMINDER);
        }

        public static ClaimMessages defaults() {
            return new ClaimMessages(DEFAULT_PLAYERS_ONLY, DEFAULT_NONE_AVAILABLE, DEFAULT_INVENTORY_FULL,
                    DEFAULT_PARTIAL, DEFAULT_COMPLETE, DEFAULT_REMINDER);
        }

        public static ClaimMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new ClaimMessages(
                    section.getString("players-only"),
                    section.getString("none-available"),
                    section.getString("inventory-full"),
                    section.getString("partial"),
                    section.getString("complete"),
                    section.getString("reminder"));
        }
    }

    public record EconomyMessages(
            String noProvider,
            String depositInsufficient,
            String purchaseInsufficient,
            String transactionFailed,
            String invalidPrice,
            String reservePositive,
            String reserveInsufficient) {

        private static final String DEFAULT_NO_PROVIDER =
                "&cThe auction house is unavailable because no economy provider is configured.";
        private static final String DEFAULT_DEPOSIT_INSUFFICIENT =
                "&cYou cannot afford the listing deposit of &6{amount}&c.";
        private static final String DEFAULT_PURCHASE_INSUFFICIENT = "&cYou cannot afford this listing.";
        private static final String DEFAULT_TRANSACTION_FAILED = "&cTransaction failed: {error}";
        private static final String DEFAULT_INVALID_PRICE = "&cThis listing does not have a valid purchase price.";
        private static final String DEFAULT_RESERVE_POSITIVE =
                "&cOrders must reserve a positive amount of currency.";
        private static final String DEFAULT_RESERVE_INSUFFICIENT =
                "&cYou cannot afford to reserve that much currency.";

        public EconomyMessages {
            noProvider = normalize(noProvider, DEFAULT_NO_PROVIDER);
            depositInsufficient = normalize(depositInsufficient, DEFAULT_DEPOSIT_INSUFFICIENT);
            purchaseInsufficient = normalize(purchaseInsufficient, DEFAULT_PURCHASE_INSUFFICIENT);
            transactionFailed = normalize(transactionFailed, DEFAULT_TRANSACTION_FAILED);
            invalidPrice = normalize(invalidPrice, DEFAULT_INVALID_PRICE);
            reservePositive = normalize(reservePositive, DEFAULT_RESERVE_POSITIVE);
            reserveInsufficient = normalize(reserveInsufficient, DEFAULT_RESERVE_INSUFFICIENT);
        }

        public static EconomyMessages defaults() {
            return new EconomyMessages(DEFAULT_NO_PROVIDER, DEFAULT_DEPOSIT_INSUFFICIENT,
                    DEFAULT_PURCHASE_INSUFFICIENT, DEFAULT_TRANSACTION_FAILED, DEFAULT_INVALID_PRICE,
                    DEFAULT_RESERVE_POSITIVE, DEFAULT_RESERVE_INSUFFICIENT);
        }

        public static EconomyMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new EconomyMessages(
                    section.getString("no-provider"),
                    section.getString("deposit-insufficient"),
                    section.getString("purchase-insufficient"),
                    section.getString("transaction-failed"),
                    section.getString("invalid-price"),
                    section.getString("reserve-positive"),
                    section.getString("reserve-insufficient"));
        }
    }

    public record NotificationMessages(
            String sellerSold,
            String sellerDepositRefunded,
            String sellerExpired,
            String sellerReturned,
            String storageReminder,
            String buyerFulfilled,
            String buyerItemsStored,
            String buyerDeliveryFailed,
            String buyerExpired) {

        private static final String DEFAULT_SELLER_SOLD =
                "&aYour auction for &b{item}&a sold for &6{price}&a.";
        private static final String DEFAULT_SELLER_DEPOSIT_REFUNDED = "&aDeposit refunded: &6{amount}&a.";
        private static final String DEFAULT_SELLER_EXPIRED = "&eYour auction for &b{item}&e has expired.";
        private static final String DEFAULT_SELLER_RETURNED =
                "&aYour expired auction item &b{item}&a has been returned to your inventory.";
        private static final String DEFAULT_STORAGE_REMINDER =
                "&eYour inventory is full. Some items were stored. Use &6{command}&e to retrieve them.";
        private static final String DEFAULT_BUYER_FULFILLED =
                "&aYour buy order for &b{item}&a has been fulfilled for &6{price}&a.";
        private static final String DEFAULT_BUYER_ITEMS_STORED =
                "&eYour inventory was full. &6{amount}&e item{item-suffix} stored. Use &6{command}&e to retrieve them.";
        private static final String DEFAULT_BUYER_DELIVERY_FAILED =
                "&eUnable to deliver fulfilled order items. Please check your inventory.";
        private static final String DEFAULT_BUYER_EXPIRED =
                "&eYour buy order for &b{item}&e has expired. Reserved funds were refunded.";

        public NotificationMessages {
            sellerSold = normalize(sellerSold, DEFAULT_SELLER_SOLD);
            sellerDepositRefunded = normalize(sellerDepositRefunded, DEFAULT_SELLER_DEPOSIT_REFUNDED);
            sellerExpired = normalize(sellerExpired, DEFAULT_SELLER_EXPIRED);
            sellerReturned = normalize(sellerReturned, DEFAULT_SELLER_RETURNED);
            storageReminder = normalize(storageReminder, DEFAULT_STORAGE_REMINDER);
            buyerFulfilled = normalize(buyerFulfilled, DEFAULT_BUYER_FULFILLED);
            buyerItemsStored = normalize(buyerItemsStored, DEFAULT_BUYER_ITEMS_STORED);
            buyerDeliveryFailed = normalize(buyerDeliveryFailed, DEFAULT_BUYER_DELIVERY_FAILED);
            buyerExpired = normalize(buyerExpired, DEFAULT_BUYER_EXPIRED);
        }

        public static NotificationMessages defaults() {
            return new NotificationMessages(DEFAULT_SELLER_SOLD, DEFAULT_SELLER_DEPOSIT_REFUNDED,
                    DEFAULT_SELLER_EXPIRED, DEFAULT_SELLER_RETURNED, DEFAULT_STORAGE_REMINDER,
                    DEFAULT_BUYER_FULFILLED, DEFAULT_BUYER_ITEMS_STORED, DEFAULT_BUYER_DELIVERY_FAILED,
                    DEFAULT_BUYER_EXPIRED);
        }

        public static NotificationMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new NotificationMessages(
                    section.getString("seller-sold"),
                    section.getString("seller-deposit-refunded"),
                    section.getString("seller-expired"),
                    section.getString("seller-returned"),
                    section.getString("storage-reminder"),
                    section.getString("buyer-fulfilled"),
                    section.getString("buyer-items-stored"),
                    section.getString("buyer-delivery-failed"),
                    section.getString("buyer-expired"));
        }
    }

    public record LiveMessages(String broadcast) {

        private static final String DEFAULT_BROADCAST =
                "&6[Live Auction] &a{seller}&7 is auctioning &b{item}&7 for &6{price}&7.";

        public LiveMessages {
            broadcast = normalize(broadcast, DEFAULT_BROADCAST);
        }

        public static LiveMessages defaults() {
            return new LiveMessages(DEFAULT_BROADCAST);
        }

        public static LiveMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new LiveMessages(section.getString("broadcast"));
        }
    }

    public record FallbackMessages(
            String unknownItem,
            String unknownName,
            String unknownMaterial,
            String unknownValue,
            String unknownError) {

        private static final String DEFAULT_UNKNOWN_ITEM = "Unknown item";
        private static final String DEFAULT_UNKNOWN_NAME = "Unknown";
        private static final String DEFAULT_UNKNOWN_MATERIAL = "Unknown";
        private static final String DEFAULT_UNKNOWN_VALUE = "Unknown";
        private static final String DEFAULT_UNKNOWN_ERROR = "unknown error";

        public FallbackMessages {
            unknownItem = normalize(unknownItem, DEFAULT_UNKNOWN_ITEM);
            unknownName = normalize(unknownName, DEFAULT_UNKNOWN_NAME);
            unknownMaterial = normalize(unknownMaterial, DEFAULT_UNKNOWN_MATERIAL);
            unknownValue = normalize(unknownValue, DEFAULT_UNKNOWN_VALUE);
            unknownError = normalize(unknownError, DEFAULT_UNKNOWN_ERROR);
        }

        public static FallbackMessages defaults() {
            return new FallbackMessages(DEFAULT_UNKNOWN_ITEM, DEFAULT_UNKNOWN_NAME, DEFAULT_UNKNOWN_MATERIAL,
                    DEFAULT_UNKNOWN_VALUE, DEFAULT_UNKNOWN_ERROR);
        }

        public static FallbackMessages from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            return new FallbackMessages(
                    section.getString("unknown-item"),
                    section.getString("unknown-name"),
                    section.getString("unknown-material"),
                    section.getString("unknown-value"),
                    section.getString("unknown-error"));
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

