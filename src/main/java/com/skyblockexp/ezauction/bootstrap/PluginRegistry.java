package com.skyblockexp.ezauction.bootstrap;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionConfigurationLoader;
import com.skyblockexp.ezauction.integration.DiscordIntegration;

/**
 * Lightweight registry used by the plugin to expose configuration and integrations.
 *
 * This implementation provides the minimal surface required by the codebase
 * (configuration access, Discord integration, and basic lifecycle methods).
 */
public final class PluginRegistry {
    private final EzAuctionPlugin plugin;
    public com.skyblockexp.ezauction.integration.DiscordIntegration discordIntegration;
    private AuctionConfiguration configuration;

    public PluginRegistry(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration and lightweight integrations.
     */
    public void load() {
        try {
            this.configuration = AuctionConfigurationLoader.load(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load auction configuration: " + t.getMessage());
            this.configuration = AuctionConfiguration.defaultConfiguration();
        }
        try {
            this.discordIntegration = new DiscordIntegration(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to initialize Discord integration: " + t.getMessage());
            this.discordIntegration = null;
        }
    }

    /**
     * Enable or initialize runtime components. Kept minimal for tests/compilation.
     */
    public void enableAll() {
        // Intentionally minimal: other components initialize themselves lazily.
    }

    /**
     * Disable runtime components and cleanup.
     */
    public void disableAll() {
        // No-op for minimal registry.
    }

    public AuctionConfiguration getConfiguration() {
        return configuration;
    }

    public void reloadConfiguration() {
        this.configuration = AuctionConfigurationLoader.load(plugin);
    }
}
