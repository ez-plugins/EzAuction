package com.skyblockexp.ezauction.gui.holder;
import com.skyblockexp.ezauction.gui.view.ActivityTab;

import java.util.UUID;

/**
 * Inventory holder for the My Activity menu.
 * Extends {@link AbstractAuctionHolder} so that the shared drag and click guards
 * in {@code AuctionMenu} automatically protect this inventory from item theft.
 */
public class ActivityMenuHolder extends AbstractAuctionHolder {
    private final ActivityTab tab;

    public ActivityMenuHolder(UUID owner, ActivityTab tab) {
        super(owner);
        this.tab = tab;
    }

    public ActivityTab tab() {
        return tab;
    }
}
