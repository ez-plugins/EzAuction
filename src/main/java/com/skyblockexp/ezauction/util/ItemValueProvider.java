package com.skyblockexp.ezauction.util;

import java.util.OptionalDouble;
import org.bukkit.inventory.ItemStack;

/**
 * Provides estimated prices for items displayed in the auction menu.
 */
@FunctionalInterface
public interface ItemValueProvider {

    ItemValueProvider NONE = item -> OptionalDouble.empty();

    OptionalDouble estimate(ItemStack itemStack);

    static ItemValueProvider none() {
        return NONE;
    }
}
