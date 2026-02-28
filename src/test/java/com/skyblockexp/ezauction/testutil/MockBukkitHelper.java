package com.skyblockexp.ezauction.testutil;

public final class MockBukkitHelper {

    private static volatile boolean initialized = false;

    private MockBukkitHelper() {}

    public static synchronized void ensureMocked() {
        if (initialized) return;
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            try {
                java.lang.reflect.Method getServer = bukkit.getMethod("getServer");
                Object srv = getServer.invoke(null);
                if (srv != null) {
                    // server already exists, MockBukkit likely initialized
                    initialized = true;
                    return;
                }
            } catch (NoSuchMethodException ignored) {
            }
        } catch (ClassNotFoundException e) {
            // Bukkit not on classpath; nothing to do
            return;
        } catch (Exception ignored) {
        }

        try {
            Class<?> mock = Class.forName("org.mockbukkit.mockbukkit.MockBukkit");
            // try to unmock first to ensure a clean state
            try {
                java.lang.reflect.Method unmock = mock.getMethod("unmock");
                unmock.invoke(null);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {
            }
            java.lang.reflect.Method mockMethod = mock.getMethod("mock");
            mockMethod.invoke(null);
            initialized = true;
        } catch (ClassNotFoundException e) {
            // MockBukkit not available; tests will run without it
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize MockBukkit", ex);
        }
    }

    public static synchronized void ensureUnmocked() {
        if (!initialized) return;
        try {
            Class<?> mock = Class.forName("org.mockbukkit.mockbukkit.MockBukkit");
            java.lang.reflect.Method unmock = mock.getMethod("unmock");
            unmock.invoke(null);
        } catch (ClassNotFoundException e) {
            return;
        } catch (NoSuchMethodException e) {
            return;
        } catch (Exception ignored) {
        } finally {
            initialized = false;
        }
    }
}
