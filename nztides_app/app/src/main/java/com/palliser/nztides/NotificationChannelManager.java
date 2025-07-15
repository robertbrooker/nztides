package com.palliser.nztides;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Manages notification channels and permission checks for the tide notification system
 */
public class NotificationChannelManager {
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public NotificationChannelManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    /**
     * Creates the notification channel for tide updates.
     * Must be called before posting any notifications.
     */
    public void createTideNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            );
            
            channel.setDescription(context.getString(R.string.notification_channel_description));
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Checks if the app has permission to post notifications.
     * For Android 13+ (API 33), this requires explicit permission.
     */
    public boolean doesNotHaveNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        }
        // For older versions, check if notifications are enabled
        return !NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
    
    /**
     * Checks if notifications are enabled for this app
     */
    public boolean areNotificationsEnabled() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
    
    /**
     * Checks if the specific tide notification channel is enabled
     */
    public boolean isTideChannelEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID);
            return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return areNotificationsEnabled();
    }
    
    /**
     * Gets the notification manager instance
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
}
