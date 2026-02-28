package com.skyblockexp.ezauction;

import com.skyblockexp.ezauction.config.OrdersOnlyConfig;
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
import com.skyblockexp.ezframework.EzPlugin;
import com.skyblockexp.ezframework.bootstrap.Component;
import java.util.List;
import com.skyblockexp.ezauction.bootstrap.EzAuctionFrameworkComponent;
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
public final class EzAuctionPlugin extends EzPlugin {

    public static final String DISPLAY_NAME = "EzAuction";
    
    private static volatile EzAuctionPlugin instance;

    /**
     * Called when the plugin is enabled by the server.
     * 
     * <p>
     * Loads configuration, sets up economy, initializes managers, GUIs, commands, metrics, and integrations.
     * Registers all event listeners and commands.
     * </p>
     */
    @Override
    protected List<Component> components() {
        // Ensure defaults and orders-only config are available early
        instance = this;
        logStartupHeader();
        ensureDefaultConfig();
        OrdersOnlyConfig.load(this);
        // Provide explicit, ordered components so each can be constructed with only
        // the plugin and resolve dependencies from the framework registry at start.
        return List.of(
            new com.skyblockexp.ezauction.bootstrap.component.ConfigurationLoaderComponent(this),
            new com.skyblockexp.ezauction.bootstrap.component.EconomySetupComponent(this),
            new com.skyblockexp.ezauction.bootstrap.component.CompatibilityAndStorageComponent(this),
            new com.skyblockexp.ezauction.bootstrap.component.EzFrameworkIntegrationComponent(this),
            new com.skyblockexp.ezauction.bootstrap.component.ServiceSetupComponent(this),
            new com.skyblockexp.ezauction.bootstrap.component.GuiAndCommandSetupComponent(this)
        );
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

    /**
     * Gets the plugin's registry containing all managers and services.
     * @return
     */
    // PluginRegistry has been replaced by EzFramework Registry; consumers should use Registry.forPlugin(this).

    /**
     * Gets the singleton instance of the EzAuctionPlugin.
     * @return
     */
    public static EzAuctionPlugin getInstance() {
        return instance;
    }

    /**
     * Lightweight detection hook to log if EzFramework is available at runtime.
     * This uses reflection so EzAuction can compile even when EzFramework
     * artifacts are not yet installed locally.
     */
    // EzFramework bootstrap handles lifecycle; no direct init required here.
}
