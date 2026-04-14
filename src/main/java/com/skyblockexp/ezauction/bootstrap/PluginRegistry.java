package com.skyblockexp.ezauction.bootstrap;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.bootstrap.component.CompatibilityAndStorageComponent;
import com.skyblockexp.ezauction.bootstrap.component.EconomySetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.GuiAndCommandSetupComponent;
import com.skyblockexp.ezauction.bootstrap.component.ServiceSetupComponent;
import com.skyblockexp.ezauction.command.DiscordCommand;
import com.skyblockexp.ezauction.command.LiveAuctionCommand;
import com.skyblockexp.ezauction.command.OrderCommand;
import com.skyblockexp.ezauction.command.OrdersCommand;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionConfigurationLoader;
import com.skyblockexp.ezauction.gui.AuctionActivityMenuListener;
import com.skyblockexp.ezauction.gui.AuctionHistoryGUI;
import com.skyblockexp.ezauction.gui.AuctionHistoryListener;
import com.skyblockexp.ezauction.gui.AuctionReturnListener;
import com.skyblockexp.ezauction.gui.AuctionSellMenuListener;
import com.skyblockexp.ezauction.hologram.AuctionHologramListener;
import com.skyblockexp.ezauction.integration.DiscordIntegration;
import com.skyblockexp.ezauction.integration.DiscordWebhookNotifier;
import com.skyblockexp.ezauction.live.LiveAuctionEnqueueListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Lightweight registry used by the plugin to expose configuration and integrations.
 */
public final class PluginRegistry {
    private final EzAuctionPlugin plugin;
    public DiscordIntegration discordIntegration;
    public DiscordWebhookNotifier discordWebhookNotifier;
    private AuctionConfiguration configuration;

    private GuiAndCommandSetupComponent.GuiSetupResult guiResult;
    private ServiceSetupComponent.ServiceSetupResult serviceResult;
    private CompatibilityAndStorageComponent.Result storageResult;

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
        plugin.getLogger().info("[Bootstrap] enableAll() starting.");
        try {
            plugin.getLogger().info("[Bootstrap] Step 1/5 - economy (Vault)...");
            Economy economy = new EconomySetupComponent().setup(plugin);
            if (economy == null) {
                plugin.getLogger().severe("[Bootstrap] Economy setup failed - commands will NOT be registered.");
                return;
            }
            plugin.getLogger().info("[Bootstrap] Step 1/5 OK - economy: " + economy.getName());

            plugin.getLogger().info("[Bootstrap] Step 2/5 - storage and compat layer...");
            storageResult = new CompatibilityAndStorageComponent().setup(plugin, configuration);
            plugin.getLogger().info("[Bootstrap] Step 2/5 OK.");

            plugin.getLogger().info("[Bootstrap] Step 3/5 - services...");
            serviceResult = ServiceSetupComponent.setupAll(
                    plugin, configuration, economy,
                    storageResult.listingStorage, storageResult.historyStorage,
                    storageResult.compatibilityFacade);
            this.discordIntegration = serviceResult.discordIntegration;
            plugin.getLogger().info("[Bootstrap] Step 3/5 OK.");

            plugin.getLogger().info("[Bootstrap] Step 4/5 - GUIs and command objects...");
            guiResult = GuiAndCommandSetupComponent.setupAll(
                    plugin, configuration,
                    serviceResult.auctionManager, serviceResult.transactionService,
                    serviceResult.transactionHistory, serviceResult.liveAuctionService,
                    storageResult.compatibilityFacade,
                    storageResult.compatibilityFacade.itemTagStorage());
            plugin.getLogger().info("[Bootstrap] Step 4/5 OK.");

            plugin.getLogger().info("[Bootstrap] Step 5/5 - registering commands and listeners...");
            registerCommands();
            registerListeners();
            plugin.getLogger().info("[Bootstrap] Step 5/5 OK - all commands registered.");

        } catch (Throwable t) {
            plugin.getLogger().severe("[Bootstrap] FATAL during enableAll() - commands will NOT work!");
            java.util.logging.Logger.getLogger(plugin.getName())
                    .log(java.util.logging.Level.SEVERE, "[Bootstrap] Stack trace:", t);
        }
    }

    private void registerCommands() {
        registerCommand("auction", guiResult.auctionCommand, guiResult.auctionCommand);

        LiveAuctionCommand liveCmd = new LiveAuctionCommand(
                serviceResult.auctionManager, guiResult.liveAuctionMenu,
                guiResult.auctionSellMenu, configuration.commandMessageConfiguration());
        registerCommand("liveauction", liveCmd, liveCmd);

        registerCommand("orders", new OrdersCommand(guiResult.auctionOrderMenu), null);
        registerCommand("order", new OrderCommand(guiResult.auctionOrderMenu), null);
        registerCommand("auctiondiscord", new DiscordCommand(serviceResult.discordIntegration), null);

        if (guiResult.hologramCommand != null) {
            registerCommand("auctionhologram", guiResult.hologramCommand, null);
        }

        AuctionHistoryListener historyListener = new AuctionHistoryListener();
        registerCommand("auctionhistory", (sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command is for players only.");
                return true;
            }
            if (!player.hasPermission("ezauction.auction.history")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length > 0 && !player.hasPermission("ezauction.auction.history.others")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to view other players' history.");
                return true;
            }
            Player target = player;
            if (args.length > 0) {
                target = plugin.getServer().getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                    return true;
                }
            }
            AuctionHistoryGUI gui = new AuctionHistoryGUI(
                    player, target, player.hasPermission("ezauction.auction.history.others"),
                    serviceResult.transactionHistory);
            historyListener.registerGUI(player, gui);
            gui.open(AuctionHistoryGUI.Tab.SALES);
            return true;
        }, null);
        plugin.getServer().getPluginManager().registerEvents(historyListener, plugin);
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(guiResult.auctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(guiResult.liveAuctionMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(guiResult.auctionOrderMenu, plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new AuctionSellMenuListener(guiResult.auctionSellMenu), plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new AuctionActivityMenuListener(
                        guiResult.auctionActivityMenu, guiResult.auctionMenu,
                        storageResult.compatibilityFacade.itemTagStorage()), plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new AuctionReturnListener(serviceResult.claimService), plugin);
        plugin.getServer().getPluginManager().registerEvents(
                new LiveAuctionEnqueueListener(serviceResult.liveAuctionService), plugin);
        if (guiResult.hologramManager != null) {
            plugin.getServer().getPluginManager().registerEvents(
                    new AuctionHologramListener(guiResult.hologramManager), plugin);
        }
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().warning("[Bootstrap] /" + name + " not found in plugin.yml - skipping.");
            return;
        }
        cmd.setExecutor(executor);
        if (completer != null) {
            cmd.setTabCompleter(completer);
        }
        plugin.getLogger().info("[Bootstrap]   registered /" + name);
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
