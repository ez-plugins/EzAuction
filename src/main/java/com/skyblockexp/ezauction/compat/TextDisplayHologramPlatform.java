package com.skyblockexp.ezauction.compat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TextDisplay-backed hologram implementation for 1.19+ servers.
 */
public final class TextDisplayHologramPlatform implements HologramPlatform {

    private final JavaPlugin plugin;

    public TextDisplayHologramPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
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
        TextDisplay display = world.spawn(location, TextDisplay.class);
        return new TextDisplayWrapper(display);
    }

    @Override
    public Collection<HologramDisplay> findDisplays(World world) {
        if (world == null) {
            return List.of();
        }
        return world.getEntitiesByClass(TextDisplay.class)
                .stream()
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
        return world.getNearbyEntities(location, radius, radius, radius, entity -> entity instanceof TextDisplay)
                .stream()
                .map(entity -> (TextDisplay) entity)
                .map(TextDisplayWrapper::new)
                .map(HologramDisplay.class::cast)
                .toList();
    }

    @Override
    public boolean isDisplayEntity(Entity entity) {
        return entity instanceof TextDisplay;
    }

    @Override
    public HologramDisplay wrap(Entity entity) {
        if (entity instanceof TextDisplay display) {
            return new TextDisplayWrapper(display);
        }
        return null;
    }

    @Override
    public void configureBase(HologramDisplay display) {
        if (!(display instanceof TextDisplayWrapper wrapper)) {
            return;
        }
        TextDisplay handle = wrapper.handle();
        if (handle == null) {
            return;
        }
        handle.setBillboard(Display.Billboard.CENTER);
        handle.setPersistent(true);
        handle.setShadowed(false);
        handle.setSeeThrough(true);
        handle.setAlignment(TextDisplay.TextAlignment.CENTER);
        handle.setViewRange(48.0F);
        handle.setLineWidth(160);
        handle.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    }

    @Override
    public void setMarker(HologramDisplay display, String key, String value) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null || value == null) {
            return;
        }
        TextDisplay handle = wrapper.handle();
        if (handle == null) {
            return;
        }
        PersistentDataContainer container = handle.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
    }

    @Override
    public String getMarker(HologramDisplay display, String key) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null) {
            return null;
        }
        TextDisplay handle = wrapper.handle();
        if (handle == null) {
            return null;
        }
        PersistentDataContainer container = handle.getPersistentDataContainer();
        return container.get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
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
            try {
                Method textMethod = display.getClass().getMethod("text", Component.class);
                textMethod.invoke(display, text);
            } catch (NoSuchMethodException ex) {
                try {
                    Method setTextMethod = display.getClass().getMethod("setText", String.class);
                    String legacy = LegacyComponentSerializer.legacySection().serialize(text);
                    setTextMethod.invoke(display, legacy);
                } catch (Exception ignored) {
                    // No compatible method found
                }
            } catch (Exception ex) {
                // Ignore reflection failures
            }
        }
    }
}
