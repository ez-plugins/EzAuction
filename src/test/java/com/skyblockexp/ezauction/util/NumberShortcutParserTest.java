package com.skyblockexp.ezauction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberShortcutParserTest {
    @Test
    void parsesThousands() {
        assertEquals(3000.0, NumberShortcutParser.parse("3k"));
        assertEquals(2500.0, NumberShortcutParser.parse("2.5k"));
        assertEquals(0.0, NumberShortcutParser.parse("0k"));
    }

    @Test
    void parsesMillions() {
        assertEquals(5000000.0, NumberShortcutParser.parse("5m"));
        assertEquals(1000000.0, NumberShortcutParser.parse("1M"));
    }

    @Test
    void parsesBillions() {
        assertEquals(10000000000.0, NumberShortcutParser.parse("10b"));
        assertEquals(1_500_000_000.0, NumberShortcutParser.parse("1.5B"));
    }

    @Test
    void parsesTrillions() {
        assertEquals(4000000000000.0, NumberShortcutParser.parse("4t"));
        assertEquals(0.5e12, NumberShortcutParser.parse("0.5T"));
    }

    @Test
    void parsesPlainNumbers() {
        assertEquals(123.0, NumberShortcutParser.parse("123"));
        assertEquals(0.0, NumberShortcutParser.parse("0"));
        assertEquals(42.42, NumberShortcutParser.parse("42.42"));
    }

    @Test
    void throwsOnInvalid() {
        assertThrows(IllegalArgumentException.class, () -> NumberShortcutParser.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> NumberShortcutParser.parse("1x"));
        assertThrows(IllegalArgumentException.class, () -> NumberShortcutParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> NumberShortcutParser.parse(null));
    }
}
