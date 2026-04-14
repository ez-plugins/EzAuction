package com.skyblockexp.ezauction.util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

    public static String formatDate(long epochMillis) {
        return FORMAT.format(new Date(epochMillis));
    }

    public static String formatDate(Date date) {
        return FORMAT.format(date);
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
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
