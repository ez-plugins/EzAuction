package com.skyblockexp.ezauction.compat;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

/**
 * Lightweight hologram display abstraction.
 */
public interface HologramDisplay {

    UUID uniqueId();

    Location location();

    boolean isValid();

    void remove();

    void setText(Component text);
}
