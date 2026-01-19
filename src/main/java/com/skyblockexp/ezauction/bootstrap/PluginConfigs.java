package com.skyblockexp.ezauction.bootstrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Holds the list of all configuration files that should be ensured on plugin startup.
 * This class is used to centralize config resource management for EzAuction.
 */
public final class PluginConfigs {
    /**
     * List of all config resource paths to ensure exist in the plugin data folder.
     */
    public static final List<String> CONFIG_FILES = Collections.unmodifiableList(Arrays.asList(
        "auction.yml",
        "auction-storage.yml",
        "messages.yml",
        "orders-only.yml",
        "messages/messages_en.yml",
        "messages/messages_es.yml",
        "messages/messages_nl.yml",
        "messages/messages_zh.yml",
        "messages/menu-layout_en.yml",
        "messages/menu-layout_es.yml",
        "messages/menu-layout_nl.yml",
        "messages/menu-layout_zh.yml",
        "messages/menu-interactions_en.yml",
        "messages/menu-interactions_es.yml",
        "messages/menu-interactions_nl.yml",
        "messages/menu-interactions_zh.yml",
        "messages/gui-messages_en.yml",
        "messages/gui-messages_es.yml",
        "messages/gui-messages_nl.yml",
        "messages/gui-messages_zh.yml",
        "auction-values.yml"
    ));

    private PluginConfigs() {
        // Utility class
    }
}
