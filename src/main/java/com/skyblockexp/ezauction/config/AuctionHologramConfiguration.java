package com.skyblockexp.ezauction.config;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Parsed configuration describing how auction holograms behave.
 */
public final class AuctionHologramConfiguration {

    private static final boolean DEFAULT_ENABLED = true;
    private static final long DEFAULT_UPDATE_INTERVAL_TICKS = 20L * 5L;
    private static final double DEFAULT_SEARCH_RADIUS = 2.5D;
    private static final double DEFAULT_HEIGHT_OFFSET = 1.75D;
    private static final double DEFAULT_REMOVAL_RADIUS = 10.0D;
    private static final double DEFAULT_MAX_REMOVAL_RADIUS = 64.0D;
    private static final boolean DEFAULT_ALLOW_UNTAGGED_REMOVAL = true;

    private final boolean enabled;
    private final long updateIntervalTicks;
    private final double searchRadius;
    private final double heightOffset;
    private final double removalRadius;
    private final double maxRemovalRadius;
    private final boolean allowUntaggedRemoval;
    // Hologram advanced config fields
    private static final boolean DEFAULT_REQUIRE_PERMISSION = false;
    private static final String DEFAULT_VIEW_PERMISSION = "ezauction.hologram.view";
    private static final boolean DEFAULT_PROXIMITY_LIMIT = false;
    private static final double DEFAULT_PROXIMITY_DISTANCE = 32.0D;
    private static final int DEFAULT_MAX_HOLOGRAMS = 100;
    private static final boolean DEFAULT_BATCH_UPDATE = true;
    private final int maxHolograms;
    private final boolean batchUpdate;
    private final boolean requirePermission;
    private final String viewPermission;
    private final boolean proximityLimit;
    private final double proximityDistance;

    public AuctionHologramConfiguration(boolean enabled, long updateIntervalTicks, double searchRadius,
            double heightOffset) {
        this(enabled, updateIntervalTicks, searchRadius, heightOffset, DEFAULT_MAX_HOLOGRAMS, DEFAULT_BATCH_UPDATE,
                DEFAULT_REQUIRE_PERMISSION, DEFAULT_VIEW_PERMISSION, DEFAULT_PROXIMITY_LIMIT, DEFAULT_PROXIMITY_DISTANCE,
                DEFAULT_REMOVAL_RADIUS, DEFAULT_MAX_REMOVAL_RADIUS, DEFAULT_ALLOW_UNTAGGED_REMOVAL);
    }

    public boolean enabled() { return enabled; }
    public long updateIntervalTicks() { return updateIntervalTicks; }
    public double searchRadius() { return searchRadius; }
    public double heightOffset() { return heightOffset; }
    public double removalRadius() { return removalRadius; }
    public double maxRemovalRadius() { return maxRemovalRadius; }
    public boolean allowUntaggedRemoval() { return allowUntaggedRemoval; }
    public int maxHolograms() { return maxHolograms; }
    public boolean batchUpdate() { return batchUpdate; }
    public boolean requirePermission() { return requirePermission; }
    public String viewPermission() { return viewPermission; }
    public boolean proximityLimit() { return proximityLimit; }
    public double proximityDistance() { return proximityDistance; }

    public AuctionHologramConfiguration(boolean enabled, long updateIntervalTicks, double searchRadius,
            double heightOffset, int maxHolograms, boolean batchUpdate,
            boolean requirePermission, String viewPermission,
            boolean proximityLimit, double proximityDistance,
            double removalRadius, double maxRemovalRadius, boolean allowUntaggedRemoval) {
        this.enabled = enabled;
        this.updateIntervalTicks = sanitizeInterval(updateIntervalTicks);
        this.searchRadius = sanitizeSearchRadius(searchRadius);
        this.heightOffset = sanitizeHeightOffset(heightOffset);
        this.maxHolograms = maxHolograms > 0 ? maxHolograms : DEFAULT_MAX_HOLOGRAMS;
        this.batchUpdate = batchUpdate;
        this.requirePermission = requirePermission;
        this.viewPermission = viewPermission != null ? viewPermission : DEFAULT_VIEW_PERMISSION;
        this.proximityLimit = proximityLimit;
        this.proximityDistance = proximityDistance > 0.0D ? proximityDistance : DEFAULT_PROXIMITY_DISTANCE;
        this.removalRadius = sanitizeRemovalRadius(removalRadius);
        this.maxRemovalRadius = sanitizeRemovalRadius(maxRemovalRadius);
        this.allowUntaggedRemoval = allowUntaggedRemoval;
    }

    public static AuctionHologramConfiguration defaults() {
        return new AuctionHologramConfiguration(DEFAULT_ENABLED, DEFAULT_UPDATE_INTERVAL_TICKS, DEFAULT_SEARCH_RADIUS,
                DEFAULT_HEIGHT_OFFSET, DEFAULT_MAX_HOLOGRAMS, DEFAULT_BATCH_UPDATE,
                DEFAULT_REQUIRE_PERMISSION, DEFAULT_VIEW_PERMISSION, DEFAULT_PROXIMITY_LIMIT, DEFAULT_PROXIMITY_DISTANCE,
                DEFAULT_REMOVAL_RADIUS, DEFAULT_MAX_REMOVAL_RADIUS, DEFAULT_ALLOW_UNTAGGED_REMOVAL);
    }

