package com.skyblockexp.ezauction.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;

public final class SellMenuHolder implements org.bukkit.inventory.InventoryHolder {

    public enum Target { NORMAL, LIVE }

    private final UUID owner;
    private final SellMenuState state;
    private Inventory inventory;
    private Target target = Target.NORMAL;

    public SellMenuHolder(UUID owner, SellMenuState state) {
        this.owner = owner;
        this.state = state;
    }

    public UUID owner() { return owner; }
    public SellMenuState state() { return state; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public Target target() { return target; }
    public void setTarget(Target target) { this.target = target == null ? Target.NORMAL : target; }
}
