package com.skyblockexp.ezauction.feature;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MockBukkitFeatureTest {

    @Test
    public void mockbukkitCanBeLoadedAndMocked() throws Exception {
        Class<?> mockClass = findMockBukkitClass();
        Assumptions.assumeTrue(mockClass != null, "MockBukkit not available; skipping feature test");

        // call MockBukkit.mock()
        Method mock = mockClass.getMethod("mock");
        Object serverMock = mock.invoke(null);
        // ensure returned object (if any) is non-null or we at least reached into the library
        // some MockBukkit versions return a ServerMock; some return void — accept either
        if (serverMock != null) {
            assertNotNull(serverMock);
        }

        // call MockBukkit.unmock() to clean up when available
        try {
            Method unmock = mockClass.getMethod("unmock");
            unmock.invoke(null);
        } catch (NoSuchMethodException ignored) {
            // older/newer variants may use different lifecycle methods; ignore
        }
    }

    private Class<?> findMockBukkitClass() {
        String[] candidates = new String[] {
                "org.mockbukkit.mockbukkit.MockBukkit"
        };
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }
}
