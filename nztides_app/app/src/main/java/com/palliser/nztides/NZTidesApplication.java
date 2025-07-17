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
        
        Log.i(TAG, "NZ Tides application starting");
        
        // Initialize tide data cache asynchronously
        TideRepository.getInstance()
            .initializeAsync(getAssets())
            .thenAccept(success -> {
                if (success) {
                    Log.i(TAG, "Tide data initialization completed successfully");
                    onTideDataReady();
                    
                    // Run tests to verify cache functionality
                    TideDataCacheTest.runTests(this)
                        .thenAccept(testSuccess -> {
                            if (testSuccess) {
                                Log.i(TAG, "All cache tests passed!");
                            } else {
                                Log.w(TAG, "Some cache tests failed - check logs");
                            }
                        });
                } else {
                    Log.e(TAG, "Tide data initialization failed");
                    onTideDataFailed();
                }
            })
            .exceptionally(throwable -> {
                Log.e(TAG, "Tide data initialization error", throwable);
                onTideDataFailed();
                return null;
            });
    }
    
    /**
     * Called when tide data is successfully loaded and ready
     */
    private void onTideDataReady() {
        Log.d(TAG, "Tide data ready - can now enable full functionality");
        // Here we could enable notifications, widgets, etc.
    }
    
    /**
     * Called when tide data loading fails
     */
    private void onTideDataFailed() {
        Log.w(TAG, "Tide data failed to load - app will use fallback file-based loading");
        // App will fall back to reading files directly when needed
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
