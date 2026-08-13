package com.skyblockexp.ezauction.hologram;

import com.skyblockexp.ezauction.service.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages EzAuction holographic displays backed by version-specific entities.
 */
public final class AuctionHologramManager {

    private final JavaPlugin plugin;
    private final AuctionManager auctionManager;
    private final AuctionTransactionService transactionService;
    private final AuctionHologramConfiguration configuration;
    private final HologramPlatform hologramPlatform;
    private final Map<UUID, HologramEntry> trackedDisplays = new HashMap<>();

    private String markerKey;
    private BukkitTask updateTask;

    public AuctionHologramManager(JavaPlugin plugin, AuctionManager auctionManager,
            AuctionTransactionService transactionService, AuctionHologramConfiguration configuration,
            HologramPlatform hologramPlatform) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.configuration = configuration != null ? configuration : AuctionHologramConfiguration.defaults();
        this.hologramPlatform = Objects.requireNonNull(hologramPlatform, "hologramPlatform");
    }

    public void enable() {
        if (!hologramPlatform.isSupported()) {
            return;
        }
        markerKey = "auction_hologram_type";
        attachExistingDisplays();
        long interval = configuration.updateIntervalTicks();
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateTrackedDisplays, 40L,
                Math.max(20L, interval));
    }

    public void disable() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        trackedDisplays.clear();
        markerKey = null;
    }

    public Location ensureHologram(Location location, AuctionHologramType type) {
        if (location == null || type == null) {
            return null;
        }
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Location spawnLocation = normalizeLocation(location.clone());
        spawnLocation.add(0.0D, configuration.heightOffset(), 0.0D);

        HologramDisplay existing = findExistingDisplay(spawnLocation, type);
        if (existing != null) {
            registerDisplay(existing, type);
            updateDisplay(existing, type);
            return spawnLocation;
        }

        int chunkX = spawnLocation.getBlockX() >> 4;
        int chunkZ = spawnLocation.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return null;
            }
        }

        // Enforce max hologram count
        if (trackedDisplays.size() >= configuration.maxHolograms()) {
            plugin.getLogger().warning("Max auction hologram count reached (" + configuration.maxHolograms() + "). Placement denied.");
            return null;
        }
        HologramDisplay created = hologramPlatform.spawn(spawnLocation);
        if (created == null) {
            return null;
        }
        hologramPlatform.configureBase(created);
        markDisplay(created, type);
        registerDisplay(created, type);
        updateDisplay(created, type);
        return spawnLocation;
    }

    /**
     * Finds EzAuction holograms near the given origin, sorted by ascending distance.
     *
     * @param origin          search centre; use the player's eye location so the vertical
     *                        placement offset is covered
     * @param radius          spherical search radius in blocks
     * @param includeUntagged when {@code true}, also returns untagged {@code TextDisplay} entities
     * @return matches sorted nearest-first; never {@code null}
     */
    public List<HologramMatch> findNearby(Location origin, double radius, boolean includeUntagged) {
        if (origin == null) {
            return List.of();
        }
        World world = origin.getWorld();
        if (world == null) {
            return List.of();
        }
        double effectiveRadius = radius > 0.0D ? radius : configuration.removalRadius();
        List<HologramMatch> matches = new ArrayList<>();
        for (HologramDisplay display : hologramPlatform.findNearby(origin, effectiveRadius)) {
            AuctionHologramType type = readType(display);
            if (type == null && !includeUntagged) {
                continue;
            }
            double distance = distanceTo(display, origin);
            if (distance >= 0.0D) {
                matches.add(new HologramMatch(display, type, distance));
            }
        }
        matches.sort(Comparator.comparingDouble(HologramMatch::distance));
        return matches;
    }

    public double removalRadius() {
        return configuration.removalRadius();
    }

    public double maxRemovalRadius() {
        return configuration.maxRemovalRadius();
    }

    public boolean allowUntaggedRemoval() {
        return configuration.allowUntaggedRemoval();
    }

    /**
     * Removes a single matched hologram. Untracks it by unique id then removes the entity.
     *
     * @return {@code true} if the display was valid and removed
     */
    public boolean remove(HologramMatch match) {
        if (match == null || match.display() == null) {
            return false;
        }
        UUID id = match.display().uniqueId();
        if (id != null) {
            trackedDisplays.remove(id);
        }
        if (match.display().isValid()) {
            match.display().remove();
            return true;
        }
        return false;
    }

    /**
     * Removes all matched holograms.
     *
     * @return the number of displays actually removed
     */
    public int removeAll(Collection<HologramMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (HologramMatch match : matches) {
            if (remove(match)) {
                removed++;
            }
        }
        return removed;
    }

    private double distanceTo(HologramDisplay display, Location origin) {
        Location location = display.location();
        if (location == null || origin.getWorld() == null || location.getWorld() == null) {
            return -1.0D;
        }
        if (!location.getWorld().equals(origin.getWorld())) {
            return -1.0D;
        }
        return location.distance(origin);
    }

    public Map<AuctionHologramType, Integer> trackedCounts() {
        Map<AuctionHologramType, Integer> counts = new EnumMap<>(AuctionHologramType.class);
        for (HologramEntry entry : trackedDisplays.values()) {
            counts.merge(entry.type(), 1, Integer::sum);
        }
        return counts;
    }

    private void attachExistingDisplays() {
        for (World world : plugin.getServer().getWorlds()) {
            for (HologramDisplay display : hologramPlatform.findDisplays(world)) {
                AuctionHologramType type = readType(display);
                if (type != null) {
                    hologramPlatform.configureBase(display);
                    registerDisplay(display, type);
                }
            }
        }
    }

    private void updateTrackedDisplays() {
        if (trackedDisplays.isEmpty()) {
            return;
        }
        // Batch update optimization
        if (configuration.batchUpdate()) {
            trackedDisplays.entrySet().removeIf(entry -> {
                HologramDisplay display = entry.getValue().display();
                if (!isDisplayValid(display)) {
                    return true;
                }
                try {
                    updateDisplay(display, entry.getValue().type());
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to update auction hologram display.", ex);
                }
                return false;
            });
        } else {
            // Update only a subset per tick for scalability
            int batchSize = Math.max(1, trackedDisplays.size() / 10);
            int count = 0;
            Iterator<Map.Entry<UUID, HologramEntry>> iterator = trackedDisplays.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, HologramEntry> entry = iterator.next();
                HologramDisplay display = entry.getValue().display();
                if (!isDisplayValid(display)) {
                    iterator.remove();
                    continue;
                }
                try {
                    updateDisplay(display, entry.getValue().type());
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to update auction hologram display.", ex);
                }
                if (++count >= batchSize) {
                    break;
                }
            }
        }
    }

    private void updateDisplay(HologramDisplay display, AuctionHologramType type) {
        if (display == null || type == null) {
            return;
        }
        net.kyori.adventure.text.Component text = type.render(auctionManager, transactionService);
        if (text != null) {
            display.setText(text);
        }
    }

    private void registerDisplay(HologramDisplay display, AuctionHologramType type) {
        if (display == null || type == null) {
            return;
        }
        trackedDisplays.put(display.uniqueId(), new HologramEntry(display, type));
        markDisplay(display, type);
        hologramPlatform.configureBase(display);
    }

    private void markDisplay(HologramDisplay display, AuctionHologramType type) {
        if (display == null || type == null || markerKey == null) {
            return;
        }
        hologramPlatform.setMarker(display, markerKey, type.name());
    }

    public AuctionHologramConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isHologramEntity(Entity entity) {
        return hologramPlatform.isDisplayEntity(entity);
    }

    public AuctionHologramType readType(Entity entity) {
        HologramDisplay display = hologramPlatform.wrap(entity);
        return readType(display);
    }

    public AuctionHologramType readType(HologramDisplay display) {
        if (display == null || markerKey == null) {
            return null;
        }
        String raw = hologramPlatform.getMarker(display, markerKey);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AuctionHologramType.valueOf(raw.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return AuctionHologramType.fromName(raw);
        }
    }

    private HologramDisplay findExistingDisplay(Location location, AuctionHologramType type) {
        if (location == null || type == null) {
            return null;
        }
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        double radius = configuration.searchRadius();
        Collection<HologramDisplay> nearby = hologramPlatform.findNearby(location, radius);
        for (HologramDisplay display : nearby) {
            AuctionHologramType existingType = readType(display);
            if (existingType == type) {
                return display;
            }
        }
        return null;
    }

    private boolean isDisplayValid(HologramDisplay display) {
        return display != null && display.isValid();
    }

    private Location normalizeLocation(Location location) {
        Location clone = location.clone();
        clone.setX(clone.getBlockX() + 0.5D);
        clone.setZ(clone.getBlockZ() + 0.5D);
        clone.setPitch(0.0F);
        clone.setYaw(0.0F);
        return clone;
    }

    private record HologramEntry(HologramDisplay display, AuctionHologramType type) {
    }

    /**
     * A single hologram discovered by a removal or listing search.
     *
     * @param display the underlying display entity
     * @param type    the EzAuction hologram type, or {@code null} for an untagged display
     * @param distance distance in blocks from the search origin
     */
    public record HologramMatch(HologramDisplay display, AuctionHologramType type, double distance) {
    }
}
