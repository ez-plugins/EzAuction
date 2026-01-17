package com.skyblockexp.ezauction.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration for the auction GUI layouts.
 */
public final class AuctionMenuConfiguration {

    private static final String DEFAULT_BROWSER_TITLE = "&2Auction House &7({page}/{total_pages})";
    private static final int DEFAULT_BROWSER_SIZE = 54;
        private static final MenuButtonConfiguration DEFAULT_BROWSER_FILLER_BUTTON =
            new MenuButtonConfiguration(Material.GRAY_STAINED_GLASS_PANE, "&8 ");
    private static final int DEFAULT_BROWSER_PREVIOUS_SLOT = 45;
    private static final int DEFAULT_BROWSER_CLOSE_SLOT = 49;
    private static final int DEFAULT_BROWSER_NEXT_SLOT = 53;
    private static final int DEFAULT_BROWSER_EMPTY_LISTING_SLOT = 22;
    private static final int DEFAULT_BROWSER_LISTINGS_TOGGLE_SLOT = 46;
    private static final int DEFAULT_BROWSER_ORDERS_TOGGLE_SLOT = 52;
        private static final ToggleButtonConfiguration DEFAULT_BROWSER_LISTINGS_TOGGLE =
            new ToggleButtonConfiguration(
                DEFAULT_BROWSER_LISTINGS_TOGGLE_SLOT,
                new MenuButtonConfiguration(
                    Material.CHEST,
                    "&aListings",
                    List.of("&7Browse player sell listings.")));
        private static final ToggleButtonConfiguration DEFAULT_BROWSER_ORDERS_TOGGLE =
            new ToggleButtonConfiguration(
                DEFAULT_BROWSER_ORDERS_TOGGLE_SLOT,
                new MenuButtonConfiguration(
                    Material.PAPER,
                    "&aBuy Orders",
                    List.of("&7Browse active buy orders.")));
        private static final BrowserMenuConfiguration.SearchButtonConfiguration DEFAULT_BROWSER_SEARCH_BUTTON =
            new BrowserMenuConfiguration.SearchButtonConfiguration(
                48,
                new MenuButtonConfiguration(
                    Material.COMPASS,
                    "&bSearch",
                    List.of("&7Find items by name.")));
        private static final BrowserMenuConfiguration.SortButtonConfiguration DEFAULT_BROWSER_SORT_BUTTON =
            new BrowserMenuConfiguration.SortButtonConfiguration(
                51,
                new MenuButtonConfiguration(
                    Material.HOPPER,
                    "&bSort",
                    List.of("&7Change how listings are ordered.")));
        private static final BrowserMenuConfiguration.SearchTipsButtonConfiguration DEFAULT_BROWSER_SEARCH_TIPS_BUTTON =
            new BrowserMenuConfiguration.SearchTipsButtonConfiguration(
                50,
                new MenuButtonConfiguration(
                    Material.KNOWLEDGE_BOOK,
                    "&eSearch Tips",
                    List.of("&7Supported patterns:", "&e• &7Item name: &ffortune",
                            "&e• &7With level: &ffortune 3", "&e• &7Roman numerals: &fsharpness v",
                            "&e• &7Material ID: &fminecraft:diamond_sword", "&7Click to close this help.")));
        private static final BrowserMenuConfiguration.ClaimsButtonConfiguration DEFAULT_BROWSER_CLAIMS_BUTTON =
            new BrowserMenuConfiguration.ClaimsButtonConfiguration(
                47,
                new MenuButtonConfiguration(
                    Material.ENDER_CHEST,
                    "&6Pending Returns",
                    List.of("&7Click to claim items.")));

    private static final String DEFAULT_CONFIRM_TITLE = "&2Confirm Purchase";
    private static final int DEFAULT_CONFIRM_SIZE = 27;
        private static final MenuButtonConfiguration DEFAULT_CONFIRM_FILLER_BUTTON =
            new MenuButtonConfiguration(Material.GRAY_STAINED_GLASS_PANE, "&8 ");
        private static final ConfirmMenuConfiguration.ButtonConfiguration DEFAULT_CONFIRM_BUTTON =
            new ConfirmMenuConfiguration.ButtonConfiguration(
                11,
                new MenuButtonConfiguration(
                    Material.LIME_CONCRETE,
                    "&aConfirm",
                    List.of()));
    private static final int DEFAULT_LISTING_SLOT = 13;
        private static final ConfirmMenuConfiguration.ButtonConfiguration DEFAULT_CANCEL_BUTTON =
            new ConfirmMenuConfiguration.ButtonConfiguration(
                15,
                new MenuButtonConfiguration(
                    Material.RED_CONCRETE,
                    "&cCancel",
                    List.of()));

