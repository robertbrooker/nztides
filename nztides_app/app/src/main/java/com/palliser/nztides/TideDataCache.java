package com.palliser.nztides;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable in-memory tide data cache
 * Provides fast O(log n) lookups for tide predictions
 * Optimized for memory efficiency and thread safety
 */
public final class TideDataCache {
    private final Map<String, TideRecord[]> portTideData;
    private final long dataValidUntil;
    private final Set<String> availablePorts;
    
    public TideDataCache(Map<String, List<TideRecord>> portData) {
        // Convert lists to sorted arrays for fast binary search
        Map<String, TideRecord[]> arrayMap = new java.util.HashMap<>();
        long maxTimestamp = 0;
        
        for (Map.Entry<String, List<TideRecord>> entry : portData.entrySet()) {
            List<TideRecord> tides = entry.getValue();
            TideRecord[] sortedArray = tides.toArray(new TideRecord[0]);
            
            // Ensure array is sorted by timestamp
            Arrays.sort(sortedArray, (a, b) -> Long.compare(a.timestamp, b.timestamp));
            arrayMap.put(entry.getKey(), sortedArray);
            
            // Track latest timestamp
            if (sortedArray.length > 0) {
                maxTimestamp = Math.max(maxTimestamp, sortedArray[sortedArray.length - 1].timestamp);
            }
        }
        
        this.portTideData = Collections.unmodifiableMap(arrayMap);
        this.dataValidUntil = maxTimestamp;
        this.availablePorts = Collections.unmodifiableSet(portData.keySet());
    }
    
    /**
     * Gets the tide interval surrounding the given timestamp for a port
     * Uses binary search for O(log n) performance
     * 
     * @param port The port name
     * @param timestamp The timestamp to search around
     * @return TideInterval containing previous and next tides, or null if not found
     */
    public TideInterval getTideInterval(String port, long timestamp) {
        TideRecord[] tides = portTideData.get(port);
        if (tides == null || tides.length < 2) {
            return null;
        }
        
        // Binary search for insertion point
        int index = Arrays.binarySearch(tides, new TideRecord(timestamp, 0, false), 
            (a, b) -> Long.compare(a.timestamp, b.timestamp));
        
        if (index < 0) {
            // Convert insertion point to actual index
            index = -(index + 1);
        }
        
        // Find previous and next tides
        TideRecord previous = null;
        TideRecord next = null;
        
        if (index > 0) {
            previous = tides[index - 1];
        }
        if (index < tides.length) {
            next = tides[index];
        }
        
        // If timestamp is exactly on a tide, adjust accordingly
        if (index < tides.length && tides[index].timestamp == timestamp) {
            previous = index > 0 ? tides[index - 1] : null;
            next = index < tides.length - 1 ? tides[index + 1] : null;
        }
        
        return new TideInterval(previous, next);
    }
    
    /**
     * Gets all tide records for a port
     * Returns a copy to maintain immutability
     */
    public TideRecord[] getAllTides(String port) {
        TideRecord[] tides = portTideData.get(port);
        return tides != null ? Arrays.copyOf(tides, tides.length) : new TideRecord[0];
    }
    
    /**
     * Gets tides for a port within a specific time range
     */
    public TideRecord[] getTidesInRange(String port, long startTime, long endTime) {
        TideRecord[] allTides = portTideData.get(port);
        if (allTides == null) {
            return new TideRecord[0];
        }
        
        // Find start index
        int startIndex = Arrays.binarySearch(allTides, new TideRecord(startTime, 0, false),
            (a, b) -> Long.compare(a.timestamp, b.timestamp));
        if (startIndex < 0) {
            startIndex = -(startIndex + 1);
        }
        
        // Find end index
        int endIndex = Arrays.binarySearch(allTides, new TideRecord(endTime, 0, false),
            (a, b) -> Long.compare(a.timestamp, b.timestamp));
        if (endIndex < 0) {
            endIndex = -(endIndex + 1);
        }
        
        // Extract range
        if (startIndex >= allTides.length || endIndex <= startIndex) {
            return new TideRecord[0];
        }
        
        return Arrays.copyOfRange(allTides, startIndex, Math.min(endIndex, allTides.length));
    }
    
    /**
     * Checks if data is available for the given port
     */
    public boolean hasDataForPort(String port) {
        return portTideData.containsKey(port);
    }
    
    /**
     * Gets all available port names
     */
    public Set<String> getAvailablePorts() {
        return availablePorts;
    }
    
    /**
     * Gets the timestamp of the latest tide data available
     */
    public long getDataValidUntil() {
        return dataValidUntil;
    }
    
    /**
     * Checks if the cache has valid data for the given timestamp
     */
    public boolean isValidAt(long timestamp) {
        return timestamp <= dataValidUntil;
    }
    
    /**
     * Gets the total number of tide records across all ports
     */
    public int getTotalRecordCount() {
        return portTideData.values().stream()
            .mapToInt(array -> array.length)
            .sum();
    }
    
    /**
     * Gets memory usage estimation in bytes
     */
    public long getEstimatedMemoryUsage() {
        // Rough estimation: each TideRecord is ~20 bytes (8 + 4 + 1 + overhead)
        return getTotalRecordCount() * 20L;
    }
}
