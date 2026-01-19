package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.compat.CompatibilityFacade;
import com.skyblockexp.ezauction.storage.AuctionStorage;
import com.skyblockexp.ezauction.storage.AuctionHistoryStorage;
import com.skyblockexp.ezauction.storage.AuctionStorageBundle;
import com.skyblockexp.ezauction.storage.AuctionStorageFactory;

/**
 * Handles compatibility and storage setup for the plugin.
 */
public class CompatibilityAndStorageComponent {
    public static class Result {
        public final CompatibilityFacade compatibilityFacade;
        public final AuctionStorage listingStorage;
        public final AuctionHistoryStorage historyStorage;
        public Result(CompatibilityFacade compatibilityFacade, AuctionStorage listingStorage, AuctionHistoryStorage historyStorage) {
            this.compatibilityFacade = compatibilityFacade;
            this.listingStorage = listingStorage;
            this.historyStorage = historyStorage;
        }
    }

    public Result setup(EzAuctionPlugin plugin, AuctionConfiguration configuration) {
        CompatibilityFacade compatibilityFacade = CompatibilityFacade.create(plugin);
        AuctionStorageBundle storageBundle = AuctionStorageFactory.create(plugin, configuration);
        return new Result(compatibilityFacade, storageBundle.listingStorage(), storageBundle.historyStorage());
    }
}
