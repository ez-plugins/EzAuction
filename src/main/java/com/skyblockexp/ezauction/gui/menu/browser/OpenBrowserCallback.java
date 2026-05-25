package com.skyblockexp.ezauction.gui.menu.browser;

import org.bukkit.entity.Player;

import com.skyblockexp.ezauction.gui.view.BrowserView;

/**
 * Callback used by browser sub-handlers to (re)open the browser inventory.
 */
@FunctionalInterface
public interface OpenBrowserCallback {
    void open(Player player, BrowserView view, int page);
}
