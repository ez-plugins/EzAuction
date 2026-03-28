package com.skyblockexp.ezauction.gui;

import java.util.UUID;

public final class BrowserMenuHolder extends AbstractAuctionHolder {

    private final int page;
    private final BrowserView view;

    public BrowserMenuHolder(UUID owner, int page, BrowserView view) {
        super(owner);
        this.page = page;
        this.view = view;
    }

    public int page() {
        return page;
    }

    public BrowserView view() {
        return view;
    }
}
