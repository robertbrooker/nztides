package com.palliser.nztides;

/**
 * Application-wide constants to avoid hardcoded strings
 */
public final class Constants {

    public static final String PREFS_NAME = "NZTidesPrefsFile";
    public static final String PREFS_RECENT_PORTS = "RecentPorts";
    public static final String PREFS_CURRENT_PORT = "CurrentPort";
    public static final String PREFS_NOTIFICATIONS_ENABLED = "NotificationsEnabled";
    public static final String PREFS_UPDATE_FREQUENCY = "UpdateFrequency";
    
    // Notification System
    public static final String NOTIFICATION_CHANNEL_ID = "TIDE_UPDATES";
    public static final int TIDE_NOTIFICATION_ID = 1001;
    public static final int DEFAULT_UPDATE_INTERVAL_MINUTES = 10;
    
    // Intent Actions
    public static final String ACTION_UPDATE_NOTIFICATION = "com.palliser.nztides.UPDATE_NOTIFICATION";

    // Default Values
    public static final String DEFAULT_PORT = "Auckland";
    public static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;
    public static final int RECENT_PORTS_COUNT = 3;
    public static final int RECORDS_TO_DISPLAY = 35 * 4; // About 35 days of tides

    private Constants() {
        // Prevent instantiation
    }
}
