package com.skyblockexp.ezauction.compat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TextDisplay-backed hologram implementation for 1.19+ servers.
 */
public final class TextDisplayHologramPlatform implements HologramPlatform {

    private final JavaPlugin plugin;

    public TextDisplayHologramPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if TextDisplay is supported on this server version.
     * Uses reflection to verify presence of required classes and methods.
     */
    @Override
    public boolean isSupported() {
        try {
            Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            textDisplayClass.getMethod("text", Component.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            try {
                Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
                textDisplayClass.getMethod("setText", String.class);
                return true;
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
        }
        return false;
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
            Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            Entity entity = world.spawn(location, (Class<Entity>) textDisplayClass);
            if (textDisplayClass.isInstance(entity)) {
                return new TextDisplayWrapper(entity);
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public Collection<HologramDisplay> findDisplays(World world) {
        if (world == null) {
            return List.of();
        }
        try {
            Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            return world.getEntitiesByClass((Class<Entity>) textDisplayClass)
                    .stream()
                    .map(entity -> textDisplayClass.isInstance(entity) ? new TextDisplayWrapper(entity) : null)
                    .filter(e -> e != null)
                    .map(HologramDisplay.class::cast)
                    .toList();
        } catch (Exception e) {
        }
        return List.of();
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
        try {
            Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            return world.getNearbyEntities(location, radius, radius, radius, entity -> textDisplayClass.isInstance(entity))
                    .stream()
                    .map(entity -> textDisplayClass.isInstance(entity) ? new TextDisplayWrapper(entity) : null)
                    .filter(e -> e != null)
                    .map(HologramDisplay.class::cast)
                    .toList();
        } catch (Exception e) {
        }
        return List.of();
    }

    @Override
    public boolean isDisplayEntity(Entity entity) {
        try {
            Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
            return textDisplayClass.isInstance(entity);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public HologramDisplay wrap(Entity entity) {
        try {
            if (entity != null) {
                Class<?> textDisplayClass = Class.forName("org.bukkit.entity.TextDisplay");
                if (textDisplayClass.isInstance(entity)) {
                    return new TextDisplayWrapper(entity);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public void configureBase(HologramDisplay display) {
        if (!(display instanceof TextDisplayWrapper wrapper)) {
            return;
        }
        Entity handle = wrapper.handle();
        if (handle == null) {
            return;
        }
        try {
            // Use reflection for all method calls
            Class<?> displayClass = handle.getClass();
            Method setBillboard = displayClass.getMethod("setBillboard", displayClass.getClass().getClassLoader().loadClass("org.bukkit.entity.Display$Billboard"));
            Object centerBillboard = displayClass.getClass().getClassLoader().loadClass("org.bukkit.entity.Display$Billboard").getField("CENTER").get(null);
            setBillboard.invoke(handle, centerBillboard);

            Method setPersistent = displayClass.getMethod("setPersistent", boolean.class);
            setPersistent.invoke(handle, true);

            Method setShadowed = displayClass.getMethod("setShadowed", boolean.class);
            setShadowed.invoke(handle, false);

            Method setSeeThrough = displayClass.getMethod("setSeeThrough", boolean.class);
            setSeeThrough.invoke(handle, true);

            Method setAlignment = displayClass.getMethod("setAlignment", displayClass.getClass().getClassLoader().loadClass("org.bukkit.entity.TextDisplay$TextAlignment"));
            Object centerAlignment = displayClass.getClass().getClassLoader().loadClass("org.bukkit.entity.TextDisplay$TextAlignment").getField("CENTER").get(null);
            setAlignment.invoke(handle, centerAlignment);

            Method setViewRange = displayClass.getMethod("setViewRange", float.class);
            setViewRange.invoke(handle, 48.0F);

            Method setLineWidth = displayClass.getMethod("setLineWidth", int.class);
            setLineWidth.invoke(handle, 160);

            Method setBackgroundColor = displayClass.getMethod("setBackgroundColor", int.class);
            setBackgroundColor.invoke(handle, 0);
        } catch (Exception e) {
            // Some methods may not exist on legacy versions
        }
    }

    @Override
    public void setMarker(HologramDisplay display, String key, String value) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null || value == null) {
            return;
        }
        Entity handle = wrapper.handle();
        if (handle == null) {
            return;
        }
        try {
            Method getPDC = handle.getClass().getMethod("getPersistentDataContainer");
            Object container = getPDC.invoke(handle);
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Object namespacedKey = namespacedKeyClass.getConstructor(JavaPlugin.class, String.class).newInstance(plugin, key);
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = persistentDataTypeClass.getField("STRING").get(null);
            Method setMethod = container.getClass().getMethod("set", namespacedKeyClass, persistentDataTypeClass, Object.class);
            setMethod.invoke(container, namespacedKey, stringType, value);
        } catch (Exception e) {
        }
    }

    @Override
    public String getMarker(HologramDisplay display, String key) {
        if (!(display instanceof TextDisplayWrapper wrapper) || key == null) {
            return null;
        }
        Entity handle = wrapper.handle();
        if (handle == null) {
            return null;
        }
        try {
            Method getPDC = handle.getClass().getMethod("getPersistentDataContainer");
            Object container = getPDC.invoke(handle);
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
            Object namespacedKey = namespacedKeyClass.getConstructor(JavaPlugin.class, String.class).newInstance(plugin, key);
            Class<?> persistentDataTypeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
            Object stringType = persistentDataTypeClass.getField("STRING").get(null);
            Method getMethod = container.getClass().getMethod("get", namespacedKeyClass, persistentDataTypeClass);
            Object result = getMethod.invoke(container, namespacedKey, stringType);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
        }
        return null;
    }

    private static final class TextDisplayWrapper implements HologramDisplay {

        private final Entity display;

        private TextDisplayWrapper(Entity display) {
            this.display = display;
        }

        private Entity handle() {
            return display;
        }

        @Override
        public UUID uniqueId() {
            try {
                Method getUniqueId = display.getClass().getMethod("getUniqueId");
                return (UUID) getUniqueId.invoke(display);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Location location() {
            try {
                Method getLocation = display.getClass().getMethod("getLocation");
                return (Location) getLocation.invoke(display);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean isValid() {
            try {
                Method isValid = display.getClass().getMethod("isValid");
                Method isDead = display.getClass().getMethod("isDead");
                return (Boolean) isValid.invoke(display) && !(Boolean) isDead.invoke(display);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void remove() {
            try {
                Method remove = display.getClass().getMethod("remove");
                remove.invoke(display);
            } catch (Exception e) {
            }
        }

        /**
         * Sets the text for the hologram display, using reflection to support both modern and legacy APIs.
         * Falls back to legacy string serialization if necessary.
         */
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
                }
            } catch (Exception ex) {
            }
        }
    }
}
