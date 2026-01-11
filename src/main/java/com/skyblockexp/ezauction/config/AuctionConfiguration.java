package com.skyblockexp.ezauction.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parsed configuration for the auction plugin.
 */

public final class AuctionConfiguration {
    private final boolean debug;

    private final int baseListingLimit;
    private final AuctionStorageConfiguration storageConfiguration;
    private final AuctionListingRules listingRules;
    private final AuctionMenuConfiguration menuConfiguration;
    private final AuctionMenuInteractionConfiguration menuInteractionConfiguration;
    private final AuctionValueConfiguration valueConfiguration;
    private final AuctionMessageConfiguration messageConfiguration;
    private final AuctionBackendMessages backendMessages;
    private final AuctionCommandMessageConfiguration commandMessageConfiguration;
    private final AuctionHologramConfiguration hologramConfiguration;
    private final LiveAuctionConfiguration liveAuctionConfiguration;
    private final List<Duration> durationOptions;

        public AuctionConfiguration(int baseListingLimit, AuctionStorageConfiguration storageConfiguration,
            AuctionListingRules listingRules, AuctionMenuConfiguration menuConfiguration,
            AuctionMenuInteractionConfiguration menuInteractionConfiguration,
            AuctionValueConfiguration valueConfiguration, AuctionMessageConfiguration messageConfiguration,
            AuctionBackendMessages backendMessages,
            AuctionCommandMessageConfiguration commandMessageConfiguration,
            AuctionHologramConfiguration hologramConfiguration,
            LiveAuctionConfiguration liveAuctionConfiguration, List<Duration> durationOptions,
            boolean debug) {
        this.baseListingLimit = Math.max(0, baseListingLimit);
        this.storageConfiguration = storageConfiguration != null
                ? storageConfiguration
                : AuctionStorageConfiguration.yaml();
        this.listingRules = listingRules != null ? listingRules : AuctionListingRules.defaults();
        this.menuConfiguration = menuConfiguration != null ? menuConfiguration : AuctionMenuConfiguration.defaults();
        this.menuInteractionConfiguration = menuInteractionConfiguration != null
                ? menuInteractionConfiguration
                : AuctionMenuInteractionConfiguration.defaults();
        this.valueConfiguration = valueConfiguration != null ? valueConfiguration : AuctionValueConfiguration.defaults();
        this.messageConfiguration = messageConfiguration != null
                ? messageConfiguration
                : AuctionMessageConfiguration.defaults();
        this.backendMessages = backendMessages != null
                ? backendMessages
                : AuctionBackendMessages.defaults();
        this.commandMessageConfiguration = commandMessageConfiguration != null
                ? commandMessageConfiguration
                : AuctionCommandMessageConfiguration.defaults();
        this.hologramConfiguration = hologramConfiguration != null
                ? hologramConfiguration
                : AuctionHologramConfiguration.defaults();
        this.liveAuctionConfiguration = liveAuctionConfiguration != null
                ? liveAuctionConfiguration
                : LiveAuctionConfiguration.defaults();
        this.durationOptions = sanitizeDurationOptions(durationOptions);
        this.debug = debug;
    }

    public boolean debug() {
        return debug;
    }

    public int baseListingLimit() {
        return baseListingLimit;
    }

    public AuctionStorageConfiguration storageConfiguration() {
        return storageConfiguration;
    }

    public AuctionListingRules listingRules() {
        return listingRules;
    }

    public AuctionMenuConfiguration menuConfiguration() {
        return menuConfiguration;
    }

    public AuctionMenuInteractionConfiguration menuInteractionConfiguration() {
        return menuInteractionConfiguration;
    }

    public AuctionValueConfiguration valueConfiguration() {
        return valueConfiguration;
    }

    public AuctionMessageConfiguration messageConfiguration() {
        return messageConfiguration;
    }

    public AuctionBackendMessages backendMessages() {
        return backendMessages;
    }

    public AuctionCommandMessageConfiguration commandMessageConfiguration() {
        return commandMessageConfiguration;
    }

    public AuctionHologramConfiguration hologramConfiguration() {
        return hologramConfiguration;
    }

    public LiveAuctionConfiguration liveAuctionConfiguration() {
        return liveAuctionConfiguration;
    }

    public List<Duration> durationOptions() {
        return durationOptions;
    }

    public static AuctionConfiguration defaultConfiguration() {
        return new AuctionConfiguration(0, AuctionStorageConfiguration.yaml(), AuctionListingRules.defaults(),
                AuctionMenuConfiguration.defaults(), AuctionMenuInteractionConfiguration.defaults(),
                AuctionValueConfiguration.defaults(), AuctionMessageConfiguration.defaults(),
                AuctionBackendMessages.defaults(), AuctionCommandMessageConfiguration.defaults(),
                AuctionHologramConfiguration.defaults(), LiveAuctionConfiguration.defaults(), List.of(), false);
    }

    /**
     * Returns whether the history GUI is enabled in the configuration.
     * Looks for a 'history-gui.enabled' field in the menu configuration if available.
     */
    public boolean isHistoryGuiEnabled() {
        if (menuConfiguration != null) {
            return menuConfiguration.historyGuiEnabled();
        }
        return true;
    }

    @Override
    public String toString() {
        return "AuctionConfiguration{"
                + "baseListingLimit=" + baseListingLimit
                + ", storageConfiguration=" + storageConfiguration
                + ", listingRules=" + listingRules
                + ", menuConfiguration=" + menuConfiguration
                + ", menuInteractionConfiguration=" + menuInteractionConfiguration
                + ", valueConfiguration=" + valueConfiguration
                + ", messageConfiguration=" + messageConfiguration
                + ", backendMessages=" + backendMessages
                + ", commandMessageConfiguration=" + commandMessageConfiguration
                + ", hologramConfiguration=" + hologramConfiguration
                + ", liveAuctionConfiguration=" + liveAuctionConfiguration
                + ", durationOptions=" + durationOptions
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuctionConfiguration that)) {
            return false;
        }
        return baseListingLimit == that.baseListingLimit
                && Objects.equals(storageConfiguration, that.storageConfiguration)
                && Objects.equals(listingRules, that.listingRules)
                && Objects.equals(menuConfiguration, that.menuConfiguration)
                && Objects.equals(menuInteractionConfiguration, that.menuInteractionConfiguration)
                && Objects.equals(valueConfiguration, that.valueConfiguration)
                && Objects.equals(messageConfiguration, that.messageConfiguration)
                && Objects.equals(backendMessages, that.backendMessages)
                && Objects.equals(commandMessageConfiguration, that.commandMessageConfiguration)
                && Objects.equals(hologramConfiguration, that.hologramConfiguration)
                && Objects.equals(liveAuctionConfiguration, that.liveAuctionConfiguration)
                && Objects.equals(durationOptions, that.durationOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseListingLimit, storageConfiguration, listingRules, menuConfiguration,
                menuInteractionConfiguration, valueConfiguration, messageConfiguration, backendMessages,
                commandMessageConfiguration, hologramConfiguration, liveAuctionConfiguration, durationOptions);
    }

    private static List<Duration> sanitizeDurationOptions(List<Duration> options) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        List<Duration> sanitized = new ArrayList<>();
        for (Duration option : options) {
            if (option == null || option.isZero() || option.isNegative()) {
                continue;
            }
            sanitized.add(option);
        }
        if (sanitized.isEmpty()) {
            return List.of();
        }
        return List.copyOf(sanitized);
    }
}
