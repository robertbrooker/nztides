package com.palliser.nztides;

import android.content.res.AssetManager;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository pattern for tide data management
 * Handles loading, caching, and access to tide data
 * Thread-safe singleton implementation
 */
public class TideRepository {
    private static final String TAG = "TideRepository";
    private static volatile TideRepository instance;
    
    private volatile TideDataCache cache;
    private volatile DataState state = DataState.UNINITIALIZED;
    private final Object stateLock = new Object();
    private final ExecutorService executorService;
    
    public enum DataState {
        UNINITIALIZED, LOADING, READY, ERROR, EXPIRED
    }
    
    private TideRepository() {
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "TideDataLoader");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Gets the singleton instance of TideRepository
     */
    public static TideRepository getInstance() {
        if (instance == null) {
            synchronized (TideRepository.class) {
                if (instance == null) {
                    instance = new TideRepository();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes the tide data cache asynchronously
     * @param assetManager AssetManager to load data from
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Boolean> initializeAsync(AssetManager assetManager) {
        synchronized (stateLock) {
            if (state == DataState.READY) {
                Log.d(TAG, "Tide data already loaded");
                return CompletableFuture.completedFuture(true);
            }
            
            if (state == DataState.LOADING) {
                Log.d(TAG, "Tide data loading already in progress");
                // Return a future that waits for current loading to complete
                return CompletableFuture.supplyAsync(() -> {
                    while (state == DataState.LOADING) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                    return state == DataState.READY;
                }, executorService);
            }
            
            state = DataState.LOADING;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Log.i(TAG, "Starting tide data initialization");
                long startTime = System.currentTimeMillis();
                
                TideDataCache newCache = TideDataLoader.loadFromAssets(assetManager);
                
                if (newCache != null) {
                    synchronized (stateLock) {
                        cache = newCache;
                        state = DataState.READY;
                    }
                    
                    long loadTime = System.currentTimeMillis() - startTime;
                    Log.i(TAG, "Tide data loaded successfully in " + loadTime + "ms. " +
                          "Ports: " + newCache.getAvailablePorts().size() + 
                          ", Records: " + newCache.getTotalRecordCount() +
                          ", Memory: " + (newCache.getEstimatedMemoryUsage() / 1024) + "KB");
                    return true;
                } else {
                    synchronized (stateLock) {
                        state = DataState.ERROR;
                    }
                    Log.e(TAG, "Failed to load tide data");
                    return false;
                }
                
            } catch (Exception e) {
                synchronized (stateLock) {
                    state = DataState.ERROR;
                }
                Log.e(TAG, "Error during tide data initialization", e);
                return false;
            }
        }, executorService);
    }
    
    /**
     * Gets the current tide data cache
     * @return TideDataCache if available, null otherwise
     */
    public TideDataCache getCache() {
        if (state != DataState.READY) {
            Log.w(TAG, "Tide data cache not ready. Current state: " + state);
            return null;
        }
        return cache;
    }
    
    /**
     * Gets the current state of the data loading
     */
    public DataState getState() {
        return state;
    }
    
    /**
     * Checks if the cache is ready for use
     */
    public boolean isReady() {
        return state == DataState.READY && cache != null;
    }
    
    /**
     * Checks if data is available for the given timestamp
     */
    public boolean isValidAt(long timestamp) {
        TideDataCache currentCache = cache;
        return currentCache != null && currentCache.isValidAt(timestamp);
    }
    
    /**
     * Forces a reload of tide data
     * @param assetManager AssetManager to load data from
     * @return CompletableFuture that completes when reloading is done
     */
    public CompletableFuture<Boolean> reload(AssetManager assetManager) {
        synchronized (stateLock) {
            state = DataState.UNINITIALIZED;
            cache = null;
        }
        return initializeAsync(assetManager);
    }
    
    /**
     * Clears the cache and releases resources
     */
    public void clear() {
        synchronized (stateLock) {
            cache = null;
            state = DataState.UNINITIALIZED;
        }
        Log.d(TAG, "Tide data cache cleared");
    }
    
    /**
     * Shuts down the repository and releases resources
     */
    public void shutdown() {
        clear();
        executorService.shutdown();
        Log.d(TAG, "TideRepository shut down");
    }
}
