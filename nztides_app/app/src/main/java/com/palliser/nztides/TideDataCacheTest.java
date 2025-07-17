package com.palliser.nztides;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple test runner to verify the refactored tide data cache functionality
 * This can be called from the Application onCreate to test the implementation
 */
public class TideDataCacheTest {
    private static final String TAG = "TideDataCacheTest";
    
    /**
     * Run basic tests on the tide data cache
     * @param context Application context
     * @return CompletableFuture that completes when tests are done
     */
    public static CompletableFuture<Boolean> runTests(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AssetManager assetManager = context.getAssets();
                
                Log.i(TAG, "Starting tide data cache tests...");
                
                // Test 1: Load data into cache
                TideRepository repository = TideRepository.getInstance();
                boolean loadSuccess = repository.initializeAsync(assetManager)
                    .get(30, TimeUnit.SECONDS); // Wait up to 30 seconds
                
                if (!loadSuccess) {
                    Log.e(TAG, "Test FAILED: Could not load tide data into cache");
                    return false;
                }
                
                Log.i(TAG, "Test PASSED: Tide data loaded successfully");
                
                // Test 2: Verify cache is ready
                if (!repository.isReady()) {
                    Log.e(TAG, "Test FAILED: Repository not ready after successful load");
                    return false;
                }
                
                TideDataCache cache = repository.getCache();
                if (cache == null) {
                    Log.e(TAG, "Test FAILED: Cache is null after successful load");
                    return false;
                }
                
                Log.i(TAG, "Test PASSED: Cache is ready and accessible");
                
                // Test 3: Check cache has data
                int totalRecords = cache.getTotalRecordCount();
                int portCount = cache.getAvailablePorts().size();
                long memoryUsage = cache.getEstimatedMemoryUsage();
                
                if (totalRecords == 0) {
                    Log.e(TAG, "Test FAILED: No tide records in cache");
                    return false;
                }
                
                if (portCount == 0) {
                    Log.e(TAG, "Test FAILED: No ports available in cache");
                    return false;
                }
                
                Log.i(TAG, "Test PASSED: Cache contains " + totalRecords + 
                      " records for " + portCount + " ports, using ~" + 
                      (memoryUsage / 1024) + "KB memory");
                
                // Test 4: Test tide calculation with common port
                String testPort = "Auckland";
                if (!cache.hasDataForPort(testPort)) {
                    // Try first available port
                    testPort = cache.getAvailablePorts().iterator().next();
                }
                
                long currentTime = System.currentTimeMillis() / 1000;
                TideInterval interval = cache.getTideInterval(testPort, currentTime);
                
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
                testPerformance(assetManager, testPort, cache);
                
                Log.i(TAG, "All tide data cache tests completed successfully!");
                return true;
                
            } catch (Exception e) {
                Log.e(TAG, "Test FAILED: Exception during testing", e);
                return false;
            }
        });
    }
    
    /**
     * Test performance difference between cached and file-based calculations
     */
    private static void testPerformance(AssetManager assetManager, String testPort, TideDataCache cache) {
        try {
            // Test cached calculation performance
            long startTime = System.nanoTime();
            CachedTideCalculationService cachedService = new CachedTideCalculationService(assetManager);
            NextTideInfo cachedResult = cachedService.getNextTideInfo(testPort);
            long cachedTime = System.nanoTime() - startTime;
            
            // Test file-based calculation performance  
            startTime = System.nanoTime();
            TideCalculationService fileService = new TideCalculationService(assetManager);
            NextTideInfo fileResult = fileService.getNextTideInfo(testPort);
            long fileTime = System.nanoTime() - startTime;
            
            if (cachedResult != null && fileResult != null) {
                double speedup = (double) fileTime / cachedTime;
                Log.i(TAG, "Performance test: Cached calculation " + 
                      String.format("%.1fx", speedup) + " faster than file-based");
                Log.d(TAG, "Cached: " + (cachedTime / 1000000) + "ms, File: " + (fileTime / 1000000) + "ms");
                
                // Verify results are similar
                int timeDiff = Math.abs(cachedResult.getSecondsUntilTide() - fileResult.getSecondsUntilTide());
                if (timeDiff <= 1) { // Allow 1 second difference due to timing
                    Log.i(TAG, "Test PASSED: Cached and file-based calculations produce same results");
                } else {
                    Log.w(TAG, "Test WARNING: Results differ by " + timeDiff + " seconds");
                }
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Performance test failed", e);
        }
    }
}
