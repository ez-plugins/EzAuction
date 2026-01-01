package com.skyblockexp.ezauction.config;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * Configuration describing how item value estimates should be displayed in the auction GUI.
 */
public final class AuctionValueConfiguration {

    private static final boolean DEFAULT_ENABLED = false;
    private static final String DEFAULT_FORMAT = "&7Value: &6{value}";
    private static final Mode DEFAULT_MODE = Mode.CONFIGURED;
    private static final ShopPriceConfiguration.EnabledState DEFAULT_SHOP_PRICE_STATE = ShopPriceConfiguration.EnabledState.AUTO;
    private static final String DEFAULT_SHOP_PRICE_FORMAT = "&7Shop Price: &6{value}";
    private static final Mode DEFAULT_SHOP_PRICE_MODE = Mode.EZSHOPS_BUY;

    private final boolean enabled;
    private final String format;
    private final Map<String, Double> materialValues;
    private final Mode mode;
    private final ShopPriceConfiguration shopPriceConfiguration;

    public AuctionValueConfiguration(boolean enabled, String format, Map<String, Double> materialValues, Mode mode,
            ShopPriceConfiguration shopPriceConfiguration) {
        this.enabled = enabled;
        this.format = format != null ? format : DEFAULT_FORMAT;
        if (materialValues == null || materialValues.isEmpty()) {
            this.materialValues = Collections.emptyMap();
        } else {
            this.materialValues = Collections.unmodifiableMap(new HashMap<>(materialValues));
        }
        this.mode = mode != null ? mode : DEFAULT_MODE;
        this.shopPriceConfiguration = shopPriceConfiguration != null
                ? shopPriceConfiguration
                : ShopPriceConfiguration.defaults();
    }

    public boolean enabled() {
        return enabled;
    }

    public String format() {
        return format;
    }

    public Map<String, Double> materialValues() {
        return materialValues;
    }

    public Mode mode() {
        return mode;
    }

    public ShopPriceConfiguration shopPriceConfiguration() {
        return shopPriceConfiguration;
    }

