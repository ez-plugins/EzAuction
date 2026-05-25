package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.model.AuctionListing;
import com.skyblockexp.ezauction.config.AuctionBackendMessages;
import com.skyblockexp.ezauction.live.component.LiveAuctionComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LiveAuctionEnqueueListener implements Listener {
    private final LiveAuctionService liveService;
    private final AuctionBackendMessages.LiveMessages messages;

    public LiveAuctionEnqueueListener(LiveAuctionService liveService,
            AuctionBackendMessages.LiveMessages messages) {
        this.liveService = liveService;
        this.messages = messages != null ? messages : AuctionBackendMessages.LiveMessages.defaults();
    }

    /** Backward-compatible constructor without messages. */
    public LiveAuctionEnqueueListener(LiveAuctionService liveService) {
        this(liveService, AuctionBackendMessages.LiveMessages.defaults());
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

        String itemName = LiveAuctionComponentBuilder.describeItem(listing.item());
        boolean enqueued = liveService.enqueue(listing, seller.getUniqueId(), seller.getName());
        if (!enqueued) {
            seller.sendMessage(colorize(messages.alreadyQueued()));
            return;
        }

        if (!liveService.isQueueEnabled()) {
            seller.sendMessage(colorize(fmt(messages.queuedInstant(), "item", itemName)));
        } else {
            int pos = liveService.queuePositionOf(seller.getUniqueId());
            seller.sendMessage(colorize(fmt(messages.queued(), "item", itemName,
                    "position", String.valueOf(pos))));
        }
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String fmt(String template, String... kvPairs) {
        String result = template;
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            result = result.replace("{" + kvPairs[i] + "}", kvPairs[i + 1] != null ? kvPairs[i + 1] : "");
        }
        return colorize(result);
    }
}
