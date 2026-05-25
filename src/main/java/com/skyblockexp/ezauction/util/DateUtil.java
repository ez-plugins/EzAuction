package com.skyblockexp.ezauction.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    private static final String DEFAULT_DATE_PATTERN = "dd-MM-yy HH:mm:ss";
    private static volatile DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);

    /**
     * Updates the date/time format pattern used by {@link #formatDate(long)} and
     * {@link #formatDate(Date)}. Changes take effect immediately and are reflected in all
     * subsequent calls. Invalid patterns are silently ignored and the previous formatter is kept.
     *
     * @param pattern {@code java.time.format.DateTimeFormatter} pattern string (e.g. {@code "dd-MM-yy HH:mm:ss"})
     */
    public static void setDateFormat(String pattern) {
        if (pattern == null || pattern.isBlank()) return;
        try {
            dateFormatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException ignored) {
            // Invalid pattern — keep the existing formatter
        }
    }

    public static String formatDate(long epochMillis) {
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        return dateFormatter.format(dt);
    }

    public static String formatDate(Date date) {
        if (date == null) return "";
        return formatDate(date.getTime());
    }

    public static Duration parseDuration(String input) {
        if (input == null) return null;
        String trimmed = input.trim().toLowerCase(java.util.Locale.ENGLISH);
        if (trimmed.isEmpty()) return null;

        // normalize permissive variants: "12 h", "12hours" -> "12h"
        trimmed = trimmed.replaceAll("\\s+", "");
        trimmed = trimmed.replace("hours", "h");
        trimmed = trimmed.replace("hour", "h");
        trimmed = trimmed.replace("hrs", "h");
        trimmed = trimmed.replace("minutes", "m");
        trimmed = trimmed.replace("minute", "m");
        trimmed = trimmed.replace("mins", "m");
        trimmed = trimmed.replace("seconds", "s");
        trimmed = trimmed.replace("second", "s");
        trimmed = trimmed.replace("days", "d");
        trimmed = trimmed.replace("day", "d");

        if (trimmed.isEmpty()) return null;

        char last = trimmed.charAt(trimmed.length() - 1);
        String numeric = trimmed;
        long unitSeconds;
        switch (last) {
            case 'd': unitSeconds = 86400; numeric = trimmed.substring(0, trimmed.length() - 1); break;
            case 'h': unitSeconds = 3600; numeric = trimmed.substring(0, trimmed.length() - 1); break;
            case 'm': unitSeconds = 60; numeric = trimmed.substring(0, trimmed.length() - 1); break;
            case 's': unitSeconds = 1; numeric = trimmed.substring(0, trimmed.length() - 1); break;
            default: unitSeconds = 3600; break; // default to hours
        }

        if (numeric.isEmpty()) return null;

        double value;
        try {
            value = Double.parseDouble(numeric);
        } catch (NumberFormatException ex) {
            return null;
        }

        if (value <= 0.0) return null;

        long seconds = Math.round(value * unitSeconds);
        if (seconds <= 0) return null;

        try {
            return Duration.ofSeconds(seconds);
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    public static String formatDuration(Duration duration) {
        if (duration == null) return "0s";
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");
        if (sb.length() == 0) sb.append("0m");
        return sb.toString().trim();
    }
}
