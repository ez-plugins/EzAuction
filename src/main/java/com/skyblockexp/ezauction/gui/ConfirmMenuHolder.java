package com.skyblockexp.ezauction.gui;

import java.util.UUID;

public final class ConfirmMenuHolder extends AbstractAuctionHolder {

    private final int previousPage;
    private final BrowserView view;
    private final ConfirmAction action;

    public ConfirmMenuHolder(UUID owner, int previousPage, BrowserView view, ConfirmAction action) {
        super(owner);
        this.previousPage = previousPage;
        this.view = view;
        this.action = action;
    }

    public int previousPage() {
        return previousPage;
    }

    public BrowserView view() {
        return view;
    }

    public ConfirmAction action() {
        return action;
    }
}
