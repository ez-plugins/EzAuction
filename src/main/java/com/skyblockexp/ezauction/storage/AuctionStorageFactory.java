package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.config.AuctionStorageConfiguration;
import com.skyblockexp.ezauction.config.AuctionStorageConfiguration.Mysql;
import com.skyblockexp.ezauction.storage.mysql.MysqlAuctionListingStorage;
import com.skyblockexp.ezauction.storage.mysql.MysqlAuctionHistoryStorage;
import com.skyblockexp.ezauction.storage.yaml.YamlAuctionHistoryStorage;
import com.skyblockexp.ezauction.storage.yaml.YamlAuctionStorage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Factory responsible for creating initialized storage implementations.
 */
public final class AuctionStorageFactory {

    private AuctionStorageFactory() {
    }

    public static AuctionStorageBundle create(JavaPlugin plugin, AuctionConfiguration configuration) {
        if (configuration == null) {
            configuration = AuctionConfiguration.defaultConfiguration();
        }
        AuctionStorageConfiguration storageConfiguration = configuration.storageConfiguration();

        if (storageConfiguration.type() == AuctionStorageConfiguration.StorageType.MYSQL) {
            Mysql mysql = storageConfiguration.mysql();
            if (mysql != null) {
                MysqlAuctionListingStorage listingStorage = new MysqlAuctionListingStorage(plugin, mysql);
                MysqlAuctionHistoryStorage historyStorage = new MysqlAuctionHistoryStorage(
                        plugin != null ? plugin.getLogger() : null, mysql);
                boolean listingOk = listingStorage.initialize();
                boolean historyOk = historyStorage.initialize();
                if (listingOk && historyOk) {
                    return new AuctionStorageBundle(listingStorage, historyStorage);
                }
                plugin.getLogger().warning(
                        "Failed to initialize MySQL storage for " + EzAuctionPlugin.DISPLAY_NAME
                                + ". Falling back to YAML data files.");
                try { listingStorage.close(); } catch (Exception ignored) {}
                try { historyStorage.close(); } catch (Exception ignored) {}
            }
        }

        YamlAuctionStorage listingStorage = new YamlAuctionStorage(plugin);
        listingStorage.initialize();
        YamlAuctionHistoryStorage historyStorage = new YamlAuctionHistoryStorage(plugin);
        historyStorage.initialize();
        return new AuctionStorageBundle(listingStorage, historyStorage);
    }
}