    public OptionalDouble estimate(ItemStack item) {
        if (!enabled || item == null || mode != Mode.CONFIGURED) {
            return OptionalDouble.empty();
        }
        Material type = item.getType();
        if (type == null || type == Material.AIR) {
            return OptionalDouble.empty();
        }
        Double value = null;
        String materialKey = resolveMaterialKey(type);
        if (materialKey != null) {
            value = materialValues.get(normalizeKey(materialKey));
        }
        if (value == null) {
            value = materialValues.get(normalizeKey(type.name()));
        }
        if (value == null) {
            return OptionalDouble.empty();
        }
        double normalized = value;
        if (Double.isNaN(normalized) || Double.isInfinite(normalized)) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.max(0.0D, normalized));
    }

    public static AuctionValueConfiguration defaults() {
        return new AuctionValueConfiguration(DEFAULT_ENABLED, DEFAULT_FORMAT, Collections.emptyMap(), DEFAULT_MODE,
                ShopPriceConfiguration.defaults());
    }

    public static AuctionValueConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        boolean enabled = section.getBoolean("enabled", DEFAULT_ENABLED);
        String format = section.getString("format", DEFAULT_FORMAT);
        Map<String, Double> values = new HashMap<>();
        ConfigurationSection materials = section.getConfigurationSection("materials");
        if (materials != null) {
            for (String key : materials.getKeys(false)) {
                if (key == null || key.trim().isEmpty()) {
                    continue;
                }
                double raw = materials.getDouble(key);
                if (Double.isNaN(raw) || Double.isInfinite(raw)) {
                    continue;
                }
                values.put(normalizeKey(key), raw);
            }
        }
        Mode mode = parseMode(section.getString("mode"));
        ShopPriceConfiguration shopPriceConfiguration = ShopPriceConfiguration
                .from(section.getConfigurationSection("shop-price"));
        return new AuctionValueConfiguration(enabled, format, values, mode, shopPriceConfiguration);
    }

    @Override
    public String toString() {
        return "AuctionValueConfiguration{"
                + "enabled=" + enabled
                + ", format='" + format + '\''
                + ", materialValues=" + materialValues
                + ", mode=" + mode
                + ", shopPriceConfiguration=" + shopPriceConfiguration
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionValueConfiguration that)) {
            return false;
        }
        return enabled == that.enabled
                && Objects.equals(format, that.format)
                && Objects.equals(materialValues, that.materialValues)
                && mode == that.mode
                && Objects.equals(shopPriceConfiguration, that.shopPriceConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, format, materialValues, mode, shopPriceConfiguration);
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ENGLISH);
    }

    private static String resolveMaterialKey(Material material) {
        if (material == null) {
            return null;
        }
        try {
            Method getKey = Material.class.getMethod("getKey");
            Object key = getKey.invoke(material);
            return key != null ? key.toString() : null;
        } catch (NoSuchMethodException ex) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static Mode parseMode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_MODE;
        }
        String normalized = value.trim().toUpperCase(Locale.ENGLISH)
                .replace('-', '_')
                .replace(' ', '_');
        switch (normalized) {
            case "EZSHOPS_BUY":
            case "SHOP_BUY":
            case "EZSHOPSBUY":
                return Mode.EZSHOPS_BUY;
            case "EZSHOPS_SELL":
            case "SHOP_SELL":
            case "EZSHOPSSELL":
                return Mode.EZSHOPS_SELL;
            default:
                return DEFAULT_MODE;
        }
    }

    private static Mode parseShopPriceMode(String value) {
        Mode parsed = parseMode(value);
        if (parsed == Mode.CONFIGURED) {
            return DEFAULT_SHOP_PRICE_MODE;
        }
        return parsed;
    }

    /**
     * Describes the source used when displaying value estimates.
     */
    public enum Mode {
        CONFIGURED,
        EZSHOPS_BUY,
        EZSHOPS_SELL
    }

    /**
     * Settings that control optional EzShops shop price overlays.
     */
    public static final class ShopPriceConfiguration {

        private final EnabledState enabledState;
        private final String format;
        private final Mode mode;

        private ShopPriceConfiguration(EnabledState enabledState, String format, Mode mode) {
            this.enabledState = enabledState != null ? enabledState : DEFAULT_SHOP_PRICE_STATE;
            this.format = format != null ? format : DEFAULT_SHOP_PRICE_FORMAT;
            this.mode = mode != null ? mode : DEFAULT_SHOP_PRICE_MODE;
        }

        public boolean enabled() {
            return enabledState == EnabledState.ENABLED;
        }

        public boolean autoDetect() {
            return enabledState == EnabledState.AUTO;
        }

        public EnabledState enabledState() {
            return enabledState;
        }

        public String format() {
            return format;
        }

        public Mode mode() {
            return mode;
        }

        public static ShopPriceConfiguration defaults() {
            return new ShopPriceConfiguration(
                    DEFAULT_SHOP_PRICE_STATE, DEFAULT_SHOP_PRICE_FORMAT, DEFAULT_SHOP_PRICE_MODE);
        }

        public static ShopPriceConfiguration from(ConfigurationSection section) {
            if (section == null) {
                return defaults();
            }
            EnabledState enabledState = DEFAULT_SHOP_PRICE_STATE;
            if (section.contains("enabled")) {
                enabledState = section.getBoolean("enabled") ? EnabledState.ENABLED : EnabledState.DISABLED;
            }
            String format = section.getString("format", DEFAULT_SHOP_PRICE_FORMAT);
            Mode mode = parseShopPriceMode(section.getString("mode"));
            return new ShopPriceConfiguration(enabledState, format, mode);
        }

        @Override
        public String toString() {
            return "ShopPriceConfiguration{"
                    + "enabledState=" + enabledState
                    + ", format='" + format + '\''
                    + ", mode=" + mode
                    + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ShopPriceConfiguration that)) {
                return false;
            }
            return enabledState == that.enabledState
                    && Objects.equals(format, that.format)
                    && mode == that.mode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabledState, format, mode);
        }

        public enum EnabledState {
            AUTO,
            ENABLED,
            DISABLED
        }
    }
}
