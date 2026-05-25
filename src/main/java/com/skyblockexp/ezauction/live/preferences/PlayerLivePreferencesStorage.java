package com.skyblockexp.ezauction.live.preferences;

import java.util.Collection;
import java.util.UUID;

/**
 * Persistence contract for {@link PlayerLivePreferences}.
 */
public interface PlayerLivePreferencesStorage {

    /** Initialises the backing store. Returns true on success. */
    boolean initialize();

    /**
     * Loads preferences for the given player. Returns {@code null} if no record exists
     * (callers should substitute {@link PlayerLivePreferences#defaults(UUID)}).
     */
    PlayerLivePreferences load(UUID playerId);

    /** Persists or upserts the given preferences record. */
    void save(PlayerLivePreferences preferences);

    /** Loads all stored player preferences at once (used for bulk cache warming). */
    Collection<PlayerLivePreferences> loadAll();

    /** Releases any resources held by this storage. */
    default void close() {}
}
