package com.skyblockexp.ezauction.compat;

import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Platform hooks for spawning and tracking hologram displays.
 */
public interface HologramPlatform {

    boolean isSupported();

    HologramDisplay spawn(Location location);

    Collection<HologramDisplay> findDisplays(World world);

    Collection<HologramDisplay> findNearby(Location location, double radius);

    boolean isDisplayEntity(Entity entity);

    HologramDisplay wrap(Entity entity);

    void configureBase(HologramDisplay display);

    void setMarker(HologramDisplay display, String key, String value);

    String getMarker(HologramDisplay display, String key);
}
