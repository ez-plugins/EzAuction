package com.skyblockexp.ezauction.test;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.mockbukkit.mockbukkit.MockBukkit;

public class MockBukkitTestExecutionListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
            // Use getOrCreateMock to avoid exceptions if already mocked and ensure server is available
            MockBukkit.getOrCreateMock();
        } catch (Throwable ignored) {
            // ignore
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
            MockBukkit.unmock();
        } catch (Throwable ignored) {
            // ignore
        }
    }
}
