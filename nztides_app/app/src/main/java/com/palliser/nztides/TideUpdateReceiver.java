package com.palliser.nztides;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * BroadcastReceiver that handles scheduled tide notification updates
 * and device boot events
 */
public class TideUpdateReceiver extends BroadcastReceiver {
    
    private static final String TAG = "TideUpdateReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if (Constants.ACTION_UPDATE_NOTIFICATION.equals(action)) {
            // Scheduled update
            handleScheduledUpdate(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Device reboot
            handleBootCompleted(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) || 
                   Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            // App updated
            handleAppUpdated(context);
        }
    }
    
    /**
     * Handles scheduled notification updates
     */
    private void handleScheduledUpdate(Context context) {
        // Check if notifications are enabled in preferences
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, false);
        
        if (notificationsEnabled) {
            Log.d(TAG, "Triggering scheduled notification update");
            TideNotificationService.updateNotification(context);
            
            // For Android 6+, we need to reschedule the next exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scheduleNextExactAlarm(context);
            }
        } else {
            Log.d(TAG, "Notifications disabled, skipping update");
        }
    }
    
    /**
     * Schedules the next exact alarm for Android 6+
     */
    private void scheduleNextExactAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int updateIntervalMinutes = prefs.getInt(Constants.PREFS_UPDATE_FREQUENCY, Constants.DEFAULT_UPDATE_INTERVAL_MINUTES);
        long updateIntervalMillis = updateIntervalMinutes * 60 * 1000L;
        
        Intent intent = new Intent(context, TideUpdateReceiver.class);
        intent.setAction(Constants.ACTION_UPDATE_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        long nextTriggerTime = System.currentTimeMillis() + updateIntervalMillis;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent);
            Log.d(TAG, "Scheduled next exact alarm for " + updateIntervalMinutes + " minutes from now");
        }
    }
    
    /**
     * Handles device boot completion
     */
    private void handleBootCompleted(Context context) {
        Log.d(TAG, "Device boot completed, checking if notifications should be restored");
        
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, false);
        
        if (notificationsEnabled) {
            Log.d(TAG, "Restoring tide notifications after boot");
            scheduleNotificationUpdates(context);
            TideNotificationService.startService(context);
        }
    }
    
    /**
     * Handles app updates
     */
    private void handleAppUpdated(Context context) {
        Log.d(TAG, "App updated, checking notification settings");
        
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, false);
        
        if (notificationsEnabled) {
            Log.d(TAG, "Re-enabling notifications after app update");
            scheduleNotificationUpdates(context);
            TideNotificationService.startService(context);
        }
    }
    
    /**
     * Schedules periodic notification updates using AlarmManager
     */
    public static void scheduleNotificationUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return;
        }
        
        Intent intent = new Intent(context, TideUpdateReceiver.class);
        intent.setAction(Constants.ACTION_UPDATE_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Get update frequency from preferences
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        int updateIntervalMinutes = prefs.getInt(Constants.PREFS_UPDATE_FREQUENCY, Constants.DEFAULT_UPDATE_INTERVAL_MINUTES);
        long updateIntervalMillis = updateIntervalMinutes * 60 * 1000L;
        
        // Cancel any existing alarms
        alarmManager.cancel(pendingIntent);
        
        // Schedule repeating alarm
        long triggerTime = System.currentTimeMillis() + updateIntervalMillis;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use setExactAndAllowWhileIdle for better reliability on newer Android versions
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            
            // For repeating, we need to reschedule manually in the receiver
            Log.d(TAG, "Scheduled exact alarm for " + updateIntervalMinutes + " minutes from now");
        } else {
            // Use setRepeating for older versions
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, updateIntervalMillis, pendingIntent);
            Log.d(TAG, "Scheduled repeating alarm every " + updateIntervalMinutes + " minutes");
        }
    }
    
    /**
     * Cancels scheduled notification updates
     */
    public static void cancelNotificationUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        Intent intent = new Intent(context, TideUpdateReceiver.class);
        intent.setAction(Constants.ACTION_UPDATE_NOTIFICATION);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Cancelled scheduled notification updates");
    }
}
