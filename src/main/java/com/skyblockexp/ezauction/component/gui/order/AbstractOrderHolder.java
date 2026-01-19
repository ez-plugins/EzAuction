package com.skyblockexp.ezauction.component.gui.order;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public abstract class AbstractOrderHolder implements InventoryHolder {
    private final UUID owner;
    private Inventory inventory;

    protected AbstractOrderHolder(UUID owner) {
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
