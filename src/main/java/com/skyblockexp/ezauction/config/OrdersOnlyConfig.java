package com.skyblockexp.ezauction.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class OrdersOnlyConfig {
    private static boolean ordersOnlyMode = false;

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "orders-only.yml");
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ordersOnlyMode = config.getBoolean("orders-only-mode", false);
        } else {
            ordersOnlyMode = false;
        }
    }

    public static boolean isOrdersOnlyMode() {
        return ordersOnlyMode;
    }
}
