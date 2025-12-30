// app/src/main/java/com/example/expensetracker/util/TimeFormat.java
package com.example.expensetracker.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormat {
    private static final SimpleDateFormat SMS_DTF =
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());

    private static final SimpleDateFormat UI_DTF =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private static final SimpleDateFormat CSV_DTF =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static long parseSmsDateTimeToMillis(String s) {
        if (s == null) return System.currentTimeMillis();
        try {
            Date d = SMS_DTF.parse(s.trim());
            if (d == null) return System.currentTimeMillis();
            return d.getTime();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }

    public static String formatForUi(long millis) {
        return UI_DTF.format(new Date(millis));
    }

    public static String formatForCsv(long millis) {
        return CSV_DTF.format(new Date(millis));
    }
}
