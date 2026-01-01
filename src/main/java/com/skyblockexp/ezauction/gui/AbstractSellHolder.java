package com.skyblockexp.ezauction.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Base holder class for sell GUIs so we can attach owner + inventory. */
public abstract class AbstractSellHolder implements InventoryHolder {

    private final UUID owner;
    private Inventory inventory;

    protected AbstractSellHolder(UUID owner) {
        this.owner = owner;
    }

    public UUID owner() {
        return owner;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
