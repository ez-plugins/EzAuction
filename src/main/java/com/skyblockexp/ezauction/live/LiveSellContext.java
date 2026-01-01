package com.skyblockexp.ezauction.live;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which players initiated a live-auction sell flow. */
public final class LiveSellContext {
    private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();

    private LiveSellContext() {}

    public static void mark(UUID playerId) {
        if (playerId != null) LIVE.add(playerId);
    }

    /** Returns true if the player was in live-sell mode and clears that state. */
    public static boolean consume(UUID playerId) {
        return playerId != null && LIVE.remove(playerId);
    }
}