    private static final BrowserMenuConfiguration DEFAULT_BROWSER = new BrowserMenuConfiguration(
            DEFAULT_BROWSER_TITLE,
            DEFAULT_BROWSER_SIZE,
            DEFAULT_BROWSER_FILLER_BUTTON,
            DEFAULT_BROWSER_PREVIOUS_SLOT,
            DEFAULT_BROWSER_CLOSE_SLOT,
            DEFAULT_BROWSER_NEXT_SLOT,
            DEFAULT_BROWSER_EMPTY_LISTING_SLOT,
            DEFAULT_BROWSER_LISTINGS_TOGGLE,
            DEFAULT_BROWSER_ORDERS_TOGGLE,
            DEFAULT_BROWSER_SEARCH_BUTTON,
            DEFAULT_BROWSER_SORT_BUTTON,
            DEFAULT_BROWSER_SEARCH_TIPS_BUTTON,
            DEFAULT_BROWSER_CLAIMS_BUTTON);
    private static final ConfirmMenuConfiguration DEFAULT_CONFIRM = new ConfirmMenuConfiguration(
            DEFAULT_CONFIRM_TITLE,
            DEFAULT_CONFIRM_SIZE,
            DEFAULT_CONFIRM_FILLER_BUTTON,
            DEFAULT_CONFIRM_BUTTON,
            DEFAULT_LISTING_SLOT,
            DEFAULT_CANCEL_BUTTON);

    private final BrowserMenuConfiguration browser;
    private final ConfirmMenuConfiguration confirm;
    private final boolean historyGuiEnabled;

    public AuctionMenuConfiguration(BrowserMenuConfiguration browser, ConfirmMenuConfiguration confirm) {
        this(browser, confirm, true);
    }

    public AuctionMenuConfiguration(BrowserMenuConfiguration browser, ConfirmMenuConfiguration confirm, boolean historyGuiEnabled) {
        this.browser = browser != null ? browser : DEFAULT_BROWSER;
        this.confirm = confirm != null ? confirm : DEFAULT_CONFIRM;
        this.historyGuiEnabled = historyGuiEnabled;
    }

    public BrowserMenuConfiguration browser() {
        return browser;
    }

    public ConfirmMenuConfiguration confirm() {
        return confirm;
    }

    public static AuctionMenuConfiguration defaults() {
        return new AuctionMenuConfiguration(DEFAULT_BROWSER, DEFAULT_CONFIRM);
    }

    public static AuctionMenuConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        BrowserMenuConfiguration browserConfiguration = BrowserMenuConfiguration
            .from(section.getConfigurationSection("browser"));
        ConfirmMenuConfiguration confirmConfiguration = ConfirmMenuConfiguration
            .from(section.getConfigurationSection("confirm"));
        boolean historyGuiEnabled = true;
        if (section.contains("history-gui.enabled")) {
            historyGuiEnabled = section.getBoolean("history-gui.enabled", true);
        }

