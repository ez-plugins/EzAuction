package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezframework.Registry;
import com.skyblockexp.ezframework.bootstrap.Component;

/**
 * Component-style loader for the main AuctionConfiguration.
 */
public class ConfigurationLoaderComponent implements Component {
    private final EzAuctionPlugin plugin;
    private AuctionConfiguration result;

    public ConfigurationLoaderComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() throws Exception {
        this.result = com.skyblockexp.ezauction.config.AuctionConfigurationLoader.load(this.plugin);
        if (this.result != null) {
            try { Registry.forPlugin(plugin).register(AuctionConfiguration.class, this.result); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void stop() throws Exception {
        // no-op
    }

    public AuctionConfiguration getResult() {
        return this.result;
    }
}
