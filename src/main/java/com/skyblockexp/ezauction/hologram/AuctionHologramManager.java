package com.skyblockexp.ezauction.hologram;

import com.skyblockexp.ezauction.AuctionManager;
import com.skyblockexp.ezauction.transaction.AuctionTransactionService;
import com.skyblockexp.ezauction.compat.HologramDisplay;
import com.skyblockexp.ezauction.compat.HologramPlatform;
import com.skyblockexp.ezauction.config.AuctionHologramConfiguration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
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

    public boolean ensureHologram(Location location, AuctionHologramType type) {
        if (location == null || type == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Location spawnLocation = normalizeLocation(location.clone());
        spawnLocation.add(0.0D, configuration.heightOffset(), 0.0D);

        HologramDisplay existing = findExistingDisplay(spawnLocation, type);
        if (existing != null) {
            registerDisplay(existing, type);
            updateDisplay(existing, type);
            return true;
        }

        int chunkX = spawnLocation.getBlockX() >> 4;
        int chunkZ = spawnLocation.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return false;
            }
        }

        // Enforce max hologram count
        if (trackedDisplays.size() >= configuration.maxHolograms()) {
            plugin.getLogger().warning("Max auction hologram count reached (" + configuration.maxHolograms() + "). Placement denied.");
            return false;
        }
        HologramDisplay created = hologramPlatform.spawn(spawnLocation);
        if (created == null) {
            return false;
        }
        hologramPlatform.configureBase(created);
        markDisplay(created, type);
        registerDisplay(created, type);
        updateDisplay(created, type);
        return true;
    }

    public boolean removeNearest(Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Location searchLocation = normalizeLocation(location.clone());
        double radius = configuration.searchRadius();
        Collection<HologramDisplay> nearby = hologramPlatform.findNearby(searchLocation, radius);
        HologramDisplay closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (HologramDisplay display : nearby) {
            AuctionHologramType type = readType(display);
            if (type == null) {
                continue;
            }
            double distance = display.location().distanceSquared(searchLocation);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = display;
            }
        }
        if (closest == null) {
            return false;
        }
        trackedDisplays.remove(closest.uniqueId());
        closest.remove();
        return true;
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
            for (Map.Entry<UUID, HologramEntry> entry : trackedDisplays.entrySet()) {
                HologramDisplay display = entry.getValue().display();
                if (!isDisplayValid(display)) {
                    trackedDisplays.remove(entry.getKey());
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
}
