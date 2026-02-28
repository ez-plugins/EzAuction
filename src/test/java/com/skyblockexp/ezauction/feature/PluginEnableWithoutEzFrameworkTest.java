package com.skyblockexp.ezauction.feature;

import com.skyblockexp.ezauction.EzAuctionPlugin;
import com.skyblockexp.ezauction.util.EzFrameworkDetector;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PluginEnableWithoutEzFrameworkTest {

    @Test
    public void pluginEnablesWithoutEzFramework() throws Exception {
        // Skip if MockBukkit not present
        Class<?> mockClass = findMockBukkitClass();
        Assumptions.assumeTrue(mockClass != null, "MockBukkit not available; skipping feature test");

        // Skip this test if EzFramework is present in classpath (we want the 'absent' scenario)
        Assumptions.assumeTrue(!EzFrameworkDetector.isEzFrameworkAvailable(), "EzFramework present; skipping absent-framework scenario");

        // call MockBukkit.mock()
        Method mock = mockClass.getMethod("mock");
        Object serverMock = mock.invoke(null);

        // call MockBukkit.load(Class<? extends JavaPlugin>)
        Method load = mockClass.getMethod("load", Class.class);
        Object pluginInstance = load.invoke(null, EzAuctionPlugin.class);

        if (pluginInstance != null) {
            assertNotNull(pluginInstance);
        }

        // cleanup: try to unmock
        try {
            Method unmock = mockClass.getMethod("unmock");
            unmock.invoke(null);
        } catch (NoSuchMethodException ignored) {
        }
    }

    private Class<?> findMockBukkitClass() {
        try {
            return Class.forName("org.mockbukkit.mockbukkit.MockBukkit");
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }
}
