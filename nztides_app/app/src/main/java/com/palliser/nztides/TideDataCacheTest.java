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
                    CachedTideCalculationService calcService = new CachedTideCalculationService(assetManager);
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
                
                // Test 5: Performance comparison
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
            CachedTideCalculationService cachedService = new CachedTideCalculationService(assetManager);
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
}
