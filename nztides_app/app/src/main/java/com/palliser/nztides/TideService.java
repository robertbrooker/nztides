package com.palliser.nztides;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tide service that loads data on-demand for each port
 * Loads fresh data from assets every time a port is requested
 */
public class TideService {
    private static final String TAG = "TideService";
    private static volatile TideService instance;
    
    private TideService() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance
     */
    public static TideService getInstance() {
        if (instance == null) {
            synchronized (TideService.class) {
                if (instance == null) {
                    instance = new TideService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Loads tide data for a specific port from a .tdat file
     * @param assetManager AssetManager to read from
     * @param portName Name of the port (without .tdat extension)
     * @return List of TideRecord objects, or null if loading fails
     */
    public List<TideRecord> loadPortData(AssetManager assetManager, String portName) {
        String filename = portName + ".tdat";
        List<TideRecord> tideRecords = new ArrayList<>();
        
        try (DataInputStream stream = new DataInputStream(assetManager.open(filename, 1))) {
            // Read and skip station name
            stream.readLine();
            
            // Read timestamp for last tide in datafile
            int lastTideTimestamp = swapBytes(stream.readInt());
            
            // Read number of records
            int numRecords = swapBytes(stream.readInt());
            
            Log.d(TAG, "Loading " + numRecords + " records for " + filename);
            
            if (numRecords < 2) {
                Log.w(TAG, "Insufficient tide records in file: " + filename);
                return null;
            }
            
            // Pre-read first two tides to establish pattern
            int firstTideTime = swapBytes(stream.readInt());
            float firstTideHeight = (float) (stream.readByte()) / 10.0f;
            
            int secondTideTime = swapBytes(stream.readInt());
            float secondTideHeight = (float) (stream.readByte()) / 10.0f;
            
            // Determine if first tide is high or low based on second tide
            boolean firstIsHigh = firstTideHeight > secondTideHeight;
            
            // Create first tide record
            TideRecord firstTide = new TideRecord(firstTideTime, firstTideHeight, firstIsHigh);
            tideRecords.add(firstTide);
            
            // Read the rest of the tides, alternating high/low status
            boolean currentTideIsHigh = !firstIsHigh; // Second tide is opposite of first
            TideRecord secondTide = new TideRecord(secondTideTime, secondTideHeight, currentTideIsHigh);
            tideRecords.add(secondTide);
            
            // Read remaining tides (already read 2)
            for (int i = 2; i < numRecords; i++) {
                try {
                    int tideTime = swapBytes(stream.readInt());
                    float tideHeight = (float) (stream.readByte()) / 10.0f;
                    
                    // Alternate tide status
                    currentTideIsHigh = !currentTideIsHigh;
                    
                    TideRecord record = new TideRecord(tideTime, tideHeight, currentTideIsHigh);
                    tideRecords.add(record);
                    
                } catch (IOException e) {
                    Log.w(TAG, "Error reading tide record " + i + " from " + filename, e);
                    break;
                }
            }
            
            // Validate that we read all expected records
            if (tideRecords.size() != numRecords) {
                Log.w(TAG, "Expected " + numRecords + " records but loaded " + 
                      tideRecords.size() + " for " + filename);
            }
            
            Log.i(TAG, "Successfully loaded " + tideRecords.size() + " tide records for " + portName);
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading tide data file: " + filename, e);
            return null;
        }
        
        return tideRecords;
    }
    
    /**
     * Gets the tide interval surrounding the given timestamp for a port
     * @param tides List of tide records (must be sorted by timestamp)
     * @param timestamp The timestamp to search around
     * @return TideInterval containing previous and next tides, or null if not found
     */
    public TideInterval getTideInterval(List<TideRecord> tides, long timestamp) {
        if (tides == null || tides.size() < 2) {
            return null;
        }
        
        // Convert to array for binary search
        TideRecord[] tidesArray = tides.toArray(new TideRecord[0]);
        
        // Binary search for insertion point
        int index = Arrays.binarySearch(tidesArray, new TideRecord(timestamp, 0, false), 
            (a, b) -> Long.compare(a.timestamp, b.timestamp));
        
        if (index < 0) {
            // Convert insertion point to actual index
            index = -(index + 1);
        }
        
        // Find previous and next tides
        TideRecord previous = null;
        TideRecord next = null;
        
        if (index > 0) {
            previous = tidesArray[index - 1];
        }
        if (index < tidesArray.length) {
            next = tidesArray[index];
        }
        
        // If timestamp is exactly on a tide, adjust accordingly
        if (index < tidesArray.length && tidesArray[index].timestamp == timestamp) {
            previous = index > 0 ? tidesArray[index - 1] : null;
            next = index < tidesArray.length - 1 ? tidesArray[index + 1] : null;
        }
        
        return new TideInterval(previous, next);
    }
    
    /**
     * Gets the next tide information for a port at the given time
     * @param tides List of tide records (must be sorted by timestamp)
     * @param currentTime The current timestamp in seconds
     * @return NextTideInfo or null if not available
     */
    public NextTideInfo getNextTideInfo(List<TideRecord> tides, long currentTime) {
        try {
            // Get the current tide interval
            TideInterval interval = getTideInterval(tides, currentTime);
            if (interval == null || !interval.isValid()) {
                return null;
            }
            
            // The next tide is always interval.next in a valid interval
            TideRecord nextTide = interval.next;
            if (nextTide == null || nextTide.timestamp <= currentTime) {
                // If interval.next is not in the future, look for the next tide after this interval
                for (TideRecord tide : tides) {
                    if (tide.timestamp > currentTime) {
                        nextTide = tide;
                        break;
                    }
                }
                if (nextTide == null) {
                    return null;
                }
            }
            
            int secondsUntilNextTide = (int) (nextTide.timestamp - currentTime);
            
            return new NextTideInfo(nextTide, secondsUntilNextTide);
                                  
        } catch (Exception e) {
            Log.e(TAG, "Error calculating next tide info", e);
            return null;
        }
    }
    
    /**
     * Calculates current tide height and rate using cosine interpolation
     * @param tides List of tide records (must be sorted by timestamp)
     * @param currentTimeSeconds Current time in seconds
     * @return TideCalculation result or null if not available
     */
    public TideCalculation calculateCurrentTide(List<TideRecord> tides, long currentTimeSeconds) {
        try {
            TideInterval interval = getTideInterval(tides, currentTimeSeconds);
            if (interval == null || !interval.isValid()) {
                return null;
            }
            
            // Cosine interpolation between tides
            double omega = 2 * Math.PI / ((interval.next.timestamp - interval.previous.timestamp) * 2);
            double amplitude = (interval.previous.height - interval.next.height) / 2;
            double mean = (interval.next.height + interval.previous.height) / 2;
            
            double currentHeight = amplitude * Math.cos(omega * (currentTimeSeconds - interval.previous.timestamp)) + mean;
            double riseRate = -amplitude * omega * Math.sin(omega * (currentTimeSeconds - interval.previous.timestamp)) * 3600; // cm/hr
            
            return new TideCalculation(currentHeight, riseRate);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating current tide", e);
            return null;
        }
    }
    
    /**
     * Checks if tide data is valid for the given timestamp
     * @param tides List of tide records
     * @param timestamp The timestamp to check
     * @return true if valid, false otherwise
     */
    public boolean isValidAt(List<TideRecord> tides, long timestamp) {
        if (tides == null || tides.isEmpty()) {
            return false;
        }
        
        // Find the latest tide timestamp
        long latestTimestamp = 0;
        for (TideRecord tide : tides) {
            if (tide.timestamp > latestTimestamp) {
                latestTimestamp = tide.timestamp;
            }
        }
        
        return timestamp <= latestTimestamp;
    }
    
    /**
     * Gets tides for a port within a specific time range
     * @param tides List of tide records (must be sorted by timestamp)
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Array of TideRecord objects within the range
     */
    public TideRecord[] getTidesInRange(List<TideRecord> tides, long startTime, long endTime) {
        if (tides == null) {
            return new TideRecord[0];
        }
        
        List<TideRecord> result = new ArrayList<>();
        for (TideRecord tide : tides) {
            if (tide.timestamp >= startTime && tide.timestamp <= endTime) {
                result.add(tide);
            }
        }
        
        return result.toArray(new TideRecord[0]);
    }
    
    /**
     * Swaps byte order for endianness conversion
     */
    private static int swapBytes(int value) {
        int b1 = (value) & 0xff;
        int b2 = (value >>  8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;
        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }
    
    /**
     * Inner class to hold tide calculation results
     */
    public static class TideCalculation {
        public final double height;
        public final double riseRate;
        
        public TideCalculation(double height, double riseRate) {
            this.height = height;
            this.riseRate = riseRate;
        }
    }
}
