package com.skyblockexp.ezauction.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration describing interactive auction menu defaults, such as price and quantity adjustments.
 */
public final class AuctionMenuInteractionConfiguration {

    private static final SellMenuInteractionConfiguration DEFAULT_SELL = SellMenuInteractionConfiguration.defaults();
    private static final OrderMenuInteractionConfiguration DEFAULT_ORDERS = OrderMenuInteractionConfiguration.defaults();

    private final SellMenuInteractionConfiguration sellMenu;
    private final OrderMenuInteractionConfiguration orderMenu;

    public AuctionMenuInteractionConfiguration(SellMenuInteractionConfiguration sellMenu,
            OrderMenuInteractionConfiguration orderMenu) {
        this.sellMenu = sellMenu != null ? sellMenu : DEFAULT_SELL;
        this.orderMenu = orderMenu != null ? orderMenu : DEFAULT_ORDERS;
    }

    public SellMenuInteractionConfiguration sellMenu() {
        return sellMenu;
    }

    public OrderMenuInteractionConfiguration orderMenu() {
        return orderMenu;
    }

    public static AuctionMenuInteractionConfiguration defaults() {
        return new AuctionMenuInteractionConfiguration(DEFAULT_SELL, DEFAULT_ORDERS);
    }

    public static AuctionMenuInteractionConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        SellMenuInteractionConfiguration sell = SellMenuInteractionConfiguration
                .from(section.getConfigurationSection("sell"));
        OrderMenuInteractionConfiguration orders = OrderMenuInteractionConfiguration
                .from(section.getConfigurationSection("orders"));
        return new AuctionMenuInteractionConfiguration(sell, orders);
    }

    @Override
    public String toString() {
        return "AuctionMenuInteractionConfiguration{"
                + "sellMenu=" + sellMenu
                + ", orderMenu=" + orderMenu
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionMenuInteractionConfiguration that)) {
            return false;
        }
        return Objects.equals(sellMenu, that.sellMenu)
                && Objects.equals(orderMenu, that.orderMenu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellMenu, orderMenu);
    }

    private static Double parseDouble(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            if (Double.isFinite(parsed)) {
                return parsed;
            }
            return null;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                double parsed = Double.parseDouble(trimmed);
                if (Double.isFinite(parsed)) {
                    return parsed;
                }
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private static Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            if (!Double.isFinite(parsed)) {
                return null;
            }
            if (Math.floor(parsed) != parsed) {
                return null;
            }
            if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
                return null;
            }
            return (int) parsed;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException first) {
                try {
                    double parsed = Double.parseDouble(trimmed);
                    if (!Double.isFinite(parsed) || Math.floor(parsed) != parsed) {
                        return null;
                    }
                    if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
                        return null;
                    }
                    return (int) parsed;
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
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
        if (value < 0 || value >= size) {
            return fallback;
        }
        return value;
    }

    private static int[] sanitizeSlots(List<?> rawValues, int size, int[] fallback) {
        if (rawValues == null || rawValues.isEmpty()) {
            return fallback != null ? fallback.clone() : new int[0];
        }
        List<Integer> parsed = new ArrayList<>();
        for (Object rawValue : rawValues) {
            Integer parsedValue = parseInteger(rawValue);
            if (parsedValue == null) {
                continue;
            }
            int slot = parsedValue;
            if (slot < 0 || slot >= size) {
                continue;
            }
            if (!parsed.contains(slot)) {
                parsed.add(slot);
            }
        }
        if (parsed.isEmpty()) {
            return fallback != null ? fallback.clone() : new int[0];
        }
        int[] result = new int[parsed.size()];
        for (int i = 0; i < parsed.size(); i++) {
            result[i] = parsed.get(i);
        }
        return result;
    }

    private static int[] sanitizeSlotsArray(int[] values, int size, int[] fallback) {
        if (values == null || values.length == 0) {
            return fallback != null ? fallback.clone() : new int[0];
        }
        List<Integer> raw = new ArrayList<>(values.length);
        for (int value : values) {
            raw.add(value);
        }
        return sanitizeSlots(raw, size, fallback);
    }

    public static final class MenuButtonDefinition {


        private final Material material;
        private final String displayName;

        private MenuButtonDefinition(Material material, String displayName) {
            this.material = material != null ? material : Material.GRAY_STAINED_GLASS_PANE;
            this.displayName = displayName != null ? displayName : "";
        }

        public static MenuButtonDefinition of(Material material, String displayName) {
            return new MenuButtonDefinition(material != null ? material : Material.GRAY_STAINED_GLASS_PANE, displayName);
        }

        public static MenuButtonDefinition from(ConfigurationSection section, MenuButtonDefinition fallback) {
            if (section == null) {
                return fallback != null ? fallback : new MenuButtonDefinition(null, null);
            }
            MenuButtonDefinition effectiveFallback = fallback != null ? fallback : new MenuButtonDefinition(null, null);
            Material material = parseMaterial(section.getString("material"), effectiveFallback.material);
            String displayName = section.getString("display-name", effectiveFallback.displayName);
            return new MenuButtonDefinition(material, displayName);
        }

        private static MenuButtonDefinition normalize(MenuButtonDefinition candidate, MenuButtonDefinition fallback) {
            if (candidate == null) {
                return fallback != null ? fallback : new MenuButtonDefinition(null, null);
            }
            MenuButtonDefinition effectiveFallback = fallback != null ? fallback : new MenuButtonDefinition(null, null);
            Material material = candidate.material != null ? candidate.material : effectiveFallback.material;
            String displayName = candidate.displayName != null ? candidate.displayName : effectiveFallback.displayName;
            return new MenuButtonDefinition(material, displayName);
        }

        public Material material() {
            return material;
        }

        public String displayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return "MenuButtonDefinition{"
                    + "material=" + material
                    + ", displayName='" + displayName + '\''
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MenuButtonDefinition that)) {
                return false;
            }
            return material == that.material
                    && Objects.equals(displayName, that.displayName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(material, displayName);
        }
    }

    public static final class ButtonLayoutConfiguration {

        private final int slot;
        private final MenuButtonDefinition button;

        private ButtonLayoutConfiguration(int slot, MenuButtonDefinition button) {
            this.slot = slot;
            this.button = button != null ? button : MenuButtonDefinition.of(Material.GRAY_STAINED_GLASS_PANE, "");
        }

        public static ButtonLayoutConfiguration of(int slot, MenuButtonDefinition button) {
            return new ButtonLayoutConfiguration(slot, button);
        }

        public static ButtonLayoutConfiguration from(ConfigurationSection section,
                ButtonLayoutConfiguration fallback, int size) {
            if (section == null) {
                return fallback;
            }
            ButtonLayoutConfiguration effectiveFallback = fallback != null ? fallback
                    : ButtonLayoutConfiguration.of(0, MenuButtonDefinition.of(Material.GRAY_STAINED_GLASS_PANE, ""));
            int slot = sanitizeSlot(section.getInt("slot", effectiveFallback.slot), size, effectiveFallback.slot);
            MenuButtonDefinition button = MenuButtonDefinition.from(section, effectiveFallback.button);
            return new ButtonLayoutConfiguration(slot, button);
        }

        private static ButtonLayoutConfiguration normalize(ButtonLayoutConfiguration candidate,
                ButtonLayoutConfiguration fallback, int size) {
            if (candidate == null) {
                return fallback;
            }
            ButtonLayoutConfiguration effectiveFallback = fallback != null ? fallback
                    : ButtonLayoutConfiguration.of(0, MenuButtonDefinition.of(Material.GRAY_STAINED_GLASS_PANE, ""));
            int slot = sanitizeSlot(candidate.slot, size, effectiveFallback.slot);
            MenuButtonDefinition button = MenuButtonDefinition.normalize(candidate.button, effectiveFallback.button);
            return new ButtonLayoutConfiguration(slot, button);
        }

        public int slot() {
            return slot;
        }

        public MenuButtonDefinition button() {
            return button;
        }

        @Override
        public String toString() {
            return "ButtonLayoutConfiguration{"
                    + "slot=" + slot
                    + ", button=" + button
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ButtonLayoutConfiguration that)) {
                return false;
            }
            return slot == that.slot
                    && Objects.equals(button, that.button);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slot, button);
        }
    }

    public static final class SellMenuLayoutConfiguration {

        private static final String DEFAULT_TITLE = "&2Create Auction Listing";
        private static final int DEFAULT_SIZE = 27;
        private static final MenuButtonDefinition DEFAULT_FILLER =
                MenuButtonDefinition.of(Material.GRAY_STAINED_GLASS_PANE, "&8 ");
        private static final int DEFAULT_LISTING_SLOT = 11;
        private static final ButtonLayoutConfiguration DEFAULT_PRICE_DISPLAY =
                ButtonLayoutConfiguration.of(13, MenuButtonDefinition.of(Material.SUNFLOWER, "&6Listing Price"));
        private static final ButtonLayoutConfiguration DEFAULT_DURATION_DISPLAY =
                ButtonLayoutConfiguration.of(15, MenuButtonDefinition.of(Material.CLOCK, "&eListing Duration"));
        private static final ButtonLayoutConfiguration DEFAULT_CUSTOM_PRICE =
                ButtonLayoutConfiguration.of(16, MenuButtonDefinition.of(Material.WRITABLE_BOOK, "&bCustom Price"));
        private static final ButtonLayoutConfiguration DEFAULT_CONFIRM_BUTTON =
                ButtonLayoutConfiguration.of(22, MenuButtonDefinition.of(Material.LIME_CONCRETE, "&aCreate Listing"));
        private static final ButtonLayoutConfiguration DEFAULT_CANCEL_BUTTON =
                ButtonLayoutConfiguration.of(24, MenuButtonDefinition.of(Material.RED_CONCRETE, "&cCancel"));
        private static final int[] DEFAULT_PRICE_ADJUST_SLOTS = new int[] {0, 1, 2, 3, 5, 6, 7, 8};
        private static final SellMenuLayoutConfiguration DEFAULT = new SellMenuLayoutConfiguration(
                DEFAULT_TITLE,
                DEFAULT_SIZE,
                DEFAULT_FILLER,
                DEFAULT_LISTING_SLOT,
                DEFAULT_PRICE_DISPLAY,
                DEFAULT_DURATION_DISPLAY,
                DEFAULT_CUSTOM_PRICE,
                DEFAULT_CONFIRM_BUTTON,
                DEFAULT_CANCEL_BUTTON,
                DEFAULT_PRICE_ADJUST_SLOTS);

        private final String title;
        private final int size;
        private final MenuButtonDefinition filler;
        private final int listingSlot;
        private final ButtonLayoutConfiguration priceDisplay;
        private final ButtonLayoutConfiguration durationDisplay;
        private final ButtonLayoutConfiguration customPrice;
        private final ButtonLayoutConfiguration confirmButton;
        private final ButtonLayoutConfiguration cancelButton;
        private final int[] priceAdjustmentSlots;

        private SellMenuLayoutConfiguration(String title, int size, MenuButtonDefinition filler, int listingSlot,
                ButtonLayoutConfiguration priceDisplay, ButtonLayoutConfiguration durationDisplay,
                ButtonLayoutConfiguration customPrice, ButtonLayoutConfiguration confirmButton,
                ButtonLayoutConfiguration cancelButton, int[] priceAdjustmentSlots) {
            this.title = title != null ? title : DEFAULT_TITLE;
            int sanitizedSize = sanitizeInventorySize(size, DEFAULT_SIZE);
            this.size = sanitizedSize;
            this.filler = MenuButtonDefinition.normalize(filler, DEFAULT_FILLER);
            this.listingSlot = sanitizeSlot(listingSlot, sanitizedSize, DEFAULT_LISTING_SLOT);
            this.priceDisplay = ButtonLayoutConfiguration.normalize(priceDisplay, DEFAULT_PRICE_DISPLAY, sanitizedSize);
            this.durationDisplay = ButtonLayoutConfiguration.normalize(durationDisplay, DEFAULT_DURATION_DISPLAY,
                    sanitizedSize);
            this.customPrice = ButtonLayoutConfiguration.normalize(customPrice, DEFAULT_CUSTOM_PRICE, sanitizedSize);
            this.confirmButton = ButtonLayoutConfiguration.normalize(confirmButton, DEFAULT_CONFIRM_BUTTON,
                    sanitizedSize);
            this.cancelButton = ButtonLayoutConfiguration.normalize(cancelButton, DEFAULT_CANCEL_BUTTON, sanitizedSize);
            this.priceAdjustmentSlots = sanitizeSlotsArray(priceAdjustmentSlots, sanitizedSize,
                    DEFAULT_PRICE_ADJUST_SLOTS);
        }

        public static SellMenuLayoutConfiguration defaults() {
            return DEFAULT;
        }

        public static SellMenuLayoutConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return DEFAULT;
            }
            int requestedSize = section.getInt("size", DEFAULT_SIZE);
            int size = sanitizeInventorySize(requestedSize, DEFAULT_SIZE);
            String title = section.getString("title", DEFAULT_TITLE);
            MenuButtonDefinition filler = MenuButtonDefinition.from(section.getConfigurationSection("filler"),
                    DEFAULT_FILLER);
            int listingSlot = sanitizeSlot(section.getInt("listing-slot", DEFAULT_LISTING_SLOT), size,
                    DEFAULT_LISTING_SLOT);
            ButtonLayoutConfiguration priceDisplay = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("price-display"), DEFAULT_PRICE_DISPLAY, size);
            ButtonLayoutConfiguration durationDisplay = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("duration-display"), DEFAULT_DURATION_DISPLAY, size);
            ButtonLayoutConfiguration customPrice = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("custom-price"), DEFAULT_CUSTOM_PRICE, size);
            ButtonLayoutConfiguration confirm = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("confirm"), DEFAULT_CONFIRM_BUTTON, size);
            ButtonLayoutConfiguration cancel = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("cancel"), DEFAULT_CANCEL_BUTTON, size);
            int[] priceAdjustSlots = sanitizeSlots(section.getList("price-adjust.slots"), size,
                    DEFAULT_PRICE_ADJUST_SLOTS);
            return new SellMenuLayoutConfiguration(title, size, filler, listingSlot, priceDisplay, durationDisplay,
                    customPrice, confirm, cancel, priceAdjustSlots);
        }

        public String title() {
            return title;
        }

        public int size() {
            return size;
        }

        public MenuButtonDefinition filler() {
            return filler;
        }

        public int listingSlot() {
            return listingSlot;
        }

        public ButtonLayoutConfiguration priceDisplay() {
            return priceDisplay;
        }

        public ButtonLayoutConfiguration durationDisplay() {
            return durationDisplay;
        }

        public ButtonLayoutConfiguration customPrice() {
            return customPrice;
        }

        public ButtonLayoutConfiguration confirmButton() {
            return confirmButton;
        }

        public ButtonLayoutConfiguration cancelButton() {
            return cancelButton;
        }

        public int[] priceAdjustmentSlots() {
            return priceAdjustmentSlots.clone();
        }

        @Override
        public String toString() {
            return "SellMenuLayoutConfiguration{"
                    + "title='" + title + '\''
                    + ", size=" + size
                    + ", filler=" + filler
                    + ", listingSlot=" + listingSlot
                    + ", priceDisplay=" + priceDisplay
                    + ", durationDisplay=" + durationDisplay
                    + ", customPrice=" + customPrice
                    + ", confirmButton=" + confirmButton
                    + ", cancelButton=" + cancelButton
                    + ", priceAdjustmentSlots=" + Arrays.toString(priceAdjustmentSlots)
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SellMenuLayoutConfiguration that)) {
                return false;
            }
            return size == that.size
                    && listingSlot == that.listingSlot
                    && Objects.equals(title, that.title)
                    && Objects.equals(filler, that.filler)
                    && Objects.equals(priceDisplay, that.priceDisplay)
                    && Objects.equals(durationDisplay, that.durationDisplay)
                    && Objects.equals(customPrice, that.customPrice)
                    && Objects.equals(confirmButton, that.confirmButton)
                    && Objects.equals(cancelButton, that.cancelButton)
                    && Arrays.equals(priceAdjustmentSlots, that.priceAdjustmentSlots);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(title, size, filler, listingSlot, priceDisplay, durationDisplay, customPrice,
                    confirmButton, cancelButton);
            result = 31 * result + Arrays.hashCode(priceAdjustmentSlots);
            return result;
        }
    }

    /**
     * Configuration for the auction sell menu.
     */
    public static final class SellMenuInteractionConfiguration {

        private static final double DEFAULT_PRICE = 100.0D;
        private static final List<Double> DEFAULT_ADJUSTMENTS = List.of(-1000.0D, -100.0D, -10.0D, -1.0D, 1.0D, 10.0D,
                100.0D, 1000.0D);

        private final double defaultPrice;
        private final List<Double> priceAdjustments;
        private final SellMenuLayoutConfiguration layout;

        private SellMenuInteractionConfiguration(double defaultPrice, List<Double> priceAdjustments,
                SellMenuLayoutConfiguration layout) {
            this.defaultPrice = defaultPrice;
            this.priceAdjustments = priceAdjustments;
            this.layout = layout != null ? layout : SellMenuLayoutConfiguration.defaults();
        }

        public double defaultPrice() {
            return defaultPrice;
        }

        public List<Double> priceAdjustments() {
            return priceAdjustments;
        }

        public SellMenuLayoutConfiguration layout() {
            return layout;
        }

        private static SellMenuInteractionConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            double defaultPrice = DEFAULT_PRICE;
            if (section.isSet("default-price")) {
                double candidate = section.getDouble("default-price", DEFAULT_PRICE);
                if (Double.isFinite(candidate) && candidate > 0.0D) {
                    defaultPrice = candidate;
                }
            }
            List<Double> adjustments = sanitizePriceAdjustments(section.getList("price-adjustments"));
            SellMenuLayoutConfiguration layout = SellMenuLayoutConfiguration
                    .from(section.getConfigurationSection("layout"));
            return new SellMenuInteractionConfiguration(defaultPrice, adjustments, layout);
        }

        private static List<Double> sanitizePriceAdjustments(List<?> rawValues) {
            if (rawValues == null || rawValues.isEmpty()) {
                return DEFAULT_ADJUSTMENTS;
            }
            List<Double> parsed = new ArrayList<>();
            for (Object rawValue : rawValues) {
                Double parsedValue = parseDouble(rawValue);
                if (parsedValue != null) {
                    parsed.add(parsedValue);
                }
            }
            if (parsed.isEmpty()) {
                return DEFAULT_ADJUSTMENTS;
            }
            return List.copyOf(parsed);
        }

        private static SellMenuInteractionConfiguration defaults() {
            return new SellMenuInteractionConfiguration(DEFAULT_PRICE, DEFAULT_ADJUSTMENTS,
                    SellMenuLayoutConfiguration.defaults());
        }

        @Override
        public String toString() {
            return "SellMenuInteractionConfiguration{"
                    + "defaultPrice=" + defaultPrice
                    + ", priceAdjustments=" + priceAdjustments
                    + ", layout=" + layout
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SellMenuInteractionConfiguration that)) {
                return false;
            }
            return Double.compare(that.defaultPrice, defaultPrice) == 0
                    && Objects.equals(priceAdjustments, that.priceAdjustments)
                    && Objects.equals(layout, that.layout);
        }

        @Override
        public int hashCode() {
            return Objects.hash(defaultPrice, priceAdjustments, layout);
        }
    }

    public static final class OrderMenuLayoutConfiguration {

        private static final String DEFAULT_TITLE = "&2Create Buy Order";
        private static final int DEFAULT_SIZE = 27;
        private static final MenuButtonDefinition DEFAULT_FILLER =
                MenuButtonDefinition.of(Material.GRAY_STAINED_GLASS_PANE, "&8 ");
        private static final int DEFAULT_ITEM_SLOT = 11;
        private static final ButtonLayoutConfiguration DEFAULT_PRICE_DISPLAY =
                ButtonLayoutConfiguration.of(13, MenuButtonDefinition.of(Material.SUNFLOWER, "&6Offer Price"));
        private static final ButtonLayoutConfiguration DEFAULT_QUANTITY_DISPLAY =
                ButtonLayoutConfiguration.of(14, MenuButtonDefinition.of(Material.WRITABLE_BOOK, "&bRequested Quantity"));
        private static final ButtonLayoutConfiguration DEFAULT_DURATION_DISPLAY =
                ButtonLayoutConfiguration.of(15, MenuButtonDefinition.of(Material.CLOCK, "&eOrder Duration"));
        private static final ButtonLayoutConfiguration DEFAULT_CUSTOM_PRICE =
                ButtonLayoutConfiguration.of(16, MenuButtonDefinition.of(Material.PAPER, "&bCustom Price"));
        private static final ButtonLayoutConfiguration DEFAULT_CONFIRM_BUTTON =
                ButtonLayoutConfiguration.of(22, MenuButtonDefinition.of(Material.LIME_CONCRETE, "&aConfirm Order"));
        private static final ButtonLayoutConfiguration DEFAULT_CANCEL_BUTTON =
                ButtonLayoutConfiguration.of(24, MenuButtonDefinition.of(Material.RED_CONCRETE, "&cCancel"));
        private static final int[] DEFAULT_PRICE_ADJUST_SLOTS = new int[] {0, 1, 2, 3, 5, 6, 7, 8};
        private static final int[] DEFAULT_QUANTITY_ADJUST_SLOTS = new int[] {9, 10, 12, 18, 19, 20};
        private static final OrderMenuLayoutConfiguration DEFAULT = new OrderMenuLayoutConfiguration(
                DEFAULT_TITLE,
                DEFAULT_SIZE,
                DEFAULT_FILLER,
                DEFAULT_ITEM_SLOT,
                DEFAULT_PRICE_DISPLAY,
                DEFAULT_QUANTITY_DISPLAY,
                DEFAULT_DURATION_DISPLAY,
                DEFAULT_CUSTOM_PRICE,
                DEFAULT_CONFIRM_BUTTON,
                DEFAULT_CANCEL_BUTTON,
                DEFAULT_PRICE_ADJUST_SLOTS,
                DEFAULT_QUANTITY_ADJUST_SLOTS);

        private final String title;
        private final int size;
        private final MenuButtonDefinition filler;
        private final int itemSlot;
        private final ButtonLayoutConfiguration priceDisplay;
        private final ButtonLayoutConfiguration quantityDisplay;
        private final ButtonLayoutConfiguration durationDisplay;
        private final ButtonLayoutConfiguration customPrice;
        private final ButtonLayoutConfiguration confirmButton;
        private final ButtonLayoutConfiguration cancelButton;
        private final int[] priceAdjustmentSlots;
        private final int[] quantityAdjustmentSlots;

        private OrderMenuLayoutConfiguration(String title, int size, MenuButtonDefinition filler, int itemSlot,
                ButtonLayoutConfiguration priceDisplay, ButtonLayoutConfiguration quantityDisplay,
                ButtonLayoutConfiguration durationDisplay, ButtonLayoutConfiguration customPrice,
                ButtonLayoutConfiguration confirmButton, ButtonLayoutConfiguration cancelButton,
                int[] priceAdjustmentSlots, int[] quantityAdjustmentSlots) {
            this.title = title != null ? title : DEFAULT_TITLE;
            int sanitizedSize = sanitizeInventorySize(size, DEFAULT_SIZE);
            this.size = sanitizedSize;
            this.filler = MenuButtonDefinition.normalize(filler, DEFAULT_FILLER);
            this.itemSlot = sanitizeSlot(itemSlot, sanitizedSize, DEFAULT_ITEM_SLOT);
            this.priceDisplay = ButtonLayoutConfiguration.normalize(priceDisplay, DEFAULT_PRICE_DISPLAY, sanitizedSize);
            this.quantityDisplay = ButtonLayoutConfiguration.normalize(quantityDisplay, DEFAULT_QUANTITY_DISPLAY,
                    sanitizedSize);
            this.durationDisplay = ButtonLayoutConfiguration.normalize(durationDisplay, DEFAULT_DURATION_DISPLAY,
                    sanitizedSize);
            this.customPrice = ButtonLayoutConfiguration.normalize(customPrice, DEFAULT_CUSTOM_PRICE, sanitizedSize);
            this.confirmButton = ButtonLayoutConfiguration.normalize(confirmButton, DEFAULT_CONFIRM_BUTTON,
                    sanitizedSize);
            this.cancelButton = ButtonLayoutConfiguration.normalize(cancelButton, DEFAULT_CANCEL_BUTTON, sanitizedSize);
            this.priceAdjustmentSlots = sanitizeSlotsArray(priceAdjustmentSlots, sanitizedSize,
                    DEFAULT_PRICE_ADJUST_SLOTS);
            this.quantityAdjustmentSlots = sanitizeSlotsArray(quantityAdjustmentSlots, sanitizedSize,
                    DEFAULT_QUANTITY_ADJUST_SLOTS);
        }

        public static OrderMenuLayoutConfiguration defaults() {
            return DEFAULT;
        }

        public static OrderMenuLayoutConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return DEFAULT;
            }
            int requestedSize = section.getInt("size", DEFAULT_SIZE);
            int size = sanitizeInventorySize(requestedSize, DEFAULT_SIZE);
            String title = section.getString("title", DEFAULT_TITLE);
            MenuButtonDefinition filler = MenuButtonDefinition.from(section.getConfigurationSection("filler"),
                    DEFAULT_FILLER);
            int itemSlot = sanitizeSlot(section.getInt("item-slot", DEFAULT_ITEM_SLOT), size, DEFAULT_ITEM_SLOT);
            ButtonLayoutConfiguration priceDisplay = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("price-display"), DEFAULT_PRICE_DISPLAY, size);
            ButtonLayoutConfiguration quantityDisplay = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("quantity-display"), DEFAULT_QUANTITY_DISPLAY, size);
            ButtonLayoutConfiguration durationDisplay = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("duration-display"), DEFAULT_DURATION_DISPLAY, size);
            ButtonLayoutConfiguration customPrice = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("custom-price"), DEFAULT_CUSTOM_PRICE, size);
            ButtonLayoutConfiguration confirm = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("confirm"), DEFAULT_CONFIRM_BUTTON, size);
            ButtonLayoutConfiguration cancel = ButtonLayoutConfiguration.from(
                    section.getConfigurationSection("cancel"), DEFAULT_CANCEL_BUTTON, size);
            int[] priceSlots = sanitizeSlots(section.getList("price-adjust.slots"), size, DEFAULT_PRICE_ADJUST_SLOTS);
            int[] quantitySlots = sanitizeSlots(section.getList("quantity-adjust.slots"), size,
                    DEFAULT_QUANTITY_ADJUST_SLOTS);
            return new OrderMenuLayoutConfiguration(title, size, filler, itemSlot, priceDisplay, quantityDisplay,
                    durationDisplay, customPrice, confirm, cancel, priceSlots, quantitySlots);
        }

        public String title() {
            return title;
        }

        public int size() {
            return size;
        }

        public MenuButtonDefinition filler() {
            return filler;
        }

        public int itemSlot() {
            return itemSlot;
        }

        public ButtonLayoutConfiguration priceDisplay() {
            return priceDisplay;
        }

        public ButtonLayoutConfiguration quantityDisplay() {
            return quantityDisplay;
        }

        public ButtonLayoutConfiguration durationDisplay() {
            return durationDisplay;
        }

        public ButtonLayoutConfiguration customPrice() {
            return customPrice;
        }

        public ButtonLayoutConfiguration confirmButton() {
            return confirmButton;
        }

        public ButtonLayoutConfiguration cancelButton() {
            return cancelButton;
        }

        public int[] priceAdjustmentSlots() {
            return priceAdjustmentSlots.clone();
        }

        public int[] quantityAdjustmentSlots() {
            return quantityAdjustmentSlots.clone();
        }

        @Override
        public String toString() {
            return "OrderMenuLayoutConfiguration{"
                    + "title='" + title + '\''
                    + ", size=" + size
                    + ", filler=" + filler
                    + ", itemSlot=" + itemSlot
                    + ", priceDisplay=" + priceDisplay
                    + ", quantityDisplay=" + quantityDisplay
                    + ", durationDisplay=" + durationDisplay
                    + ", customPrice=" + customPrice
                    + ", confirmButton=" + confirmButton
                    + ", cancelButton=" + cancelButton
                    + ", priceAdjustmentSlots=" + Arrays.toString(priceAdjustmentSlots)
                    + ", quantityAdjustmentSlots=" + Arrays.toString(quantityAdjustmentSlots)
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof OrderMenuLayoutConfiguration that)) {
                return false;
            }
            return size == that.size
                    && itemSlot == that.itemSlot
                    && Objects.equals(title, that.title)
                    && Objects.equals(filler, that.filler)
                    && Objects.equals(priceDisplay, that.priceDisplay)
                    && Objects.equals(quantityDisplay, that.quantityDisplay)
                    && Objects.equals(durationDisplay, that.durationDisplay)
                    && Objects.equals(customPrice, that.customPrice)
                    && Objects.equals(confirmButton, that.confirmButton)
                    && Objects.equals(cancelButton, that.cancelButton)
                    && Arrays.equals(priceAdjustmentSlots, that.priceAdjustmentSlots)
                    && Arrays.equals(quantityAdjustmentSlots, that.quantityAdjustmentSlots);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(title, size, filler, itemSlot, priceDisplay, quantityDisplay, durationDisplay,
                    customPrice, confirmButton, cancelButton);
            result = 31 * result + Arrays.hashCode(priceAdjustmentSlots);
            result = 31 * result + Arrays.hashCode(quantityAdjustmentSlots);
            return result;
        }
    }

    /**
     * Configuration for the auction buy order menu.
     */
    public static final class OrderMenuInteractionConfiguration {

        private static final double DEFAULT_PRICE = 100.0D;
        private static final List<Double> DEFAULT_PRICE_ADJUSTMENTS = List.of(-1000.0D, -100.0D, -10.0D, -1.0D, 1.0D,
                10.0D, 100.0D, 1000.0D);
        private static final List<Integer> DEFAULT_QUANTITY_ADJUSTMENTS = List.of(-16, -8, -1, 1, 8, 16);

        private final double defaultPricePerItem;
        private final List<Double> priceAdjustments;
        private final List<Integer> quantityAdjustments;
        private final OrderMenuLayoutConfiguration layout;

        private OrderMenuInteractionConfiguration(double defaultPricePerItem, List<Double> priceAdjustments,
                List<Integer> quantityAdjustments, OrderMenuLayoutConfiguration layout) {
            this.defaultPricePerItem = defaultPricePerItem;
            this.priceAdjustments = priceAdjustments;
            this.quantityAdjustments = quantityAdjustments;
            this.layout = layout != null ? layout : OrderMenuLayoutConfiguration.defaults();
        }

        public double defaultPricePerItem() {
            return defaultPricePerItem;
        }

        public List<Double> priceAdjustments() {
            return priceAdjustments;
        }

        public List<Integer> quantityAdjustments() {
            return quantityAdjustments;
        }

        public OrderMenuLayoutConfiguration layout() {
            return layout;
        }

        private static OrderMenuInteractionConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            double defaultPrice = DEFAULT_PRICE;
            if (section.isSet("default-price")) {
                double candidate = section.getDouble("default-price", DEFAULT_PRICE);
                if (Double.isFinite(candidate) && candidate > 0.0D) {
                    defaultPrice = candidate;
                }
            }
            List<Double> priceAdjustments = sanitizePriceAdjustments(section.getList("price-adjustments"));
            List<Integer> quantityAdjustments = sanitizeQuantityAdjustments(section.getList("quantity-adjustments"));
            OrderMenuLayoutConfiguration layout = OrderMenuLayoutConfiguration
                    .from(section.getConfigurationSection("layout"));
            return new OrderMenuInteractionConfiguration(defaultPrice, priceAdjustments, quantityAdjustments, layout);
        }

        private static List<Double> sanitizePriceAdjustments(List<?> rawValues) {
            if (rawValues == null || rawValues.isEmpty()) {
                return DEFAULT_PRICE_ADJUSTMENTS;
            }
            List<Double> parsed = new ArrayList<>();
            for (Object rawValue : rawValues) {
                Double parsedValue = parseDouble(rawValue);
                if (parsedValue != null) {
                    parsed.add(parsedValue);
                }
            }
            if (parsed.isEmpty()) {
                return DEFAULT_PRICE_ADJUSTMENTS;
            }
            return List.copyOf(parsed);
        }

        private static List<Integer> sanitizeQuantityAdjustments(List<?> rawValues) {
            if (rawValues == null || rawValues.isEmpty()) {
                return DEFAULT_QUANTITY_ADJUSTMENTS;
            }
            List<Integer> parsed = new ArrayList<>();
            for (Object rawValue : rawValues) {
                Integer parsedValue = parseInteger(rawValue);
                if (parsedValue != null && parsedValue != 0) {
                    parsed.add(parsedValue);
                }
            }
            if (parsed.isEmpty()) {
                return DEFAULT_QUANTITY_ADJUSTMENTS;
            }
            return List.copyOf(parsed);
        }

        private static OrderMenuInteractionConfiguration defaults() {
            return new OrderMenuInteractionConfiguration(DEFAULT_PRICE, DEFAULT_PRICE_ADJUSTMENTS,
                    DEFAULT_QUANTITY_ADJUSTMENTS, OrderMenuLayoutConfiguration.defaults());
        }

        @Override
        public String toString() {
            return "OrderMenuInteractionConfiguration{"
                    + "defaultPricePerItem=" + defaultPricePerItem
                    + ", priceAdjustments=" + priceAdjustments
                    + ", quantityAdjustments=" + quantityAdjustments
                    + ", layout=" + layout
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof OrderMenuInteractionConfiguration that)) {
                return false;
            }
            return Double.compare(that.defaultPricePerItem, defaultPricePerItem) == 0
                    && Objects.equals(priceAdjustments, that.priceAdjustments)
                    && Objects.equals(quantityAdjustments, that.quantityAdjustments)
                    && Objects.equals(layout, that.layout);
        }

        @Override
        public int hashCode() {
            return Objects.hash(defaultPricePerItem, priceAdjustments, quantityAdjustments, layout);
        }
    }
}
