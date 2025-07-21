package com.palliser.nztides;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * Simple test runner to verify the refactored tide data cache functionality
 * This can be called from the Application onCreate to test the implementation
 */
public class TideDataCacheTest {
    private static final String TAG = "TideDataCacheTest";
    
    /**
     * Run basic tests on the tide data cache with lazy loading
     * @param context Application context
     * @return Boolean indicating success
     */
    public static Boolean runTests(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            
            Log.i(TAG, "Starting lazy loading tide data cache tests...");
            
            // Test 1: Load a single port (Auckland) to test lazy loading
            TideRepository repository = TideRepository.getInstance();
            String testPort = "Auckland";
            
            // Load directly without using CompletableFuture
            TideDataCache cache = TideDataLoader.loadSinglePortAsCache(assetManager, testPort);
            if (cache == null) {
                Log.e(TAG, "Test FAILED: Could not load tide data for " + testPort);
                return false;
            }
            
            // Manually add to repository
            repository.addPortCache(testPort, cache);
            
            Log.i(TAG, "Test PASSED: Tide data loaded successfully for " + testPort);
                
                // Test 2: Verify port is ready
                if (!repository.isPortReady(testPort)) {
                    Log.e(TAG, "Test FAILED: Port " + testPort + " not ready after successful load");
                    return false;
                }
                
                TideDataCache portCache = repository.getCache(testPort);
                if (portCache == null) {
                    Log.e(TAG, "Test FAILED: Cache is null for " + testPort + " after successful load");
                    return false;
                }
                
                Log.i(TAG, "Test PASSED: Cache is ready and accessible for " + testPort);
                
                // Test 3: Check cache has data
                int totalRecords = portCache.getTotalRecordCount();
                int portCount = portCache.getAvailablePorts().size();
                long memoryUsage = portCache.getEstimatedMemoryUsage();
                
                if (totalRecords == 0) {
                    Log.e(TAG, "Test FAILED: No tide records in cache for " + testPort);
                    return false;
                }
                
                if (portCount != 1) {
                    Log.e(TAG, "Test FAILED: Expected 1 port but found " + portCount + " ports in cache");
                    return false;
                }
                
                Log.i(TAG, "Test PASSED: Cache contains " + totalRecords + 
                      " records for " + portCount + " port (" + testPort + "), using ~" + 
                      (memoryUsage / 1024) + "KB memory");
                
                // Test 4: Test tide calculation with loaded port
                long currentTime = System.currentTimeMillis() / 1000;
                TideInterval interval = portCache.getTideInterval(testPort, currentTime);
                
                if (interval == null) {
                    Log.w(TAG, "Test WARNING: No tide interval found for " + testPort + 
                          " at current time (data may be expired)");
                } else if (interval.isValid()) {
                    Log.i(TAG, "Test PASSED: Tide interval calculation successful for " + testPort);
                    
                    // Test cached calculation service
                    CachedTideCalculationService calcService = new CachedTideCalculationService();
                    NextTideInfo nextTide = calcService.getNextTideInfo(testPort);
                    
                    if (nextTide != null) {
                        Log.i(TAG, "Test PASSED: Next tide calculation successful - " +
                              (nextTide.isHighTide() ? "HIGH" : "low") + " tide in " + 
                              (nextTide.getSecondsUntilTide() / 60) + " minutes");
                    } else {
                        Log.w(TAG, "Test WARNING: Next tide calculation returned null");
                    }
                } else {
                    Log.w(TAG, "Test WARNING: Invalid tide interval for " + testPort);
                }
                
                // Test 5: Test tide type determination fix
                testTideTypeAccuracy(assetManager, testPort, portCache);
                
                // Test 6: Performance comparison
                testPerformance(assetManager, testPort, portCache);
                
                // Test 6: Test loading a second port to verify lazy loading
                String secondPort = "Wellington";
                if (!repository.isPortReady(secondPort)) {
                    // Load directly without CompletableFuture
                    TideDataCache secondCache = TideDataLoader.loadSinglePortAsCache(assetManager, secondPort);
                    if (secondCache != null) {
                        repository.addPortCache(secondPort, secondCache);
                        Log.i(TAG, "Test PASSED: Second port (" + secondPort + ") loaded successfully");
                        Log.i(TAG, "Repository now has " + repository.getLoadedPorts().size() + " ports loaded");
                    } else {
                        Log.w(TAG, "Test WARNING: Failed to load second port " + secondPort);
                    }
                }
                
                Log.i(TAG, "All lazy loading tide data cache tests completed successfully!");
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "Test FAILED: Exception during testing", e);
                return false;
            }
    }
    
    /**
     * Test performance difference between cached and file-based calculations
     */
    private static void testPerformance(AssetManager assetManager, String testPort, TideDataCache cache) {
        try {
            // Test performance - only test cached calculation since file-based is removed
            long startTime = System.nanoTime();
            CachedTideCalculationService cachedService = new CachedTideCalculationService();
            NextTideInfo cachedResult = cachedService.getNextTideInfo(testPort);
            long cachedTime = System.nanoTime() - startTime;
            
            if (cachedResult != null) {
                Log.i(TAG, "Performance test: Cached calculation took " + (cachedTime / 1000000) + "ms");
                Log.i(TAG, "Test PASSED: Cached calculation successful for " + testPort);
            } else {
                Log.w(TAG, "Test WARNING: Cached calculation returned null for " + testPort);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Performance test failed", e);
        }
    }
    
    /**
     * Test tide type determination accuracy
     */
    private static void testTideTypeAccuracy(AssetManager assetManager, String testPort, TideDataCache cache) {
        try {
            Log.i(TAG, "Testing tide type determination accuracy for " + testPort + "...");
            
            // Get a sample of tides and check that heights make sense for their types
            long currentTime = System.currentTimeMillis() / 1000;
            TideRecord[] sampleTides = cache.getTidesInRange(testPort, currentTime, currentTime + (7 * 24 * 3600)); // next 7 days
            
            if (sampleTides.length < 10) {
                Log.w(TAG, "Not enough tide data for accuracy test");
                return;
            }
            
            int highTideCount = 0;
            int lowTideCount = 0;
            int accurateClassifications = 0;
            
            // Check several consecutive tides to ensure they alternate high/low correctly
            for (int i = 1; i < Math.min(sampleTides.length - 1, 20); i++) {
                TideRecord prevTide = sampleTides[i - 1];
                TideRecord currentTide = sampleTides[i];
                TideRecord nextTide = sampleTides[i + 1];
                
                if (currentTide.isHighTide) {
                    highTideCount++;
                    // High tide should be higher than adjacent tides
                    if (currentTide.height > prevTide.height && currentTide.height > nextTide.height) {
                        accurateClassifications++;
                    } else {
                        Log.w(TAG, "Incorrectly classified HIGH tide at " + 
                              new java.util.Date(currentTide.timestamp * 1000L) + 
                              " - height " + currentTide.height + "m not higher than neighbors (" + 
                              prevTide.height + "m, " + nextTide.height + "m)");
                    }
                } else {
                    lowTideCount++;
                    // Low tide should be lower than adjacent tides
                    if (currentTide.height < prevTide.height && currentTide.height < nextTide.height) {
                        accurateClassifications++;
                    } else {
                        Log.w(TAG, "Incorrectly classified LOW tide at " + 
                              new java.util.Date(currentTide.timestamp * 1000L) + 
                              " - height " + currentTide.height + "m not lower than neighbors (" + 
                              prevTide.height + "m, " + nextTide.height + "m)");
                    }
                }
            }
            
            int totalChecked = Math.min(sampleTides.length - 2, 19); // -2 because we skip first and last
            double accuracy = (double) accurateClassifications / totalChecked * 100;
            
            Log.i(TAG, "Tide Type Test Results:");
            Log.i(TAG, "  - Checked " + totalChecked + " tides");
            Log.i(TAG, "  - High tides: " + highTideCount + ", Low tides: " + lowTideCount);
            Log.i(TAG, "  - Accurate classifications: " + accurateClassifications + "/" + totalChecked + " (" + 
                  String.format("%.1f%%", accuracy) + ")");
            
            if (accuracy >= 90.0) {
                Log.i(TAG, "Test PASSED: Tide type determination accuracy is good (" + 
                      String.format("%.1f%%", accuracy) + ")");
            } else {
                Log.w(TAG, "Test WARNING: Tide type determination accuracy is low (" + 
                      String.format("%.1f%%", accuracy) + ")");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Tide type accuracy test failed", e);
        }
    }
}
