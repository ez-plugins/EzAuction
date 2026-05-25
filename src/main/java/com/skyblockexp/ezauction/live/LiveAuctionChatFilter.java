package com.skyblockexp.ezauction.live;

import com.skyblockexp.ezauction.live.preferences.PlayerLivePreferencesManager;
import com.skyblockexp.ezauction.live.registry.LiveBidSessionRegistry;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.UUID;

/**
 * Optional chat filter that removes chat messages from the audience of players who have opted into
 * {@code muteOtherChatInAuction} while they are actively participating in a live auction.
 *
 * <p>Only registered when {@code chat-filter-feature-enabled: true} is set in {@code auction.yml}.
 * Operates on {@link AsyncChatEvent} viewers, which represents the set of players who will see
 * the message. This does NOT prevent the sender from seeing their own message in Paper.</p>
 */
public final class LiveAuctionChatFilter implements Listener {

    private final LiveBidSessionRegistry sessionRegistry;
    private final PlayerLivePreferencesManager preferencesManager;

    public LiveAuctionChatFilter(LiveBidSessionRegistry sessionRegistry,
            PlayerLivePreferencesManager preferencesManager) {
        this.sessionRegistry = sessionRegistry;
        this.preferencesManager = preferencesManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        // Iterate over a snapshot to avoid ConcurrentModificationException
        UUID senderId = event.getPlayer().getUniqueId();
        Iterator<Audience> it = event.viewers().iterator();
        while (it.hasNext()) {
            Audience viewer = it.next();
            if (!(viewer instanceof Player viewerPlayer)) continue;
            UUID viewerId = viewerPlayer.getUniqueId();
            // Never filter the sender themselves
            if (viewerId.equals(senderId)) continue;
            if (sessionRegistry.isParticipating(viewerId)
                    && preferencesManager.getPreferences(viewerId).muteOtherChatInAuction()) {
                it.remove();
            }
        }
    }
}
