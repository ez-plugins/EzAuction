package com.skyblockexp.ezauction.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockbukkit.mockbukkit.MockBukkit;

public class MockBukkitGlobalExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        try {
            MockBukkit.getOrCreateMock();
        } catch (Throwable ignored) {
            // If MockBukkit is not available or already mocked, ignore
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
            // ignore
        }
    }
}
