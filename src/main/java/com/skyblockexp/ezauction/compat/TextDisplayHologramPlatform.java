package com.skyblockexp.ezauction.compat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TextDisplay-backed hologram implementation for 1.19+ servers.
 *
 * <p>The project targets Paper 1.21+ / Java 21, so {@link TextDisplay}, {@link Display.Billboard},
 * {@link TextDisplay.TextAlignment} and the persistent data container are all available at compile
 * time. Direct typed calls are used instead of reflection, which previously hid marker and
 * configuration failures behind swallowed exceptions.
 */
public final class TextDisplayHologramPlatform implements HologramPlatform {

    private final JavaPlugin plugin;
    private final NamespacedKey markerKey;

    public TextDisplayHologramPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "auction_hologram_type");
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public HologramDisplay spawn(Location location) {
        if (location == null) {
            return null;
        }
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        try {
            TextDisplay entity = world.spawn(location, TextDisplay.class);
            return new TextDisplayWrapper(entity);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn auction hologram TextDisplay: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Collection<HologramDisplay> findDisplays(World world) {
        if (world == null) {
            return List.of();
        }
        return world.getEntitiesByClass(TextDisplay.class).stream()
                .map(TextDisplayWrapper::new)
                .map(HologramDisplay.class::cast)
                .toList();
    }

    @Override
    public Collection<HologramDisplay> findNearby(Location location, double radius) {
        if (location == null) {
            return List.of();
        }
        World world = location.getWorld();
        if (world == null) {
            return List.of();
        }
        double box = Math.max(0.0D, radius);
        List<HologramDisplay> boxed = world.getNearbyEntities(location, box, box, box,
                        entity -> entity instanceof TextDisplay)
                .stream()
                .map(entity -> (TextDisplay) entity)
                .map(TextDisplayWrapper::new)
                .map(HologramDisplay.class::cast)
                .toList();
        if (box >= Double.MAX_VALUE / 2.0D) {
            return boxed;
        }
        double squared = radius * radius;
        return boxed.stream()
                .filter(display -> {
                    Location loc = display.location();
                    return loc != null && loc.getWorld() == world
                            && loc.distanceSquared(location) <= squared;
                })
                .sorted(Comparator.comparingDouble(display -> {
                    Location loc = display.location();
                    return loc == null ? Double.MAX_VALUE : loc.distanceSquared(location);
                }))
                .toList();
    }

    @Override
    public boolean isDisplayEntity(Entity entity) {
        return entity instanceof TextDisplay;
    }

    @Override
    public HologramDisplay wrap(Entity entity) {
        if (entity instanceof TextDisplay textDisplay) {
            return new TextDisplayWrapper(textDisplay);
        }
        return null;
    }

    @Override
    public void configureBase(HologramDisplay display) {
        if (!(display instanceof TextDisplayWrapper wrapper)) {
            return;
        }
        TextDisplay entity = wrapper.handle();
        if (entity == null) {
            return;
        }
        entity.setBillboard(Display.Billboard.CENTER);
        entity.setPersistent(true);
        entity.setShadowed(false);
        entity.setSeeThrough(true);
        entity.setAlignment(TextDisplay.TextAlignment.CENTER);
        entity.setViewRange(48.0F);
        entity.setLineWidth(160);
        entity.setBackgroundColor(Color.fromARGB(0));
    }

    @Override
    public void setMarker(HologramDisplay display, String key, String value) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null || value == null) {
            return;
        }
        TextDisplay entity = wrapper.handle();
        if (entity == null) {
            return;
        }
        entity.getPersistentDataContainer().set(markerKeyFor(key), PersistentDataType.STRING, value);
    }

    @Override
    public String getMarker(HologramDisplay display, String key) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null) {
            return null;
        }
        TextDisplay entity = wrapper.handle();
        if (entity == null) {
            return null;
        }
        return entity.getPersistentDataContainer().get(markerKeyFor(key), PersistentDataType.STRING);
    }

    private NamespacedKey markerKeyFor(String key) {
        if (key.equals(markerKey.getKey())) {
            return markerKey;
        }
        return new NamespacedKey(plugin, key);
    }

    private static final class TextDisplayWrapper implements HologramDisplay {

        private final TextDisplay display;

        private TextDisplayWrapper(TextDisplay display) {
            this.display = display;
        }

        private TextDisplay handle() {
            return display;
        }

        @Override
        public UUID uniqueId() {
            return display.getUniqueId();
        }

        @Override
        public Location location() {
            return display.getLocation();
        }

        @Override
        public boolean isValid() {
            return display.isValid() && !display.isDead();
        }

        @Override
        public void remove() {
            display.remove();
        }

        @Override
        public void setText(Component text) {
            if (text == null) {
                return;
            }
            display.text(text);
        }
    }
}
