package com.palliser.nztides;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository pattern for tide data management
 * Handles lazy loading, caching, and access to tide data
 * Thread-safe singleton implementation with on-demand port loading
 */
public class TideRepository {
    private static final String TAG = "TideRepository";
    private static volatile TideRepository instance;
    
    private final Map<String, TideDataCache> portCaches = new ConcurrentHashMap<>();
    private final Object stateLock = new Object();
    
    public enum DataState {
        UNINITIALIZED, LOADING, READY, ERROR
    }
    
    private TideRepository() {
        // Simple constructor for lazy loading approach
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
     * Manually adds a port cache (for simple lazy loading)
     * @param port The port name
     * @param cache The cache for this port
     */
    public void addPortCache(String port, TideDataCache cache) {
        if (port != null && cache != null) {
            portCaches.put(port, cache);
            Log.d(TAG, "Added cache for port: " + port);
        }
    }
    
    /**
     * Gets the tide data cache for a specific port
     * @param port The port name
     * @return TideDataCache if loaded, null otherwise
     */
    public TideDataCache getCache(String port) {
        return portCaches.get(port);
    }
    
    /**
     * Checks if a port is loaded and ready
     */
    public boolean isPortReady(String port) {
        return portCaches.containsKey(port);
    }
    
    /**
     * Checks if data is available for the given timestamp at a specific port
     */
    public boolean isValidAt(String port, long timestamp) {
        TideDataCache portCache = portCaches.get(port);
        return portCache != null && portCache.isValidAt(timestamp);
    }
    
    /**
     * Gets all currently loaded ports
     */
    public java.util.Set<String> getLoadedPorts() {
        return new java.util.HashSet<>(portCaches.keySet());
    }
    
    /**
     * Clears all cached data for all ports
     */
    public void clear() {
        synchronized (stateLock) {
            portCaches.clear();
        }
        Log.d(TAG, "All tide data caches cleared");
    }
    
    /**
     * Clears cached data for a specific port
     */
    public void clearPort(String port) {
        portCaches.remove(port);
        Log.d(TAG, "Tide data cache cleared for port: " + port);
    }
    
    /**
     * Shuts down the repository and releases resources
     */
    public void shutdown() {
        clear();
        Log.d(TAG, "TideRepository shut down");
    }
}
