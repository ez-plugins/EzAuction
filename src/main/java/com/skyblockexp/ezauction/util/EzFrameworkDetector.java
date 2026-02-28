package com.skyblockexp.ezauction.util;

/**
 * Lightweight runtime detector for EzFramework presence.
 */
public final class EzFrameworkDetector {

    private EzFrameworkDetector() {
    }

    /**
     * Returns true if `com.skyblockexp.ezframework.EzPlugin` is loadable.
     */
    public static boolean isEzFrameworkAvailable() {
        try {
            Class.forName("com.skyblockexp.ezframework.EzPlugin", false, EzFrameworkDetector.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