    public static AuctionHologramConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        boolean enabled = section.getBoolean("enabled", DEFAULT_ENABLED);
        long updateInterval = section.getLong("update-interval-ticks", DEFAULT_UPDATE_INTERVAL_TICKS);
        if (updateInterval <= 0L) {
            double seconds = section.getDouble("update-interval-seconds", -1.0D);
            if (Double.isFinite(seconds) && seconds > 0.0D) {
                updateInterval = Math.round(seconds * 20.0D);
            }
        }
        double searchRadius = section.getDouble("search-radius", DEFAULT_SEARCH_RADIUS);
        double heightOffset = section.getDouble("height-offset", DEFAULT_HEIGHT_OFFSET);
        int maxHolograms = section.getInt("max-holograms", DEFAULT_MAX_HOLOGRAMS);
        boolean batchUpdate = section.getBoolean("batch-update", DEFAULT_BATCH_UPDATE);
        boolean requirePermission = section.getBoolean("require-permission", DEFAULT_REQUIRE_PERMISSION);
        String viewPermission = section.getString("view-permission", DEFAULT_VIEW_PERMISSION);
        boolean proximityLimit = section.getBoolean("proximity-limit", DEFAULT_PROXIMITY_LIMIT);
        double proximityDistance = section.getDouble("proximity-distance", DEFAULT_PROXIMITY_DISTANCE);
        double removalRadius = section.getDouble("removal-radius", DEFAULT_REMOVAL_RADIUS);
        double maxRemovalRadius = section.getDouble("max-removal-radius", DEFAULT_MAX_REMOVAL_RADIUS);
        boolean allowUntaggedRemoval = section.getBoolean("allow-untagged-removal", DEFAULT_ALLOW_UNTAGGED_REMOVAL);
        return new AuctionHologramConfiguration(enabled, updateInterval, searchRadius, heightOffset, maxHolograms, batchUpdate, requirePermission, viewPermission, proximityLimit, proximityDistance, removalRadius, maxRemovalRadius, allowUntaggedRemoval);
    }

    @Override
    public String toString() {
        return "AuctionHologramConfiguration{" +
            "enabled=" + enabled +
            ", updateIntervalTicks=" + updateIntervalTicks +
            ", searchRadius=" + searchRadius +
            ", heightOffset=" + heightOffset +
            ", removalRadius=" + removalRadius +
            ", maxRemovalRadius=" + maxRemovalRadius +
            ", allowUntaggedRemoval=" + allowUntaggedRemoval +
            ", maxHolograms=" + maxHolograms +
            ", batchUpdate=" + batchUpdate +
            ", requirePermission=" + requirePermission +
            ", viewPermission='" + viewPermission + '\'' +
            ", proximityLimit=" + proximityLimit +
            ", proximityDistance=" + proximityDistance +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionHologramConfiguration)) return false;
        AuctionHologramConfiguration that = (AuctionHologramConfiguration) o;
        return enabled == that.enabled &&
                updateIntervalTicks == that.updateIntervalTicks &&
                Double.compare(that.searchRadius, searchRadius) == 0 &&
                Double.compare(that.heightOffset, heightOffset) == 0 &&
                Double.compare(that.removalRadius, removalRadius) == 0 &&
                Double.compare(that.maxRemovalRadius, maxRemovalRadius) == 0 &&
                allowUntaggedRemoval == that.allowUntaggedRemoval &&
                maxHolograms == that.maxHolograms &&
                batchUpdate == that.batchUpdate &&
                requirePermission == that.requirePermission &&
                Objects.equals(viewPermission, that.viewPermission) &&
                proximityLimit == that.proximityLimit &&
                Double.compare(that.proximityDistance, proximityDistance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, updateIntervalTicks, searchRadius, heightOffset, removalRadius, maxRemovalRadius, allowUntaggedRemoval, maxHolograms, batchUpdate, requirePermission, viewPermission, proximityLimit, proximityDistance);
    }

    private static long sanitizeInterval(long intervalTicks) {
        if (intervalTicks < 20L) {
            return DEFAULT_UPDATE_INTERVAL_TICKS;
        }
        return intervalTicks;
    }

    private static double sanitizeSearchRadius(double radius) {
        if (!(Double.isFinite(radius)) || radius <= 0.0D) {
            return DEFAULT_SEARCH_RADIUS;
        }
        return radius;
    }

    private static double sanitizeRemovalRadius(double radius) {
        if (!(Double.isFinite(radius)) || radius <= 0.0D) {
            return DEFAULT_REMOVAL_RADIUS;
        }
        return radius;
    }

    private static double sanitizeHeightOffset(double offset) {
        if (!Double.isFinite(offset)) {
            return DEFAULT_HEIGHT_OFFSET;
        }
        if (offset < 0.25D) {
            return 0.25D;
        }
        return offset;
    }
}
