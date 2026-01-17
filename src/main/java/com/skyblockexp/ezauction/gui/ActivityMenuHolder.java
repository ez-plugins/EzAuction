package com.skyblockexp.ezauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Inventory holder for the My Activity menu.
 */
public class ActivityMenuHolder implements InventoryHolder {
    private final UUID owner;
    private final ActivityTab tab;
    private Inventory inventory;

    public ActivityMenuHolder(UUID owner, ActivityTab tab) {
        this.owner = owner;
        this.tab = tab;
    }

    public UUID owner() {
        return owner;
    }

    public ActivityTab tab() {
        return tab;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
