package com.palliser.nztides;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting tide-related text and timestamps
 */
public class TideFormatter {
    private static final DecimalFormat HEIGHT_FORMAT = new DecimalFormat(" 0.00;-0.00");
    private static final DecimalFormat CURRENT_HEIGHT_FORMAT = new DecimalFormat(" 0.0;-0.0");
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("00");
    private static final DecimalFormat RISE_RATE_FORMAT = new DecimalFormat("0");
    
    private static final SimpleDateFormat FULL_DATE_FORMAT = new SimpleDateFormat("HH:mm E dd/MM/yy zzz");
    private static final SimpleDateFormat HOUR_MINUTE_FORMAT = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("E dd");
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMM yyyy");
    
    public static String formatHeight(float height) {
        return HEIGHT_FORMAT.format(height);
    }

    public static String formatCurrentHeight(double height) {
        return CURRENT_HEIGHT_FORMAT.format(height);
    }

    public static String formatRiseRate(double riseRate) {
        return RISE_RATE_FORMAT.format(riseRate);
    }

    public static String formatTime(int timeValue) {
        return TIME_FORMAT.format(timeValue);
    }

    public static String formatFullDate(long timestamp) {
        return FULL_DATE_FORMAT.format(new Date(1000 * timestamp));
    }
    
    public static String formatHourMinute(long timestamp) {
        return HOUR_MINUTE_FORMAT.format(new Date(1000 * timestamp));
    }
    
    public static String formatDay(long timestamp) {
        return DAY_FORMAT.format(new Date(1000 * timestamp));
    }
    
    public static String formatMonth(long timestamp) {
        return MONTH_FORMAT.format(new Date(1000 * timestamp));
    }

    /**
     * Format a TideData object for display
     */
    public static String formatTideRecord(TideData tideData) {
        return " " + (tideData.isHighTide() ? " HIGH " : "  low ")
                + formatHourMinute(tideData.getTimestamp())
                + formatHeight(tideData.getHeight()) + "m\n";
    }
    
    /**
     * Format time duration in hours and minutes
     */
    public static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds / 60) % 60;
        return hours + "h" + formatTime(minutes) + "m";
    }
}
