package com.skyblockexp.ezauction.live.preferences;
import com.skyblockexp.ezauction.compat.Scheduler;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * In-memory cache backed by a {@link PlayerLivePreferencesStorage} implementation.
 *
 * <p>All cache misses trigger a synchronous storage load and populate the cache.
 * Saves are written through to storage immediately and scheduled asynchronously so as
 * not to block the calling thread.</p>
 */
public final class PlayerLivePreferencesManager {

    private final PlayerLivePreferencesStorage storage;
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerLivePreferences> cache = new ConcurrentHashMap<>();

    public PlayerLivePreferencesManager(JavaPlugin plugin, PlayerLivePreferencesStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /** Pre-loads all stored preferences into the cache (call during plugin enable). */
    public void loadAll() {
        try {
            Collection<PlayerLivePreferences> all = storage.loadAll();
            for (PlayerLivePreferences pref : all) {
                cache.put(pref.playerId(), pref);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[EzAuction] Failed to pre-load live player preferences.", ex);
        }
    }

    /**
     * Returns the preferences for the given player, loading from storage if not yet cached.
     * Never returns null - falls back to {@link PlayerLivePreferences#defaults(UUID)}.
     */
    public PlayerLivePreferences getPreferences(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> {
            try {
                PlayerLivePreferences loaded = storage.load(id);
                return loaded != null ? loaded : PlayerLivePreferences.defaults(id);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "[EzAuction] Failed to load live preferences for " + id, ex);
                return PlayerLivePreferences.defaults(id);
            }
        });
    }

    /**
     * Persists the given preferences, updating the cache and writing to storage asynchronously.
     */
    public void savePreferences(PlayerLivePreferences preferences) {
        cache.put(preferences.playerId(), preferences);
        Scheduler.runAsync(plugin, () -> {
            try {
                storage.save(preferences);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "[EzAuction] Failed to save live preferences for " + preferences.playerId(), ex);
            }
        });
    }

    /**
     * Evicts the cache entry for the given player (e.g. on disconnect to free memory).
     */
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }

    public void close() {
        try {
            storage.close();
        } catch (Exception ignored) {
        }
    }
}
