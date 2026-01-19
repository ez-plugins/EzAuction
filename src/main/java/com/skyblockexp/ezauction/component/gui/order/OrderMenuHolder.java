package com.skyblockexp.ezauction.component.gui.order;

import java.util.UUID;

public class OrderMenuHolder extends AbstractOrderHolder {
    private final OrderMenuState state;

    public OrderMenuHolder(UUID owner, OrderMenuState state) {
        super(owner);
        this.state = state;
    }

    public OrderMenuState state() {
        return state;
    }
}
