package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;

/**
 * Handles loading of the main AuctionConfiguration for the plugin.
 */
public class ConfigurationLoaderComponent {
    public AuctionConfiguration load(EzAuctionPlugin plugin) {
        // Replace with actual config loader if needed
        return com.skyblockexp.ezauction.config.AuctionConfigurationLoader.load(plugin);
    }
}
