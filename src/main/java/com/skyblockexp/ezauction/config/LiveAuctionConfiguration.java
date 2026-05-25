package com.skyblockexp.ezauction.config;

import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration describing the optional live auction queue feature.
 */
public final class LiveAuctionConfiguration {

    private final boolean enabled;
    private final boolean queueEnabled;
    private final boolean displayInChat;
    private final long announcementIntervalTicks;
    private final LiveAuctionCountdownConfiguration countdownConfiguration;

    public LiveAuctionConfiguration(boolean enabled, boolean queueEnabled, boolean displayInChat,
            long announcementIntervalTicks) {
        this(enabled, queueEnabled, displayInChat, announcementIntervalTicks,
                LiveAuctionCountdownConfiguration.defaults());
    }

    public LiveAuctionConfiguration(boolean enabled, boolean queueEnabled, boolean displayInChat,
            long announcementIntervalTicks, LiveAuctionCountdownConfiguration countdownConfiguration) {
        this.enabled = enabled;
        this.queueEnabled = queueEnabled;
        this.displayInChat = displayInChat;
        this.announcementIntervalTicks = Math.max(0L, announcementIntervalTicks);
        this.countdownConfiguration = countdownConfiguration != null
                ? countdownConfiguration
                : LiveAuctionCountdownConfiguration.defaults();
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean queueEnabled() {
        return queueEnabled;
    }

    public boolean displayInChat() {
        return displayInChat;
    }

    public long announcementIntervalTicks() {
        return announcementIntervalTicks;
    }

    public LiveAuctionCountdownConfiguration countdownConfiguration() {
        return countdownConfiguration;
    }

    public static LiveAuctionConfiguration defaults() {
        return new LiveAuctionConfiguration(false, true, true, 200L);
    }

    public static LiveAuctionConfiguration from(ConfigurationSection section) {
        if (section == null) {
            return defaults();
        }
        boolean enabled = section.getBoolean("enabled", false);
        boolean queueEnabled = section.getBoolean("queue-enabled", true);
        boolean displayInChat = section.getBoolean("display-in-chat", true);
        long interval = section.getLong("announcement-interval-ticks", 200L);
        LiveAuctionCountdownConfiguration countdownConfig =
                LiveAuctionCountdownConfiguration.from(section);
        return new LiveAuctionConfiguration(enabled, queueEnabled, displayInChat, interval, countdownConfig);
    }

    @Override
    public String toString() {
        return "LiveAuctionConfiguration{"
                + "enabled=" + enabled
                + ", queueEnabled=" + queueEnabled
                + ", displayInChat=" + displayInChat
                + ", announcementIntervalTicks=" + announcementIntervalTicks
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiveAuctionConfiguration that)) {
            return false;
        }
        return enabled == that.enabled
                && queueEnabled == that.queueEnabled
                && displayInChat == that.displayInChat
                && announcementIntervalTicks == that.announcementIntervalTicks;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, queueEnabled, displayInChat, announcementIntervalTicks);
    }
}
