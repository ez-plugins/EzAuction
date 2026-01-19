package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Handles Vault economy setup for the plugin.
 */
public class EconomySetupComponent {
    public Economy setup(EzAuctionPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found. EzAuction requires Vault and an economy provider to process currency.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            plugin.getLogger().severe("No Vault economy provider found. EzAuction cannot function without an economy bridge.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return null;
        }
        Economy economy = registration.getProvider();
        if (economy == null) {
            plugin.getLogger().severe("Unable to acquire a Vault economy provider. EzAuction cannot function without an economy bridge.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
        return economy;
    }
}
