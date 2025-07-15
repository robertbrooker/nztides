package com.palliser.nztides;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * Helper class for building and managing tide notifications
 */
public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private final Context context;
    private final NotificationChannelManager channelManager;
    
    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.channelManager = new NotificationChannelManager(context);
    }
    
    /**
     * Creates and displays a tide notification
     * @param tideInfo Information about the next tide
     * @param portName Name of the current port
     */
    public void showTideNotification(NextTideInfo tideInfo, String portName) {
        if (tideInfo == null) {
            Log.w(TAG, "Cannot show notification: tide info is null");
            return;
        }
        
        if (channelManager.doesNotHaveNotificationPermission()) {
            Log.w(TAG, "Cannot show notification: permission denied");
            return;
        }
        
        // Ensure notification channel exists
        channelManager.createTideNotificationChannel();
        
        String title = String.format("Next %s", tideInfo.getTideTypeString());
        String content = String.format("%s • %s", tideInfo.getTimeRemainingString(), portName);
        String bigText = String.format("%s\n%s\nHeight: %.1fm", 
                tideInfo.getTimeRemainingString(), 
                TideFormatter.formatHourMinute(tideInfo.getTimestamp()),
                tideInfo.getHeight());
        
        // Create intent to open the main activity
        Intent intent = new Intent(context, NZTides.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Makes it persistent
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(false); // Don't show timestamp
        
        // Add action to open app
        PendingIntent openAppIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        builder.addAction(R.drawable.icon, context.getString(R.string.notification_action_open), openAppIntent);
        
        try {
            NotificationManager notificationManager = channelManager.getNotificationManager();
            if (notificationManager != null) {
                notificationManager.notify(Constants.TIDE_NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Tide notification updated: " + title + " - " + content);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }
    
    /**
     * Cancels the tide notification
     */
    public void cancelTideNotification() {
        try {
            NotificationManager notificationManager = channelManager.getNotificationManager();
            if (notificationManager != null) {
                notificationManager.cancel(Constants.TIDE_NOTIFICATION_ID);
                Log.d(TAG, "Tide notification cancelled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification", e);
        }
    }
    
    /**
     * Shows an error notification when tide data cannot be loaded
     */
    public void showErrorNotification(String portName, String errorMessage) {
        if (channelManager.doesNotHaveNotificationPermission()) {
            return;
        }
        
        channelManager.createTideNotificationChannel();
        
        String title = context.getString(R.string.notification_error_title);
        String content = String.format("%s: %s", portName, errorMessage);
        
        Intent intent = new Intent(context, NZTides.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        
        try {
            NotificationManager notificationManager = channelManager.getNotificationManager();
            if (notificationManager != null) {
                notificationManager.notify(Constants.TIDE_NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error notification", e);
        }
    }
}
