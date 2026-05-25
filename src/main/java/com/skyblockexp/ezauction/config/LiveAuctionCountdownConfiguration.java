package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the live-auction countdown and bidding sub-sections of {@code auction.yml}.
 *
 * <p>Display type names are stored as strings (e.g. {@code "ACTION_BAR"}) so this class has no
 * compile-time dependency on EzCountdown; conversion to enum values happens in
 * {@link com.skyblockexp.ezauction.integration.EzCountdownIntegration}.</p>
 */
public final class LiveAuctionCountdownConfiguration {

    private final boolean ezcountdownEnabled;
    private final boolean ezcountdownAutoDetect;
    private final List<String> displayTypeNames;
    private final String formatMessage;
    private final String startMessage;
    private final String endMessage;
    private final long durationSeconds;
    private final long chatReminderIntervalSeconds;
    private final List<Long> chatReminderAtSeconds;
    private final double minimumBidIncrementPercent;
    private final double minimumBidIncrementFixed;
    private final boolean chatFilterFeatureEnabled;
    private final long minTimeBeforeFinishSeconds;

    public LiveAuctionCountdownConfiguration(
            boolean ezcountdownEnabled,
            boolean ezcountdownAutoDetect,
            List<String> displayTypeNames,
            String formatMessage,
            String startMessage,
            String endMessage,
            long durationSeconds,
            long chatReminderIntervalSeconds,
            List<Long> chatReminderAtSeconds,
            double minimumBidIncrementPercent,
            double minimumBidIncrementFixed,
            boolean chatFilterFeatureEnabled,
            long minTimeBeforeFinishSeconds) {
        this.ezcountdownEnabled = ezcountdownEnabled;
        this.ezcountdownAutoDetect = ezcountdownAutoDetect;
        this.displayTypeNames = displayTypeNames != null && !displayTypeNames.isEmpty()
                ? List.copyOf(displayTypeNames)
                : List.of("ACTION_BAR");
        this.formatMessage = formatMessage != null && !formatMessage.isBlank() ? formatMessage : "{formatted}";
        this.startMessage = startMessage != null ? startMessage : "";
        this.endMessage = endMessage != null ? endMessage : "Auction ended!";
        this.durationSeconds = Math.max(5L, durationSeconds);
        this.chatReminderIntervalSeconds = Math.max(0L, chatReminderIntervalSeconds);
        this.chatReminderAtSeconds = chatReminderAtSeconds != null
                ? Collections.unmodifiableList(new ArrayList<>(chatReminderAtSeconds))
                : List.of(30L, 10L, 5L);
        this.minimumBidIncrementPercent = Math.max(0.0, minimumBidIncrementPercent);
        this.minimumBidIncrementFixed = Math.max(0.0, minimumBidIncrementFixed);
        this.chatFilterFeatureEnabled = chatFilterFeatureEnabled;
        this.minTimeBeforeFinishSeconds = Math.max(0L, minTimeBeforeFinishSeconds);
    }

    // ---- getters ----

    public boolean ezcountdownEnabled() { return ezcountdownEnabled; }

    /**
     * When {@code true} (default), EzCountdown is used automatically if the plugin is present,
     * regardless of the {@code ezcountdown-enabled} setting.
     */
    public boolean ezcountdownAutoDetect() { return ezcountdownAutoDetect; }

    /** Raw display type names; passed directly to EzCountdownIntegration for conversion. */
    public List<String> displayTypeNames() { return displayTypeNames; }

    public String formatMessage() { return formatMessage; }

    public String startMessage() { return startMessage; }

    public String endMessage() { return endMessage; }

    public long durationSeconds() { return durationSeconds; }

    public long chatReminderIntervalSeconds() { return chatReminderIntervalSeconds; }

    public List<Long> chatReminderAtSeconds() { return chatReminderAtSeconds; }

    public double minimumBidIncrementPercent() { return minimumBidIncrementPercent; }

    public double minimumBidIncrementFixed() { return minimumBidIncrementFixed; }

    public boolean chatFilterFeatureEnabled() { return chatFilterFeatureEnabled; }

    public long minTimeBeforeFinishSeconds() { return minTimeBeforeFinishSeconds; }

    // ---- factories ----

    public static LiveAuctionCountdownConfiguration defaults() {
        return new LiveAuctionCountdownConfiguration(
                false,
                true,
                List.of("ACTION_BAR"),
                "{formatted}",
                "",
                "Auction ended!",
                60L, 0L,
                List.of(30L, 10L, 5L),
                5.0, 10.0,
                false,
                5L);
    }

    public static LiveAuctionCountdownConfiguration from(ConfigurationSection section) {
        if (section == null) return defaults();

        ConfigurationSection countdownSection = section.getConfigurationSection("countdown");
        boolean ezEnabled = false;
        boolean ezAutoDetect = true;
        List<String> displayTypes = List.of("ACTION_BAR");
        String format = "{formatted}";
        String start = "";
        String end = "Auction ended!";
        long duration = 60L;
        long reminderInterval = 0L;
        List<Long> reminderAt = List.of(30L, 10L, 5L);

        if (countdownSection != null) {
            ezEnabled = countdownSection.getBoolean("ezcountdown-enabled", false);
            ezAutoDetect = countdownSection.getBoolean("ezcountdown-auto-detect", true);
            List<String> rawTypes = countdownSection.getStringList("display-types");
            if (!rawTypes.isEmpty()) displayTypes = rawTypes;
            format = countdownSection.getString("format-message", "{formatted}");
            start = countdownSection.getString("start-message", "");
            end = countdownSection.getString("end-message", "Auction ended!");
            duration = countdownSection.getLong("duration-seconds", 60L);
            reminderInterval = countdownSection.getLong("chat-reminder-interval-seconds", 0L);
            reminderAt = parseLongList(countdownSection.getList("chat-reminder-at-seconds",
                    List.of(30, 10, 5)));
        }

        ConfigurationSection biddingSection = section.getConfigurationSection("bidding");
        double incrementPct = 5.0;
        double incrementFixed = 10.0;
        if (biddingSection != null) {
            incrementPct = biddingSection.getDouble("minimum-bid-increment-percent", 5.0);
            incrementFixed = biddingSection.getDouble("minimum-bid-increment-fixed", 10.0);
        }

        boolean chatFilter = section.getBoolean("chat-filter-feature-enabled", false);

        return new LiveAuctionCountdownConfiguration(
                ezEnabled, ezAutoDetect, displayTypes, format, start, end,
                duration, reminderInterval, reminderAt,
                incrementPct, incrementFixed, chatFilter,
                section.getLong("min-time-before-finish-seconds", 5L));
    }

    private static List<Long> parseLongList(List<?> list) {
        if (list == null) return List.of();
        List<Long> result = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) result.add(n.longValue());
        }
        return Collections.unmodifiableList(result);
    }
}
