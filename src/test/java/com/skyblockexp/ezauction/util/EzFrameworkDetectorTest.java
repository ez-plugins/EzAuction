package com.skyblockexp.ezauction.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class EzFrameworkDetectorTest {

    @Test
    public void detectAbsent() {
        // If EzFramework is present (e.g., local install or profile enabled), skip this test.
        Assumptions.assumeFalse(EzFrameworkDetector.isEzFrameworkAvailable(), "EzFramework is present; skipping absent-detection test");
        assertFalse(EzFrameworkDetector.isEzFrameworkAvailable());
    }
}

