package com.palliser.nztides;

import android.app.Application;
import android.util.Log;

/**
 * Application class for NZ Tides
 * Handles global initialization
 */
public class NZTidesApplication extends Application {
    private static final String TAG = "NZTidesApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "NZ Tides application starting - using on-demand data loading");
        
        // Data will be loaded fresh on-demand when a port is accessed
        onTideDataReady();
        
        
        Log.i(TAG, "Application ready - tide data will be loaded on demand");
    }
    
    /**
     * Called when tide data is successfully loaded and ready
     */
    private void onTideDataReady() {
        Log.d(TAG, "Tide data ready - can now enable full functionality");
        // Here we could enable notifications, widgets, etc.
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        Log.i(TAG, "NZ Tides application terminated");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        Log.w(TAG, "Low memory warning received");
    }
}
