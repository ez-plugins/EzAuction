package com.skyblockexp.ezauction.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

    public static String formatDate(long epochMillis) {
        return FORMAT.format(new Date(epochMillis));
    }

    public static String formatDate(Date date) {
        return FORMAT.format(date);
    }
}
