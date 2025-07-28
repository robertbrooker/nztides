package com.palliser.nztides.notification;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.palliser.nztides.TideService;
import com.palliser.nztides.Constants;
import com.palliser.nztides.NextTideInfo;
import com.palliser.nztides.R;

/**
 * Background service that maintains the persistent tide notification
 * Uses a foreground service to ensure reliable updates
 */
public class TideNotificationService extends Service {
    
    private static final String TAG = "TideNotificationService";
    
    private NotificationHelper notificationHelper;
    private TideService tideService;
    private String currentPort;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        notificationHelper = new NotificationHelper(this);
        tideService = TideService.getInstance();
        
        // Load current port from preferences
        loadCurrentPort();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        updateNotification();

        // Return START_STICKY to restart service if killed
        return START_STICKY;
    }
    
    /**
     * Updates the tide notification with current information
     */
    private void updateNotification() {
        if (currentPort == null || currentPort.trim().isEmpty()) {
            Log.w(TAG, "No current port set, cannot update notification");
            return;
        }
        
        try {
            // Load tide data directly for this port
            java.util.List<com.palliser.nztides.TideRecord> tides = 
                tideService.loadPortData(getAssets(), currentPort);
            
            if (tides != null && !tides.isEmpty()) {
                long currentTime = System.currentTimeMillis() / 1000;
                NextTideInfo nextTide = tideService.getNextTideInfo(tides, currentTime);
                
                if (nextTide != null) {
                    notificationHelper.showTideNotification(nextTide, currentPort);
                    Log.d(TAG, "Updated notification for " + currentPort + ": " + nextTide);
                } else {
                    // Show error notification
                    notificationHelper.showErrorNotification(currentPort, 
                            getString(R.string.error_calculating_tides));
                    Log.w(TAG, "Failed to calculate tide info for " + currentPort);
                }
            } else {
                // Show error notification
                notificationHelper.showErrorNotification(currentPort, 
                        "No tide data available");
                Log.w(TAG, "No tide data available for " + currentPort);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
            notificationHelper.showErrorNotification(currentPort, 
                    getString(R.string.error_updating_notification));
        }
    }
    
    /**
     * Loads the current port from shared preferences
     */
    private void loadCurrentPort() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        currentPort = prefs.getString(Constants.PREFS_CURRENT_PORT, Constants.DEFAULT_PORT);
        Log.d(TAG, "Loaded current port: " + currentPort);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        // Cancel notification when service is destroyed
        if (notificationHelper != null) {
            notificationHelper.cancelTideNotification();
        }
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't support binding
        return null;
    }
    
    /**
     * Static method to start the service
     */
    public static void startService(android.content.Context context) {
        Intent intent = new Intent(context, TideNotificationService.class);
        context.startService(intent);
    }
    
    /**
     * Static method to stop the service
     */
    public static void stopService(android.content.Context context) {
        Intent intent = new Intent(context, TideNotificationService.class);
        context.stopService(intent);
    }
    
    /**
     * Static method to trigger notification update
     */
    public static void updateNotification(android.content.Context context) {
        Intent intent = new Intent(context, TideNotificationService.class);
        intent.setAction(Constants.ACTION_UPDATE_NOTIFICATION);
        context.startService(intent);
    }
}
