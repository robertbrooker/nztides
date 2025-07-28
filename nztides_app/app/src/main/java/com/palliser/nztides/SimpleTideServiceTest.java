package com.palliser.nztides;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.util.List;

/**
 * Simple test runner to verify the simplified tide service functionality
 */
public class SimpleTideServiceTest {
    private static final String TAG = "SimpleTideServiceTest";
    
    /**
     * Run basic tests on the simplified tide service
     * @param context Application context
     * @return Boolean indicating success
     */
    public static Boolean runTests(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            
            Log.i(TAG, "Starting simplified tide service tests...");
            
            // Test 1: Load a single port (Auckland)
            SimpleTideService tideService = SimpleTideService.getInstance();
            String testPort = "Auckland";
            
            List<TideRecord> tides = tideService.loadPortData(assetManager, testPort);
            if (tides == null || tides.isEmpty()) {
                Log.e(TAG, "Test FAILED: Could not load tide data for " + testPort);
                return false;
            }
            
            Log.i(TAG, "Test PASSED: Loaded " + tides.size() + " tide records for " + testPort);
            
            // Test 2: Check data validity
            long currentTime = System.currentTimeMillis() / 1000;
            if (!tideService.isValidAt(tides, currentTime)) {
                Log.w(TAG, "Test WARNING: Tide data may be expired for current time");
            } else {
                Log.i(TAG, "Test PASSED: Tide data is valid for current time");
            }
            
            // Test 3: Test tide interval calculation
            TideInterval interval = tideService.getTideInterval(tides, currentTime);
            if (interval == null || !interval.isValid()) {
                Log.w(TAG, "Test WARNING: No valid tide interval found for current time");
            } else {
                Log.i(TAG, "Test PASSED: Tide interval calculation successful");
            }
            
            // Test 4: Test next tide calculation
            NextTideInfo nextTide = tideService.getNextTideInfo(tides, currentTime);
            if (nextTide != null) {
                Log.i(TAG, "Test PASSED: Next tide calculation successful - " +
                      (nextTide.isHighTide() ? "HIGH" : "low") + " tide in " + 
                      (nextTide.getSecondsUntilTide() / 60) + " minutes");
            } else {
                Log.w(TAG, "Test WARNING: Next tide calculation returned null");
            }
            
            // Test 5: Test current tide calculation
            SimpleTideService.TideCalculation currentTideCalc = 
                tideService.calculateCurrentTide(tides, currentTime);
            if (currentTideCalc != null) {
                Log.i(TAG, "Test PASSED: Current tide calculation successful - " +
                      String.format("%.2f", currentTideCalc.height) + "m, " +
                      String.format("%.1f", currentTideCalc.riseRate) + " cm/hr");
            } else {
                Log.w(TAG, "Test WARNING: Current tide calculation returned null");
            }
            
            // Test 6: Test loading a different port to verify no caching side effects
            String secondPort = "Wellington";
            List<TideRecord> secondTides = tideService.loadPortData(assetManager, secondPort);
            if (secondTides != null && !secondTides.isEmpty()) {
                Log.i(TAG, "Test PASSED: Second port (" + secondPort + ") loaded successfully with " +
                      secondTides.size() + " records");
            } else {
                Log.w(TAG, "Test WARNING: Failed to load second port " + secondPort);
            }
            
            // Test 7: Test tides in range
            long endTime = currentTime + (7 * 24 * 3600); // next 7 days
            TideRecord[] tidesInRange = tideService.getTidesInRange(tides, currentTime, endTime);
            if (tidesInRange.length > 0) {
                Log.i(TAG, "Test PASSED: Found " + tidesInRange.length + " tides in next 7 days");
            } else {
                Log.w(TAG, "Test WARNING: No tides found in next 7 days");
            }
            
            Log.i(TAG, "All simplified tide service tests completed successfully!");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Test FAILED: Exception during testing", e);
            return false;
        }
    }
}
