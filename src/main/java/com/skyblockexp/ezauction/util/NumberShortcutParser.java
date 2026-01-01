package com.skyblockexp.ezauction.util;

public class NumberShortcutParser {
    /**
     * Parses a string with number shortcuts (k, m, b, t) into a double.
     * Supports case-insensitive suffixes and decimal values (e.g., 2.5m).
     * Throws IllegalArgumentException for invalid formats.
     *
     * @param input the input string (e.g., "3k", "2.5m", "10b", "4t")
     * @return the parsed double value
     */
    public static double parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        String trimmed = input.trim().toLowerCase();
        double multiplier = 1.0;
        if (trimmed.endsWith("k")) {
            multiplier = 1_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("m")) {
            multiplier = 1_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        } else if (trimmed.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try {
            double value = Double.parseDouble(trimmed);
            return value * multiplier;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + input);
        }
    }
}
