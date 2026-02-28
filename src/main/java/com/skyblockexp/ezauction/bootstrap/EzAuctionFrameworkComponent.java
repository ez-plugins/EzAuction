package com.skyblockexp.ezauction.bootstrap;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import java.util.logging.Level;
import com.skyblockexp.ezframework.bootstrap.Component;
import com.skyblockexp.ezauction.bootstrap.component.ConfigurationLoaderComponent;
import com.skyblockexp.ezauction.bootstrap.component.EconomySetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.CompatibilityAndStorageComponent;
import com.skyblockexp.ezauction.bootstrap.component.EzFrameworkIntegrationComponent;
import com.skyblockexp.ezauction.bootstrap.component.ServiceSetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.GuiAndCommandSetupComponent;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.transaction.AuctionTransactionHistory;
import com.skyblockexp.ezauction.live.LiveAuctionService;
import org.bukkit.command.PluginCommand;
import com.skyblockexp.ezauction.gui.AuctionReturnListener;
import com.skyblockexp.ezauction.gui.AuctionSellMenuListener;
import com.skyblockexp.ezauction.live.LiveAuctionEnqueueListener;
import com.skyblockexp.ezauction.gui.AuctionActivityMenuListener;
import com.skyblockexp.ezauction.hologram.AuctionHologramListener;
import com.skyblockexp.ezauction.update.SpigotUpdateChecker;
import com.skyblockexp.ezauction.command.LiveAuctionCommand;
import com.skyblockexp.ezauction.command.OrdersCommand;
import com.skyblockexp.ezauction.command.OrderCommand;
import com.skyblockexp.ezauction.config.OrdersOnlyConfig;
import com.skyblockexp.ezauction.compat.CompatibilityFacade;
import org.bstats.bukkit.Metrics;
import com.skyblockexp.ezauction.util.ItemValueProvider;

/**
 * EzFramework Component that boots EzAuction startup components during framework startup.
 */
public class EzAuctionFrameworkComponent implements Component {

    private final EzAuctionPlugin plugin;

    public EzAuctionFrameworkComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() throws Exception {
        // Sequentially start components and register events/commands (replaces PluginRegistry)
        ConfigurationLoaderComponent configLoader = new ConfigurationLoaderComponent(plugin);
        configLoader.start();
        AuctionConfiguration configuration = configLoader.getResult();

        EconomySetupComponent economySetup = new EconomySetupComponent(plugin);
        economySetup.start();

        CompatibilityAndStorageComponent compatAndStorage = new CompatibilityAndStorageComponent(plugin, configuration);
        compatAndStorage.start();
        CompatibilityAndStorageComponent.Result compatResult = compatAndStorage.getResult();

        EzFrameworkIntegrationComponent ezFrameworkIntegration = new EzFrameworkIntegrationComponent(plugin);
        try { ezFrameworkIntegration.start(); } catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Failed to initialize EzFramework integration component", ex); }

        ServiceSetupComponent serviceComponent = new ServiceSetupComponent(plugin, configuration, economySetup.getResult(), compatResult.listingRepository, compatResult.historyRepository, compatResult.compatibilityFacade);
        serviceComponent.start();
        ServiceSetupComponent.ServiceSetupResult serviceResult = serviceComponent.getResult();

        GuiAndCommandSetupComponent guiComponent = new GuiAndCommandSetupComponent(plugin, configuration, serviceResult.auctionManager, serviceResult.transactionService, serviceResult.transactionHistory, serviceResult.liveAuctionService, compatResult.compatibilityFacade, compatResult.compatibilityFacade.itemTagStorage());
        guiComponent.start();
        GuiAndCommandSetupComponent.GuiSetupResult guiResult = guiComponent.getResult();

        // Register listeners and commands (mirrors previous PluginRegistry.enableAll behavior)
        plugin.getServer().getPluginManager().registerEvents(guiResult.auctionOrderMenu, plugin);

        PluginCommand ordersCmd = plugin.getCommand("orders");
        if (ordersCmd != null) {
            OrdersCommand ordersCommand = new OrdersCommand(guiResult.auctionOrderMenu);
            ordersCmd.setExecutor(ordersCommand);
            ordersCmd.setTabCompleter(ordersCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'orders' is not defined in plugin.yml; orders-only mode will not work.");
        }
        PluginCommand orderCmd = plugin.getCommand("order");
        if (orderCmd != null) {
            OrderCommand orderCommand = new OrderCommand(guiResult.auctionOrderMenu);
            orderCmd.setExecutor(orderCommand);
            orderCmd.setTabCompleter(orderCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'order' is not defined in plugin.yml; orders-only mode will not work.");
        }

        if (OrdersOnlyConfig.isOrdersOnlyMode()) return;

        plugin.getServer().getPluginManager().registerEvents(guiResult.auctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(guiResult.liveAuctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionActivityMenuListener(guiResult.auctionActivityMenu, guiResult.auctionMenu, compatResult.compatibilityFacade.itemTagStorage()), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionReturnListener(new com.skyblockexp.ezauction.claim.AuctionClaimService(serviceResult.pendingReturns, configuration.backendMessages())), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionSellMenuListener(guiResult.auctionSellMenu), plugin);

        if (configuration != null && configuration.isHistoryGuiEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(new com.skyblockexp.ezauction.gui.AuctionHistoryListener(), plugin);
        }

        if (serviceResult.liveAuctionService != null && serviceResult.liveAuctionService.isEnabled()) {
            plugin.getServer().getPluginManager().registerEvents(new LiveAuctionEnqueueListener(serviceResult.liveAuctionService), plugin);
            LiveAuctionCommand liveCmd = new LiveAuctionCommand(serviceResult.auctionManager, guiResult.liveAuctionMenu, guiResult.auctionSellMenu, configuration.commandMessageConfiguration());
            plugin.getCommand("liveauction").setExecutor(liveCmd);
            plugin.getCommand("liveauction").setTabCompleter(liveCmd);
        }

        PluginCommand command = plugin.getCommand("auction");
        if (command != null) {
            command.setExecutor(guiResult.auctionCommand);
            command.setTabCompleter(guiResult.auctionCommand);
        } else {
            plugin.getLogger().severe("Plugin command 'auction' is not defined in plugin.yml; the auction house will be unusable.");
        }

        if (guiResult.hologramManager != null && guiResult.hologramCommand != null) {
            PluginCommand hologramPluginCommand = plugin.getCommand("auctionhologram");
            if (hologramPluginCommand != null) {
                hologramPluginCommand.setExecutor(guiResult.hologramCommand);
                hologramPluginCommand.setTabCompleter(guiResult.hologramCommand);
            } else {
                plugin.getLogger().warning("Plugin command 'auctionhologram' is not defined; hologram placement will be unavailable.");
            }
            plugin.getServer().getPluginManager().registerEvents(new AuctionHologramListener(guiResult.hologramManager), plugin);
        }

        new SpigotUpdateChecker(plugin, 129779).checkForUpdates();
    }

    @Override
    public void stop() throws Exception {
        // Components are responsible for their own cleanup where applicable.
    }
}
