package com.skyblockexp.ezauction.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class DateUtilTest {

    @Test
    void parseCommonDurations() {
        Duration d1 = DateUtil.parseDuration("12h");
        assertNotNull(d1);
        assertEquals(12, d1.toHours());

        Duration d2 = DateUtil.parseDuration("30m");
        assertNotNull(d2);
        assertEquals(30, d2.toMinutes());

        Duration d3 = DateUtil.parseDuration("2d");
        assertNotNull(d3);
        assertEquals(48, d3.toHours());

        Duration d4 = DateUtil.parseDuration("1.5h");
        assertNotNull(d4);
        assertEquals(90, d4.toMinutes());
    }

    @Test
    void parsePermissiveVariants() {
        Duration a = DateUtil.parseDuration("12 h");
        assertNotNull(a);
        assertEquals(12, a.toHours());

        Duration b = DateUtil.parseDuration("12hours");
        assertNotNull(b);
        assertEquals(12, b.toHours());

        assertNull(DateUtil.parseDuration(""));
        assertNull(DateUtil.parseDuration("abc"));
    }
}
