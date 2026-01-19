package com.skyblockexp.ezauction;

import com.skyblockexp.ezauction.bootstrap.PluginRegistry;
import com.skyblockexp.ezauction.bootstrap.PluginConfigs;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

/**
 * Main plugin class for EzAuction.
 * 
 * <p>
 * Handles initialization, configuration, service registration, and shutdown for the EzAuction house plugin.
 * Integrates with Vault, PlaceholderAPI, bStats, and optional hologram/shop price overlays.
 * </p>
 * <p>
 * All gameplay features and logic are delegated to dedicated managers, GUIs, and services.
 * </p>
 *
 * @author Shadow48402
 * @since 1.1.0
 */
public final class EzAuctionPlugin extends JavaPlugin {

    public static final String DISPLAY_NAME = "EzAuction";
    private PluginRegistry registry;

    /**
     * Called when the plugin is enabled by the server.
     * 
     * <p>
     * Loads configuration, sets up economy, initializes managers, GUIs, commands, metrics, and integrations.
     * Registers all event listeners and commands.
     * </p>
     */
    @Override
    public void onEnable() {
        logStartupHeader();
        ensureDefaultConfig();
        // PluginRegistry will handle all initialization and registration
        registry = new PluginRegistry(this);
        registry.enableAll();
        getLogger().info(DISPLAY_NAME + " plugin enabled.");
    }

    /**
     * Called when the plugin is disabled by the server.
     * 
     * <p>
     * Unregisters all listeners, disables managers and services, cleans up resources, and closes storage.
     * </p>
     */
    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (registry != null) {
            registry.disableAll();
            registry = null;
        }
        getLogger().info(DISPLAY_NAME + " plugin disabled.");
    }

    /**
     * Ensures all default configuration files are present in the plugin data folder.
     * Creates missing files from plugin resources.
     */
    private void ensureDefaultConfig() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            getLogger().log(Level.WARNING, "Failed to create plugin data folder at {0}.", dataFolder);
        }
        for (String resourcePath : PluginConfigs.CONFIG_FILES) {
            ensureConfigFile(dataFolder, resourcePath);
        }
    }

    /**
     * Ensures a specific configuration file exists in the data folder, copying from resources if missing.
     *
     * @param dataFolder   The plugin's data folder
     * @param resourcePath The resource path of the config file
     */
    private void ensureConfigFile(File dataFolder, String resourcePath) {
        if (dataFolder == null || resourcePath == null || resourcePath.isEmpty()) {
            return;
        }
        File target = new File(dataFolder, resourcePath);
        if (!target.exists()) {
            saveResource(resourcePath, false);
        }
    }

    /**
     * Logs the plugin startup header to the console.
     */
    private void logStartupHeader() {
        String version = getDescription().getVersion();
        getLogger().info("========================================");
        getLogger().info(DISPLAY_NAME + " v" + version + " starting up.");
    }

}
