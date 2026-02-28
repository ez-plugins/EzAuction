package com.skyblockexp.ezauction.storage.mysql;

import com.skyblockexp.ezauction.storage.AuctionListingRepository;

/**
 * Concrete repository backed by the MySQL listing storage.
 */
public final class MysqlAuctionListingRepository implements AuctionListingRepository {
    private final AuctionListingRepository delegate;

    public MysqlAuctionListingRepository(MysqlAuctionListingStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean initialize() {
        return delegate.initialize();
    }

    @Override
    public com.skyblockexp.ezauction.storage.AuctionStorageSnapshot load() {
        return delegate.load();
    }

    @Override
    public void saveListings(java.util.Collection<com.skyblockexp.ezauction.AuctionListing> listings, java.util.Collection<com.skyblockexp.ezauction.AuctionOrder> orders) {
        delegate.saveListings(listings, orders);
    }

    @Override
    public void saveReturns(java.util.Map<java.util.UUID, java.util.List<org.bukkit.inventory.ItemStack>> returnsByPlayer) {
        delegate.saveReturns(returnsByPlayer);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public java.util.Optional<com.skyblockexp.ezauction.AuctionListing> find(String id) throws Exception {
        return delegate.find(id);
    }

    @Override
    public java.util.List<com.skyblockexp.ezauction.AuctionListing> findAll() throws Exception {
        return delegate.findAll();
    }

    @Override
    public void save(com.skyblockexp.ezauction.AuctionListing entity) throws Exception {
        delegate.save(entity);
    }

    @Override
    public void delete(String id) throws Exception {
        delegate.delete(id);
    }
}
