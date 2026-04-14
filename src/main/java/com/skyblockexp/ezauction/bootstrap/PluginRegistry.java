package com.skyblockexp.ezauction.bootstrap;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionConfigurationLoader;
import com.skyblockexp.ezauction.integration.DiscordIntegration;
import com.skyblockexp.ezauction.integration.DiscordWebhookNotifier;

/**
 * Lightweight registry used by the plugin to expose configuration and integrations.
 */
public final class PluginRegistry {
    private final EzAuctionPlugin plugin;
    public DiscordIntegration discordIntegration;
    public DiscordWebhookNotifier discordWebhookNotifier;
    private AuctionConfiguration configuration;

    public PluginRegistry(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

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
        try {
            this.discordWebhookNotifier = new DiscordWebhookNotifier(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to initialize Discord webhook notifier: " + t.getMessage());
            this.discordWebhookNotifier = null;
        }
    }

    public void enableAll() {
        // Intentionally minimal: other components initialize themselves lazily.
    }

    public void disableAll() {
        // No-op for minimal registry.
    }

    public AuctionConfiguration getConfiguration() {
        return configuration;
    }

    public void reloadConfiguration() {
        this.configuration = AuctionConfigurationLoader.load(plugin);
    }

    /**
     * Reloads both the DiscordSRV integration and the webhook notifier from disk.
     * Does not reload the main auction configuration.
     */
    public void reloadDiscordIntegrations() {
        try {
            this.discordIntegration = new DiscordIntegration(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to reload Discord integration: " + t.getMessage());
            this.discordIntegration = null;
        }
        try {
            this.discordWebhookNotifier = new DiscordWebhookNotifier(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to reload Discord webhook notifier: " + t.getMessage());
            this.discordWebhookNotifier = null;
        }
    }
}
