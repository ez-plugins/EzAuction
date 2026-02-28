package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import net.milkbowl.vault.economy.Economy;
import com.skyblockexp.ezframework.Registry;
import org.bukkit.plugin.RegisteredServiceProvider;
import com.skyblockexp.ezframework.bootstrap.Component;

/**
 * Component-style handler for Vault economy setup.
 */
public class EconomySetupComponent implements Component {
    private final EzAuctionPlugin plugin;
    private Economy result;

    public EconomySetupComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() throws Exception {
        if (this.plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            this.plugin.getLogger().severe("Vault plugin not found. EzAuction requires Vault and an economy provider to process currency.");
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            this.result = null;
            return;
        }
        RegisteredServiceProvider<Economy> registration = this.plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            this.plugin.getLogger().severe("No Vault economy provider found. EzAuction cannot function without an economy bridge.");
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            this.result = null;
            return;
        }
        Economy economy = registration.getProvider();
        if (economy == null) {
            this.plugin.getLogger().severe("Unable to acquire a Vault economy provider. EzAuction cannot function without an economy bridge.");
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
        }
        this.result = economy;
        if (this.result != null) {
            try { Registry.forPlugin(plugin).register(Economy.class, this.result); } catch (Throwable ignored) {}
        }
    }

    @Override
    public void stop() throws Exception {
        // no-op
    }

    public Economy getResult() {
        return this.result;
    }
}
