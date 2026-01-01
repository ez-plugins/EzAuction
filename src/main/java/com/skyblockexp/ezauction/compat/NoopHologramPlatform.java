package com.skyblockexp.ezauction.compat;

import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Disabled hologram platform for legacy servers without display entities.
 */
public final class NoopHologramPlatform implements HologramPlatform {

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public HologramDisplay spawn(Location location) {
        return null;
    }

    @Override
    public Collection<HologramDisplay> findDisplays(World world) {
        return List.of();
    }

    @Override
    public Collection<HologramDisplay> findNearby(Location location, double radius) {
        return List.of();
    }

    @Override
    public boolean isDisplayEntity(Entity entity) {
        return false;
    }

    @Override
    public HologramDisplay wrap(Entity entity) {
        return null;
    }

    @Override
    public void configureBase(HologramDisplay display) {
        // No-op
    }

    @Override
    public void setMarker(HologramDisplay display, String key, String value) {
        // No-op
    }

    @Override
    public String getMarker(HologramDisplay display, String key) {
        return null;
    }
}
