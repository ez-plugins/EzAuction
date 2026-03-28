package com.skyblockexp.ezauction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public abstract class AbstractAuctionHolder implements InventoryHolder {

    private final UUID owner;
    private Inventory inventory;

    protected AbstractAuctionHolder(UUID owner) {
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
