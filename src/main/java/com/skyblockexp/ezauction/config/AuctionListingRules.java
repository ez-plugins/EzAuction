package com.skyblockexp.ezauction.config;

import java.time.Duration;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Listing related configuration such as default duration and pricing rules.
 */
public final class AuctionListingRules {

    private static final Duration DEFAULT_DEFAULT_DURATION = Duration.ofHours(24);
    private static final Duration DEFAULT_MAX_DURATION = Duration.ofHours(72);
    private static final double DEFAULT_MINIMUM_PRICE = 0.01D;
    private static final double DEFAULT_DEPOSIT_PERCENT = 0.0D;

    private final Duration defaultDuration;
    private final Duration maxDuration;
    private final double minimumPrice;
    private final double depositPercent;

    public AuctionListingRules(Duration defaultDuration, Duration maxDuration, double minimumPrice, double depositPercent) {
        Duration resolvedDefault = sanitizeDuration(defaultDuration, DEFAULT_DEFAULT_DURATION);
        Duration resolvedMax = sanitizeDuration(maxDuration, DEFAULT_MAX_DURATION);
        if (resolvedDefault.compareTo(resolvedMax) > 0) {
            resolvedDefault = resolvedMax;
        }
        this.defaultDuration = resolvedDefault;
        this.maxDuration = resolvedMax;
        this.minimumPrice = Math.max(0.0D, minimumPrice);
        this.depositPercent = clampPercent(depositPercent);
    }

    private static Duration sanitizeDuration(Duration duration, Duration fallback) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return fallback;
        }
        return duration;
    }

    private static double clampPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent)) {
            return DEFAULT_DEPOSIT_PERCENT;
        }
        if (percent < 0.0D) {
            return 0.0D;
        }
        if (percent > 100.0D) {
            return 100.0D;
        }
        return percent;
    }

    public Duration defaultDuration() {
        return defaultDuration;
    }

    public Duration maxDuration() {
        return maxDuration;
    }

    public double minimumPrice() {
        return minimumPrice;
    }

    public double depositPercent() {
        return depositPercent;
    }

    public double depositFraction() {
        return depositPercent / 100.0D;
    }

    public double depositAmount(double price) {
        double normalizedPrice = Math.max(0.0D, price);
        return normalizedPrice * depositFraction();
    }

    public Duration clampDuration(Duration requested) {
        Duration sanitized = requested;
        if (sanitized == null || sanitized.isNegative() || sanitized.isZero()) {
            sanitized = defaultDuration;
        }
        if (sanitized.compareTo(maxDuration) > 0) {
            sanitized = maxDuration;
        }
        return sanitized;
    }

    public static AuctionListingRules defaults() {
        return new AuctionListingRules(DEFAULT_DEFAULT_DURATION, DEFAULT_MAX_DURATION, DEFAULT_MINIMUM_PRICE,
                DEFAULT_DEPOSIT_PERCENT);
    }

    public static AuctionListingRules from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        Duration defaultDuration = hoursToDuration(section.getDouble("default-duration-hours",
                DEFAULT_DEFAULT_DURATION.toMinutes() / 60.0D), DEFAULT_DEFAULT_DURATION);
        Duration maxDuration = hoursToDuration(section.getDouble("max-duration-hours",
                DEFAULT_MAX_DURATION.toMinutes() / 60.0D), DEFAULT_MAX_DURATION);
        double minimumPrice = section.getDouble("minimum-price", DEFAULT_MINIMUM_PRICE);
        double depositPercent = section.getDouble("listing-deposit-percent", DEFAULT_DEPOSIT_PERCENT);
        return new AuctionListingRules(defaultDuration, maxDuration, minimumPrice, depositPercent);
    }

    private static Duration hoursToDuration(double hours, Duration fallback) {
        double sanitized = Double.isFinite(hours) ? hours : fallback.toMinutes() / 60.0D;
        if (sanitized <= 0.0D) {
            return fallback;
        }
        long minutes = Math.max(1L, Math.round(sanitized * 60.0D));
        try {
            return Duration.ofMinutes(minutes);
        } catch (ArithmeticException ex) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return "AuctionListingRules{"
                + "defaultDuration=" + defaultDuration
                + ", maxDuration=" + maxDuration
                + ", minimumPrice=" + minimumPrice
                + ", depositPercent=" + depositPercent
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionListingRules that)) {
            return false;
        }
        return Double.compare(that.minimumPrice, minimumPrice) == 0
                && Double.compare(that.depositPercent, depositPercent) == 0
                && Objects.equals(defaultDuration, that.defaultDuration)
                && Objects.equals(maxDuration, that.maxDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultDuration, maxDuration, minimumPrice, depositPercent);
    }
}