        return new AuctionMenuConfiguration(browserConfiguration, confirmConfiguration, historyGuiEnabled);
    }
    
    public boolean historyGuiEnabled() {
        return historyGuiEnabled;
    }

    @Override
    public String toString() {
        return "AuctionMenuConfiguration{"
                + "browser=" + browser
                + ", confirm=" + confirm
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionMenuConfiguration that)) {
            return false;
        }
        return Objects.equals(browser, that.browser)
                && Objects.equals(confirm, that.confirm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(browser, confirm);
    }

    private static int sanitizeInventorySize(int requested, int fallback) {
        int size = requested;
        if (size <= 0 || size % 9 != 0) {
            size = fallback;
        }
        if (size > 54) {
            size = 54;
        }
        return size;
    }

    private static int sanitizeSlot(int value, int size, int fallback) {
        int sanitizedFallback = fallback;
        if (sanitizedFallback < 0 || sanitizedFallback >= size) {
            sanitizedFallback = size > 0 ? Math.max(0, Math.min(size - 1, sanitizedFallback)) : 0;
        }
        if (value < 0 || value >= size) {
            return sanitizedFallback;
        }
        return value;
    }

    private static Material parseMaterial(String value, Material fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String normalized = value.trim().replace(' ', '_');
        try {
            return Material.valueOf(normalized.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static MenuButtonConfiguration parseMenuButton(ConfigurationSection section,
            MenuButtonConfiguration fallback) {
        if (section == null) {
            return fallback;
        }
        Material material = parseMaterial(section.getString("material"), fallback.material());
        String displayName = section.getString("display-name", fallback.displayName());
        List<String> lore = parseLore(section, "lore", fallback.lore());
        return new MenuButtonConfiguration(material, displayName, lore);
    }

    private static List<String> parseLore(ConfigurationSection section, String path, List<String> fallback) {
        if (section == null) {
            return fallback;
        }
        if (!section.contains(path)) {
            return fallback;
        }
        if (section.isList(path)) {
            return sanitizeLore(section.getStringList(path));
        }
        if (section.isString(path)) {
            String value = section.getString(path);
            if (value == null) {
                return List.of();
            }
            return List.of(value);
        }
        return fallback;
    }

    private static List<String> sanitizeLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>(lore.size());
        for (String line : lore) {
            if (line != null) {
                sanitized.add(line);
            }
        }
        return sanitized.isEmpty() ? List.of() : List.copyOf(sanitized);
    }

    private static ToggleButtonConfiguration sanitizeToggle(ToggleButtonConfiguration configuration, int size,
            ToggleButtonConfiguration fallback) {
        ToggleButtonConfiguration fallbackConfig = Objects.requireNonNull(fallback, "fallbackToggle");
        ToggleButtonConfiguration effective = configuration != null ? configuration : fallbackConfig;
        int sanitizedSlot = sanitizeSlot(effective.slot, size, fallbackConfig.slot);
        MenuButtonConfiguration button = effective.button != null ? effective.button : fallbackConfig.button;
        return new ToggleButtonConfiguration(sanitizedSlot, button);
    }

    private static BrowserMenuConfiguration.SearchButtonConfiguration sanitizeSearchButton(
            BrowserMenuConfiguration.SearchButtonConfiguration configuration, int size,
            BrowserMenuConfiguration.SearchButtonConfiguration fallback) {
        BrowserMenuConfiguration.SearchButtonConfiguration fallbackConfig = Objects
                .requireNonNull(fallback, "fallbackSearch");
        BrowserMenuConfiguration.SearchButtonConfiguration effective =
                configuration != null ? configuration : fallbackConfig;
        int sanitizedSlot = sanitizeSlot(effective.slot(), size, fallbackConfig.slot());
        AuctionMenuConfiguration.MenuButtonConfiguration button = effective.button() != null
                ? effective.button()
                : fallbackConfig.button();
        return new BrowserMenuConfiguration.SearchButtonConfiguration(sanitizedSlot, button);
    }

    private static BrowserMenuConfiguration.SortButtonConfiguration sanitizeSortButton(
            BrowserMenuConfiguration.SortButtonConfiguration configuration, int size,
            BrowserMenuConfiguration.SortButtonConfiguration fallback) {
        BrowserMenuConfiguration.SortButtonConfiguration fallbackConfig = Objects
                .requireNonNull(fallback, "fallbackSort");
        BrowserMenuConfiguration.SortButtonConfiguration effective =
                configuration != null ? configuration : fallbackConfig;
        int sanitizedSlot = sanitizeSlot(effective.slot(), size, fallbackConfig.slot());
        AuctionMenuConfiguration.MenuButtonConfiguration button = effective.button() != null
                ? effective.button()
                : fallbackConfig.button();
        return new BrowserMenuConfiguration.SortButtonConfiguration(sanitizedSlot, button);
    }

    private static ConfirmMenuConfiguration.ButtonConfiguration sanitizeButton(
            ConfirmMenuConfiguration.ButtonConfiguration configuration, int size,
            ConfirmMenuConfiguration.ButtonConfiguration fallback) {
        ConfirmMenuConfiguration.ButtonConfiguration fallbackConfig = Objects.requireNonNull(fallback, "fallbackButton");
        ConfirmMenuConfiguration.ButtonConfiguration effective = configuration != null ? configuration : fallbackConfig;
        int sanitizedSlot = sanitizeSlot(effective.slot, size, fallbackConfig.slot);
        MenuButtonConfiguration button = effective.button != null ? effective.button : fallbackConfig.button;
        return new ConfirmMenuConfiguration.ButtonConfiguration(sanitizedSlot, button);
    }

    public static final class BrowserMenuConfiguration {

        private final String title;
        private final int size;
        private final MenuButtonConfiguration filler;
        private final int previousSlot;
        private final int closeSlot;
        private final int nextSlot;
        private final int emptyListingSlot;
        private final ToggleButtonConfiguration listingsToggle;
        private final ToggleButtonConfiguration ordersToggle;
        private final SearchButtonConfiguration searchButton;
        private final SortButtonConfiguration sortButton;
        private final SearchTipsButtonConfiguration searchTipsButton;
        private final ClaimsButtonConfiguration claimsButton;

        private BrowserMenuConfiguration(String title, int size, MenuButtonConfiguration filler, int previousSlot,
                int closeSlot, int nextSlot, int emptyListingSlot, ToggleButtonConfiguration listingsToggle,
                ToggleButtonConfiguration ordersToggle, SearchButtonConfiguration searchButton,
                SortButtonConfiguration sortButton, SearchTipsButtonConfiguration searchTipsButton,
                ClaimsButtonConfiguration claimsButton) {
            this.title = title != null ? title : DEFAULT_BROWSER_TITLE;
            int sanitizedSize = sanitizeInventorySize(size, DEFAULT_BROWSER_SIZE);
            this.size = sanitizedSize;
            this.filler = filler != null ? filler : DEFAULT_BROWSER_FILLER_BUTTON;
            this.previousSlot = sanitizeSlot(previousSlot, sanitizedSize, DEFAULT_BROWSER_PREVIOUS_SLOT);
            this.closeSlot = sanitizeSlot(closeSlot, sanitizedSize, DEFAULT_BROWSER_CLOSE_SLOT);
            this.nextSlot = sanitizeSlot(nextSlot, sanitizedSize, DEFAULT_BROWSER_NEXT_SLOT);
            this.emptyListingSlot = sanitizeSlot(emptyListingSlot, sanitizedSize, DEFAULT_BROWSER_EMPTY_LISTING_SLOT);
            this.listingsToggle = sanitizeToggle(listingsToggle, sanitizedSize, DEFAULT_BROWSER_LISTINGS_TOGGLE);
            this.ordersToggle = sanitizeToggle(ordersToggle, sanitizedSize, DEFAULT_BROWSER_ORDERS_TOGGLE);
            this.searchButton = sanitizeSearchButton(searchButton, sanitizedSize, DEFAULT_BROWSER_SEARCH_BUTTON);
            this.sortButton = sanitizeSortButton(sortButton, sanitizedSize, DEFAULT_BROWSER_SORT_BUTTON);
            this.searchTipsButton = searchTipsButton != null ? searchTipsButton : DEFAULT_BROWSER_SEARCH_TIPS_BUTTON;
            this.claimsButton = claimsButton != null ? claimsButton : DEFAULT_BROWSER_CLAIMS_BUTTON;
        }

        public static BrowserMenuConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return DEFAULT_BROWSER;
            }
            String title = section.getString("title", DEFAULT_BROWSER.title);
            int requestedSize = section.getInt("size", DEFAULT_BROWSER.size);
            int size = sanitizeInventorySize(requestedSize, DEFAULT_BROWSER.size);
            MenuButtonConfiguration filler = parseMenuButton(section.getConfigurationSection("filler"),
                    DEFAULT_BROWSER.filler);
            int previous = sanitizeSlot(section.getInt("navigation.previous-slot", DEFAULT_BROWSER.previousSlot), size,
                    DEFAULT_BROWSER.previousSlot);
            int close = sanitizeSlot(section.getInt("navigation.close-slot", DEFAULT_BROWSER.closeSlot), size,
                    DEFAULT_BROWSER.closeSlot);
            int next = sanitizeSlot(section.getInt("navigation.next-slot", DEFAULT_BROWSER.nextSlot), size,
                    DEFAULT_BROWSER.nextSlot);
            int emptySlot = sanitizeSlot(section.getInt("empty-listing-slot", DEFAULT_BROWSER.emptyListingSlot), size,
                    DEFAULT_BROWSER.emptyListingSlot);
            ToggleButtonConfiguration listingsToggle = ToggleButtonConfiguration.from(
                    section.getConfigurationSection("toggles.listings"), size, DEFAULT_BROWSER.listingsToggle);
            ToggleButtonConfiguration ordersToggle = ToggleButtonConfiguration.from(
                    section.getConfigurationSection("toggles.orders"), size, DEFAULT_BROWSER.ordersToggle);
            SearchButtonConfiguration searchButton = SearchButtonConfiguration.from(
                    section.getConfigurationSection("search"), size, DEFAULT_BROWSER.searchButton);
            SortButtonConfiguration sortButton = SortButtonConfiguration.from(
                    section.getConfigurationSection("sort"), size, DEFAULT_BROWSER.sortButton);
            SearchTipsButtonConfiguration searchTipsButton = SearchTipsButtonConfiguration.from(
                    section.getConfigurationSection("search-tips"), size, DEFAULT_BROWSER.searchTipsButton);
            ClaimsButtonConfiguration claimsButton = ClaimsButtonConfiguration.from(
                    section.getConfigurationSection("claims"), size, DEFAULT_BROWSER.claimsButton);
            return new BrowserMenuConfiguration(title, size, filler, previous, close, next, emptySlot, listingsToggle,
                    ordersToggle, searchButton, sortButton, searchTipsButton, claimsButton);
        }

        public String title() {
            return title;
        }

        public int size() {
            return size;
        }

        public MenuButtonConfiguration filler() {
            return filler;
        }

        public int previousSlot() {
            return previousSlot;
        }

        public int closeSlot() {
            return closeSlot;
        }

        public int nextSlot() {
            return nextSlot;
        }

        public int emptyListingSlot() {
            return emptyListingSlot;
        }

        public ToggleButtonConfiguration listingsToggle() {
            return listingsToggle;
        }

        public ToggleButtonConfiguration ordersToggle() {
            return ordersToggle;
        }

        public SearchButtonConfiguration searchButton() {
            return searchButton;
        }

        public SortButtonConfiguration sortButton() {
            return sortButton;
        }

        public SearchTipsButtonConfiguration searchTipsButton() {
            return searchTipsButton;
        }

        public ClaimsButtonConfiguration claimsButton() {
            return claimsButton;
        }

        @Override
        public String toString() {
            return "BrowserMenuConfiguration{"
                    + "title='" + title + '\''
                    + ", size=" + size
                    + ", filler=" + filler
                    + ", previousSlot=" + previousSlot
                    + ", closeSlot=" + closeSlot
                    + ", nextSlot=" + nextSlot
                    + ", emptyListingSlot=" + emptyListingSlot
                    + ", listingsToggle=" + listingsToggle
                    + ", ordersToggle=" + ordersToggle
                    + ", searchButton=" + searchButton
                    + ", sortButton=" + sortButton
                    + ", searchTipsButton=" + searchTipsButton
                    + ", claimsButton=" + claimsButton
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BrowserMenuConfiguration that)) {
                return false;
            }
            return size == that.size
                    && previousSlot == that.previousSlot
                    && closeSlot == that.closeSlot
                    && nextSlot == that.nextSlot
                    && emptyListingSlot == that.emptyListingSlot
                    && Objects.equals(title, that.title)
                    && Objects.equals(filler, that.filler)
                    && Objects.equals(listingsToggle, that.listingsToggle)
                    && Objects.equals(ordersToggle, that.ordersToggle)
                    && Objects.equals(searchButton, that.searchButton)
                    && Objects.equals(sortButton, that.sortButton)
                    && Objects.equals(searchTipsButton, that.searchTipsButton)
                    && Objects.equals(claimsButton, that.claimsButton);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, size, filler, previousSlot, closeSlot, nextSlot, emptyListingSlot, listingsToggle,
                    ordersToggle, searchButton, sortButton, searchTipsButton, claimsButton);
        }

        public static final class SearchButtonConfiguration {

            private final int slot;
            private final MenuButtonConfiguration button;

            private SearchButtonConfiguration(int slot, MenuButtonConfiguration button) {
                this.slot = slot;
                this.button = button != null ? button : DEFAULT_BROWSER_FILLER_BUTTON;
            }

            public static SearchButtonConfiguration from(ConfigurationSection section, int inventorySize,
                    SearchButtonConfiguration fallback) {
                SearchButtonConfiguration fallbackConfig = fallback != null ? fallback : DEFAULT_BROWSER_SEARCH_BUTTON;
                if (section == null) {
                    return sanitizeSearchButton(fallbackConfig, inventorySize, fallbackConfig);
                }
                int slot = sanitizeSlot(section.getInt("slot", fallbackConfig.slot), inventorySize, fallbackConfig.slot);
                MenuButtonConfiguration button = parseMenuButton(section, fallbackConfig.button);
                return new SearchButtonConfiguration(slot, button);
            }

            public int slot() {
                return slot;
            }

            public MenuButtonConfiguration button() {
                return button;
            }

            @Override
            public String toString() {
                return "SearchButtonConfiguration{"
                        + "slot=" + slot
                        + ", button=" + button
                        + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof SearchButtonConfiguration that)) {
                    return false;
                }
                return slot == that.slot && Objects.equals(button, that.button);
            }

            @Override
            public int hashCode() {
                return Objects.hash(slot, button);
            }
        }

        public static final class SortButtonConfiguration {

            private final int slot;
            private final MenuButtonConfiguration button;

            private SortButtonConfiguration(int slot, MenuButtonConfiguration button) {
                this.slot = slot;
                this.button = button != null ? button : DEFAULT_BROWSER_FILLER_BUTTON;
            }

            public static SortButtonConfiguration from(ConfigurationSection section, int inventorySize,
                    SortButtonConfiguration fallback) {
                SortButtonConfiguration fallbackConfig = fallback != null ? fallback : DEFAULT_BROWSER_SORT_BUTTON;
                if (section == null) {
                    return sanitizeSortButton(fallbackConfig, inventorySize, fallbackConfig);
                }
                int slot = sanitizeSlot(section.getInt("slot", fallbackConfig.slot), inventorySize,
                        fallbackConfig.slot);
                MenuButtonConfiguration button = parseMenuButton(section, fallbackConfig.button);
                return new SortButtonConfiguration(slot, button);
            }

            public int slot() {
                return slot;
            }

            public MenuButtonConfiguration button() {
                return button;
            }

            @Override
            public String toString() {
                return "SortButtonConfiguration{"
                        + "slot=" + slot
                        + ", button=" + button
                        + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof SortButtonConfiguration that)) {
                    return false;
                }
                return slot == that.slot && Objects.equals(button, that.button);
            }

            @Override
            public int hashCode() {
                return Objects.hash(slot, button);
            }
        }

        public static final class SearchTipsButtonConfiguration {

            private final int slot;
            private final MenuButtonConfiguration button;

            private SearchTipsButtonConfiguration(int slot, MenuButtonConfiguration button) {
                this.slot = slot;
                this.button = button != null ? button : DEFAULT_BROWSER_FILLER_BUTTON;
            }

            public static SearchTipsButtonConfiguration from(ConfigurationSection section, int inventorySize,
                    SearchTipsButtonConfiguration fallback) {
                if (fallback == null) {
                    fallback = new SearchTipsButtonConfiguration(50,
                            new MenuButtonConfiguration(Material.KNOWLEDGE_BOOK, "&eSearch Tips",
                                    List.of("&7Supported patterns:", "&e• &7Item name: &ffortune",
                                            "&e• &7With level: &ffortune 3", "&e• &7Roman numerals: &fsharpness v",
                                            "&e• &7Material ID: &fminecraft:diamond_sword",
                                            "&7Click to close this help.")));
                }
                if (section == null) {
                    return fallback;
                }
                int slot = sanitizeSlot(section.getInt("slot", fallback.slot), inventorySize, fallback.slot);
                MenuButtonConfiguration button = parseMenuButton(section, fallback.button);
                return new SearchTipsButtonConfiguration(slot, button);
            }

            public int slot() {
                return slot;
            }

            public MenuButtonConfiguration button() {
                return button;
            }

            @Override
            public String toString() {
                return "SearchTipsButtonConfiguration{" + "slot=" + slot + ", button=" + button + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof SearchTipsButtonConfiguration that)) return false;
                return slot == that.slot && Objects.equals(button, that.button);
            }

            @Override
            public int hashCode() {
                return Objects.hash(slot, button);
            }
        }

        public static final class ClaimsButtonConfiguration {

            private final int slot;
            private final MenuButtonConfiguration button;

            private ClaimsButtonConfiguration(int slot, MenuButtonConfiguration button) {
                this.slot = slot;
                this.button = button != null ? button : DEFAULT_BROWSER_FILLER_BUTTON;
            }

            public static ClaimsButtonConfiguration from(ConfigurationSection section, int inventorySize,
                    ClaimsButtonConfiguration fallback) {
                if (fallback == null) {
                    fallback = new ClaimsButtonConfiguration(47,
                            new MenuButtonConfiguration(Material.ENDER_CHEST, "&6Pending Returns",
                                    List.of("&7Click to claim items.")));
                }
                if (section == null) {
                    return fallback;
                }
                int slot = sanitizeSlot(section.getInt("slot", fallback.slot), inventorySize, fallback.slot);
                MenuButtonConfiguration button = parseMenuButton(section, fallback.button);
                return new ClaimsButtonConfiguration(slot, button);
            }

            public int slot() {
                return slot;
            }

            public MenuButtonConfiguration button() {
                return button;
            }

            @Override
            public String toString() {
                return "ClaimsButtonConfiguration{" + "slot=" + slot + ", button=" + button + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ClaimsButtonConfiguration that)) return false;
                return slot == that.slot && Objects.equals(button, that.button);
            }

            @Override
            public int hashCode() {
                return Objects.hash(slot, button);
            }
        }
    }

    public static final class ToggleButtonConfiguration {

        private final int slot;
        private final MenuButtonConfiguration button;

        private ToggleButtonConfiguration(int slot, MenuButtonConfiguration button) {
            this.slot = slot;
            this.button = button != null ? button : DEFAULT_BROWSER_FILLER_BUTTON;
        }

        public static ToggleButtonConfiguration from(ConfigurationSection section, int inventorySize,
                ToggleButtonConfiguration fallback) {
            ToggleButtonConfiguration fallbackConfig = fallback != null ? fallback : DEFAULT_BROWSER_LISTINGS_TOGGLE;
            if (section == null) {
                return sanitizeToggle(fallbackConfig, inventorySize, fallbackConfig);
            }
            int slot = sanitizeSlot(section.getInt("slot", fallbackConfig.slot), inventorySize, fallbackConfig.slot);
            MenuButtonConfiguration button = parseMenuButton(section, fallbackConfig.button);
            return new ToggleButtonConfiguration(slot, button);
        }

        public int slot() {
            return slot;
        }

        public MenuButtonConfiguration button() {
            return button;
        }

        @Override
        public String toString() {
            return "ToggleButtonConfiguration{"
                    + "slot=" + slot
                    + ", button=" + button
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ToggleButtonConfiguration that)) {
                return false;
            }
            return slot == that.slot && Objects.equals(button, that.button);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slot, button);
        }

    }

    public static final class ConfirmMenuConfiguration {

        private final String title;
        private final int size;
        private final MenuButtonConfiguration filler;
        private final ButtonConfiguration confirmButton;
        private final int listingSlot;
        private final ButtonConfiguration cancelButton;

        private ConfirmMenuConfiguration(String title, int size, MenuButtonConfiguration filler,
                ButtonConfiguration confirmButton, int listingSlot, ButtonConfiguration cancelButton) {
            this.title = title != null ? title : DEFAULT_CONFIRM_TITLE;
            int sanitizedSize = sanitizeInventorySize(size, DEFAULT_CONFIRM_SIZE);
            this.size = sanitizedSize;
            this.filler = filler != null ? filler : DEFAULT_CONFIRM_FILLER_BUTTON;
            this.confirmButton = sanitizeButton(confirmButton, sanitizedSize, DEFAULT_CONFIRM_BUTTON);
            this.listingSlot = sanitizeSlot(listingSlot, sanitizedSize, DEFAULT_LISTING_SLOT);
            this.cancelButton = sanitizeButton(cancelButton, sanitizedSize, DEFAULT_CANCEL_BUTTON);
        }

        public static ConfirmMenuConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return DEFAULT_CONFIRM;
            }
            String title = section.getString("title", DEFAULT_CONFIRM.title);
            int requestedSize = section.getInt("size", DEFAULT_CONFIRM.size);
            int size = sanitizeInventorySize(requestedSize, DEFAULT_CONFIRM.size);
            MenuButtonConfiguration filler = parseMenuButton(section.getConfigurationSection("filler"),
                    DEFAULT_CONFIRM.filler);
            ConfigurationSection buttonsSection = section.getConfigurationSection("buttons");
            ConfigurationSection confirmSection = buttonsSection != null
                    ? buttonsSection.getConfigurationSection("confirm")
                    : null;
            ButtonConfiguration confirmButton = ButtonConfiguration.from(confirmSection, size,
                    DEFAULT_CONFIRM.confirmButton);
            int listingSlot = sanitizeSlot(section.getInt("listing-slot", DEFAULT_CONFIRM.listingSlot), size,
                    DEFAULT_CONFIRM.listingSlot);
            ConfigurationSection cancelSection = buttonsSection != null
                    ? buttonsSection.getConfigurationSection("cancel")
                    : null;
            ButtonConfiguration cancelButton = ButtonConfiguration.from(cancelSection, size,
                    DEFAULT_CONFIRM.cancelButton);
            if (confirmSection == null && section.contains("confirm-slot")) {
                confirmButton = confirmButton.withSlot(section.getInt("confirm-slot", confirmButton.slot()), size);
            }
            if (cancelSection == null && section.contains("cancel-slot")) {
                cancelButton = cancelButton.withSlot(section.getInt("cancel-slot", cancelButton.slot()), size);
            }
            return new ConfirmMenuConfiguration(title, size, filler, confirmButton, listingSlot, cancelButton);
        }

        public String title() {
            return title;
        }

        public int size() {
            return size;
        }

        public MenuButtonConfiguration filler() {
            return filler;
        }

        public ButtonConfiguration confirmButton() {
            return confirmButton;
        }

        public int listingSlot() {
            return listingSlot;
        }

        public ButtonConfiguration cancelButton() {
            return cancelButton;
        }

        @Override
        public String toString() {
            return "ConfirmMenuConfiguration{"
                    + "title='" + title + '\''
                    + ", size=" + size
                    + ", filler=" + filler
                    + ", confirmButton=" + confirmButton
                    + ", listingSlot=" + listingSlot
                    + ", cancelButton=" + cancelButton
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConfirmMenuConfiguration that)) {
                return false;
            }
            return size == that.size
                    && listingSlot == that.listingSlot
                    && Objects.equals(title, that.title)
                    && Objects.equals(filler, that.filler)
                    && Objects.equals(confirmButton, that.confirmButton)
                    && Objects.equals(cancelButton, that.cancelButton);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, size, filler, confirmButton, listingSlot, cancelButton);
        }

        public static final class ButtonConfiguration {

            private final int slot;
            private final MenuButtonConfiguration button;

            private ButtonConfiguration(int slot, MenuButtonConfiguration button) {
                this.slot = slot;
                this.button = button != null ? button : DEFAULT_CONFIRM_FILLER_BUTTON;
            }

            public static ButtonConfiguration from(ConfigurationSection section, int size,
                    ButtonConfiguration fallback) {
                ButtonConfiguration fallbackConfig = fallback != null ? fallback : DEFAULT_CONFIRM_BUTTON;
                if (section == null) {
                    return sanitizeButton(fallbackConfig, size, fallbackConfig);
                }
                int slot = sanitizeSlot(section.getInt("slot", fallbackConfig.slot), size, fallbackConfig.slot);
                MenuButtonConfiguration button = parseMenuButton(section, fallbackConfig.button);
                return new ButtonConfiguration(slot, button);
            }

            private ButtonConfiguration withSlot(int newSlot, int size) {
                int sanitizedSlot = sanitizeSlot(newSlot, size, slot);
                return new ButtonConfiguration(sanitizedSlot, button);
            }

            public int slot() {
                return slot;
            }

            public MenuButtonConfiguration button() {
                return button;
            }

            @Override
            public String toString() {
                return "ButtonConfiguration{"
                        + "slot=" + slot
                        + ", button=" + button
                        + '}';
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof ButtonConfiguration that)) {
                    return false;
                }
                return slot == that.slot && Objects.equals(button, that.button);
            }

            @Override
            public int hashCode() {
                return Objects.hash(slot, button);
            }
        }
    }

    public static final class MenuButtonConfiguration {

        private final Material material;
        private final String displayName;
        private final List<String> lore;

        private MenuButtonConfiguration(Material material, String displayName) {
            this(material, displayName, List.of());
        }

        private MenuButtonConfiguration(Material material, String displayName, List<String> lore) {
            this.material = material != null ? material : Material.GRAY_STAINED_GLASS_PANE;
            this.displayName = displayName != null ? displayName : "";
            this.lore = lore != null ? List.copyOf(lore) : List.of();
        }

        public Material material() {
            return material;
        }

        public String displayName() {
            return displayName;
        }

        public List<String> lore() {
            return lore;
        }

        @Override
        public String toString() {
            return "MenuButtonConfiguration{"
                    + "material=" + material
                    + ", displayName='" + displayName + '\''
                    + ", lore=" + lore
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MenuButtonConfiguration that)) {
                return false;
            }
            return material == that.material
                    && Objects.equals(displayName, that.displayName)
                    && Objects.equals(lore, that.lore);
        }

        @Override
        public int hashCode() {
            return Objects.hash(material, displayName, lore);
        }
    }
}
