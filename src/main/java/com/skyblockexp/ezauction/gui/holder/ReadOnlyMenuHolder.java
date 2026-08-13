package com.skyblockexp.ezauction.gui.holder;

import java.util.UUID;

/**
 * A generic read-only inventory holder for display-only GUIs (e.g. buy-order overviews).
 * Extending {@link AbstractAuctionHolder} ensures that {@code AuctionMenu}'s click and
 * drag listeners automatically cancel all player interactions with the inventory.
 */
public final class ReadOnlyMenuHolder extends AbstractAuctionHolder {

    public ReadOnlyMenuHolder(UUID owner) {
        super(owner);
    }
}
