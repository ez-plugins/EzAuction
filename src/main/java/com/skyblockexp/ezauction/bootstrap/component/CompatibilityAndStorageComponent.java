package com.skyblockexp.ezauction.bootstrap.component;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.config.AuctionConfiguration;
import com.skyblockexp.ezauction.compat.CompatibilityFacade;
import com.skyblockexp.ezauction.storage.AuctionStorageBundle;
import com.skyblockexp.ezauction.storage.AuctionStorageFactory;
import com.skyblockexp.ezframework.Registry;
import com.skyblockexp.ezframework.bootstrap.Component;

/**
 * Component-style handler for compatibility and storage setup.
 *
 * Converts the previous procedural `setup(...)` into an EzFramework `Component`.
 */
public class CompatibilityAndStorageComponent implements Component {
    public static class Result {
        public final CompatibilityFacade compatibilityFacade;
        public final com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepository;
        public final com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepository;

        public Result(CompatibilityFacade compatibilityFacade, com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepository, com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepository) {
            this.compatibilityFacade = compatibilityFacade;
            this.listingRepository = listingRepository;
            this.historyRepository = historyRepository;
        }
    }

    private EzAuctionPlugin plugin;
    private AuctionConfiguration configuration;
    private Result result;

    public CompatibilityAndStorageComponent(EzAuctionPlugin plugin, AuctionConfiguration configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
    }

    public CompatibilityAndStorageComponent(EzAuctionPlugin plugin) {
        this.plugin = plugin;
        this.configuration = null;
    }

    @Override
    public void start() throws Exception {
        // If configuration wasn't provided at construction, attempt to resolve it from the framework registry.
        try { if (this.configuration == null) try { this.configuration = Registry.forPlugin(plugin).get(AuctionConfiguration.class); } catch (Throwable ignored) {} } catch (Throwable ignored) {}
        CompatibilityFacade compatibilityFacade = CompatibilityFacade.create(plugin);
        AuctionStorageBundle storageBundle = AuctionStorageFactory.create(plugin, configuration);
        com.skyblockexp.ezauction.storage.AuctionListingRepository listingRepo = storageBundle.listingRepository();
        com.skyblockexp.ezauction.storage.AuctionHistoryRepository historyRepo = storageBundle.historyRepository();
        this.result = new Result(compatibilityFacade, listingRepo, historyRepo);
        // register key services into the framework registry for discovery
        try {
            Registry.forPlugin(plugin).register(CompatibilityFacade.class, compatibilityFacade);

            // Expose repository-typed views created by the storage bundle so callers
            // can rely on the EzFramework `Repository` contract directly.
            try { Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.storage.AuctionListingRepository.class, listingRepo); } catch (Throwable ignored) {}
            try { Registry.forPlugin(plugin).register(com.skyblockexp.ezauction.storage.AuctionHistoryRepository.class, historyRepo); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    @Override
    public void stop() throws Exception {
        // no-op for setup component; storages are managed/closed by their owning components
    }

    public Result getResult() {
        return this.result;
    }
}
