package com.skyblockexp.ezauction.live.registry;

import com.skyblockexp.ezauction.live.model.LiveAuctionState;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which live auction each player has explicitly joined (their "active" auction session).
 *
 * <p>A player may only be an active participant in one live auction at a time. Joining a new
 * auction automatically replaces the previous session. This registry does NOT manage EzCountdown
 * notifications; those handles live on {@link LiveAuctionState#notificationHandlesSnapshot()}.</p>
 */
public final class LiveBidSessionRegistry {

    /** Maps player UUID → the auction ID they are actively following. */
    private final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();

    /**
     * Registers the player as an active participant of the given auction.
     * Any existing session for this player is silently replaced.
     */
    public void join(UUID player, LiveAuctionState state) {
        activeSessions.put(player, state.id());
        state.addParticipant(player);
    }

    /**
     * Removes the player from their current session.
     *
     * @return the auction ID they left, or empty if they were not in any session
     */
    public Optional<String> leave(UUID player) {
        String auctionId = activeSessions.remove(player);
        return Optional.ofNullable(auctionId);
    }

    /**
     * Returns the ID of the auction the player is currently following, if any.
     */
    public Optional<String> getActiveAuctionId(UUID player) {
        return Optional.ofNullable(activeSessions.get(player));
    }

    /**
     * Returns whether the player is currently an active participant in any live auction.
     */
    public boolean isParticipating(UUID player) {
        return activeSessions.containsKey(player);
    }

    /**
     * Removes ALL participants of the given state from the registry (called when an auction ends).
     *
     * @param state the auction that just ended
     */
    public void clearAuction(LiveAuctionState state) {
        activeSessions.entrySet().removeIf(e -> e.getValue().equals(state.id()));
    }

    /**
     * Returns an unmodifiable view of active sessions (player UUID → auction ID).
     */
    public Map<UUID, String> activeSessions() {
        return Collections.unmodifiableMap(activeSessions);
    }

    /**
     * Returns the set of player UUIDs currently participating in the given auction.
     */
    public Collection<UUID> participantsOf(String auctionId) {
        return activeSessions.entrySet().stream()
                .filter(e -> e.getValue().equals(auctionId))
                .map(Map.Entry::getKey)
                .toList();
    }
}
