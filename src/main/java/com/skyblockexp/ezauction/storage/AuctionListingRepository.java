package com.skyblockexp.ezauction.storage;

import com.skyblockexp.ezauction.AuctionListing;
import com.skyblockexp.ezauction.AuctionOrder;
import com.skyblockexp.ezframework.storage.Repository;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Repository abstraction for auction listings, orders and returns.
 *
 * Combines the CRUD `Repository` contract with the persistence methods
 * previously defined on the legacy storage interface so concrete
 * implementations can implement a single interface.
 */
public interface AuctionListingRepository extends Repository<AuctionListing, String> {
	boolean initialize();

	AuctionStorageSnapshot load();

	void saveListings(Collection<AuctionListing> listings, Collection<AuctionOrder> orders);

	void saveReturns(Map<UUID, List<ItemStack>> returnsByPlayer);

	@Override
	void close();

	@Override
	Optional<AuctionListing> find(String id) throws Exception;

	@Override
	List<AuctionListing> findAll() throws Exception;

	@Override
	void save(AuctionListing entity) throws Exception;

	@Override
	void delete(String id) throws Exception;
}
