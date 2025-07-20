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
        
        Log.i(TAG, "NZ Tides application starting - using lazy loading for tide data");
        
        // With lazy loading, we no longer need to initialize all tide data at startup
        // Data will be loaded on-demand when a port is first accessed
        onTideDataReady();
        
        // Run tests to verify lazy loading functionality in background thread
        new Thread(() -> {
            Boolean testSuccess = TideDataCacheTest.runTests(this);
            if (testSuccess != null && testSuccess) {
                Log.i(TAG, "All cache tests passed!");
            } else {
                Log.w(TAG, "Some cache tests failed - check logs");
            }
        }).start();
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
        
        // Clean up resources
        TideRepository.getInstance().shutdown();
        Log.i(TAG, "NZ Tides application terminated");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        Log.w(TAG, "Low memory warning received");
        // In extreme cases, we could clear the cache here
        // But for our relatively small dataset, it's probably better to keep it
    }
}
