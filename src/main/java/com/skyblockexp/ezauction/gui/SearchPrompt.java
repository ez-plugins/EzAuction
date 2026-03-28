package com.skyblockexp.ezauction.gui;

public final class SearchPrompt {

    private final BrowserView view;
    private final int page;

    public SearchPrompt(BrowserView view, int page) {
        this.view = view;
        this.page = page;
    }

    public BrowserView view() {
        return view;
    }

    public int page() {
        return page;
    }
}
