package com.skyblockexp.ezauction.gui;

import java.util.UUID;

public final class PreviewMenuHolder extends AbstractAuctionHolder {

    private final BrowserView originView;
    private final int originPage;
    private final ConfirmAction confirmAction;
    private final String targetId;

    public PreviewMenuHolder(UUID owner, BrowserView originView, int originPage,
                             ConfirmAction confirmAction, String targetId) {
        super(owner);
        this.originView = originView;
        this.originPage = originPage;
        this.confirmAction = confirmAction;
        this.targetId = targetId;
    }

    public BrowserView originView() {
        return originView;
    }

    public int originPage() {
        return originPage;
    }

    public ConfirmAction confirmAction() {
        return confirmAction;
    }

    public String targetId() {
        return targetId;
    }
}
