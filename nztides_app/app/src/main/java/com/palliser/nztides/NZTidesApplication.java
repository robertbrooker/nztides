package com.palliser.nztides;

import android.app.Application;
import android.util.Log;

/**
 * Application class for NZ Tides
 * Handles global initialization including tide data cache
 */
public class NZTidesApplication extends Application {
    private static final String TAG = "NZTidesApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "NZ Tides application starting - using simplified on-demand data loading");
        
        // With simplified loading, we no longer need to initialize any caches
        // Data will be loaded fresh on-demand when a port is accessed
        onTideDataReady();
        
        // Note: TideDataCacheTest is no longer relevant with simplified approach
        // Run tests to verify simplified functionality in background thread
        new Thread(() -> {
            Boolean testSuccess = SimpleTideServiceTest.runTests(this);
            if (testSuccess != null && testSuccess) {
                Log.i(TAG, "All simplified service tests passed!");
            } else {
                Log.w(TAG, "Some simplified service tests failed - check logs");
            }
        }).start();
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
        
        // No cleanup needed with simplified approach
        Log.i(TAG, "NZ Tides application terminated");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        Log.w(TAG, "Low memory warning received - no caches to clear with simplified approach");
    }
}
