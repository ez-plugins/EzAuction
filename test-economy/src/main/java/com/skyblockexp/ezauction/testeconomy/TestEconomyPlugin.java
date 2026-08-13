package com.skyblockexp.ezauction.testeconomy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake economy plugin: registers a no-op Vault {@link Economy} provider on enable so dependent
 * plugins (e.g. EzAuction) can fully enable during the server-boot smoke workflow.
 */
public final class TestEconomyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Economy economy = new FakeEconomy();
        getServer().getServicesManager().register(
                Economy.class, economy, this, ServicePriority.Normal);
        getLogger().info("Registered fake Vault economy provider: " + economy.getName());
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(Economy.class, this);
    }
}
