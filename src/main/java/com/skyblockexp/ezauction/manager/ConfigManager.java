package com.skyblockexp.ezauction.manager;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.bootstrap.component.ConfigurationLoaderComponent;

/**
 * Small manager responsible for reloading configuration at runtime.
 */
public final class ConfigManager {
    private final EzAuctionPlugin plugin;

    public ConfigManager(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public AuctionConfiguration reloadConfiguration() throws Exception {
        ConfigurationLoaderComponent loader = new ConfigurationLoaderComponent(plugin);
        loader.start();
        AuctionConfiguration cfg = loader.getResult();
        return cfg;
    }
}
