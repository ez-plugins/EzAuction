package com.skyblockexp.ezauction.live;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import com.skyblockexp.ezauction.AuctionListing;

public class LiveAuctionEnqueueListener implements Listener {
    private final LiveAuctionService liveService;

    public LiveAuctionEnqueueListener(LiveAuctionService liveService) {
        this.liveService = liveService;
    }

    @EventHandler
    public void onListingCreated(AuctionListingCreatedEvent event) {
        Player seller = event.getSeller();
        if (seller == null) return;

        // Only handle if this seller came from /liveauction sell
        if (!LiveSellContext.consume(seller.getUniqueId())) {
            return;
        }

        AuctionListing listing = event.getListing();
        if (listing == null) return;

        // Enqueue listing into live auction
        liveService.enqueue(listing, seller.getUniqueId(), seller.getName());
    }
}
