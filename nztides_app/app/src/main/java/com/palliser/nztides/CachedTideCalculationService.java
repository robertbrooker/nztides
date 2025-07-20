package com.palliser.nztides;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Enhanced tide calculation service using cached data when available
 * Falls back to file-based calculations if cache is not ready
 * Thread-safe and optimized for performance
 */
public class CachedTideCalculationService {
    private static final String TAG = "CachedTideCalcService";
    private final AssetManager assetManager;
    private final TideRepository repository;
    
    public CachedTideCalculationService(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.repository = TideRepository.getInstance();
    }
    
    /**
     * Calculates the next tide information for a given port
     * Uses lazy loading to get port data on demand
     * 
     * @param port The port name to calculate tides for
     * @return NextTideInfo object or null if calculation fails
     */
    public NextTideInfo getNextTideInfo(String port) {
        if (port == null || port.trim().isEmpty()) {
            Log.w(TAG, "Port name is null or empty");
            return null;
        }
        
        // Use lazy loading - check if already loaded
        if (!repository.isPortReady(port)) {
            Log.d(TAG, "Port " + port + " not ready, will need to load first");
            // Port not loaded yet - return null for now
            // The caller should trigger loading and try again later
            return null;
        }
        
        // Use cached calculation
        NextTideInfo result = getNextTideFromCache(port);
        if (result != null) {
            return result;
        }
        
        Log.w(TAG, "Cache calculation failed for port: " + port + ", trying file fallback");
        return getNextTideFromFile(port);
    }
    
    /**
     * Gets next tide info using cached data
     */
    private NextTideInfo getNextTideFromCache(String port) {
        try {
            TideDataCache cache = repository.getCache(port);
            if (cache == null) {
                return null;
            }
            
            long currentTime = System.currentTimeMillis() / 1000;
            
            // Check if we have valid data for current time
            if (!cache.isValidAt(currentTime)) {
                Log.w(TAG, "Cache data expired for current time");
                return null;
            }
            
            TideInterval interval = cache.getTideInterval(port, currentTime);
            if (interval == null || !interval.isValid()) {
                Log.w(TAG, "No valid tide interval found for port: " + port);
                return null;
            }
            
            // Determine which tide is next
            TideRecord nextTide;
            if (interval.next != null && interval.next.timestamp > currentTime) {
                nextTide = interval.next;
            } else {
                // Look for the tide after the current interval
                TideRecord[] tidesAfter = cache.getTidesInRange(port, currentTime, currentTime + 86400); // next 24 hours
                if (tidesAfter.length > 0) {
                    nextTide = tidesAfter[0];
                } else {
                    Log.w(TAG, "No future tides found for port: " + port);
                    return null;
                }
            }
            
            int secondsUntilNextTide = (int) (nextTide.timestamp - currentTime);
            
            return new NextTideInfo(nextTide.isHighTide, (int) nextTide.timestamp, 
                                  nextTide.height, secondsUntilNextTide);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating tide from cache for port: " + port, e);
            return null;
        }
    }
    
    /**
     * Gets next tide info using file-based calculation (fallback)
     * This is the original implementation for compatibility
     */
    private NextTideInfo getNextTideFromFile(String port) {
        try (DataInputStream tideDataStream = new DataInputStream(assetManager.open(port + ".tdat", 1))) {
            // Skip station name
            tideDataStream.readLine();
            
            // Read timestamp for last tide in datafile
            int lastTideInFile = TideDataReader.swapBytes(tideDataStream.readInt());
            
            // Skip number of records in datafile
            tideDataStream.readInt();
            
            Date currentTime = new Date();
            int currentTimeSeconds = (int) (currentTime.getTime() / 1000);
            
            // Check if we're past the last tide in the file
            if (currentTimeSeconds > lastTideInFile) {
                Log.w(TAG, "Current time is past the last tide in file for port: " + port);
                return null;
            }
            
            int previousTideTime = TideDataReader.swapBytes(tideDataStream.readInt());
            float previousTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
            
            // If first tide is in the future, return it
            if (previousTideTime > currentTimeSeconds) {
                // Read the next tide to determine tide type
                tideDataStream.readInt();
                float nextTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
                
                boolean isHighTide = previousTideHeight > nextTideHeight;
                
                return new NextTideInfo(isHighTide, previousTideTime, previousTideHeight, 
                        previousTideTime - currentTimeSeconds);
            }
            
            // Look through tide data file for current time
            int nextTideTime;
            float nextTideHeight;
            
            while (true) {
                nextTideTime = TideDataReader.swapBytes(tideDataStream.readInt());
                nextTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
                
                if (nextTideTime > currentTimeSeconds) {
                    break;
                }

                previousTideHeight = nextTideHeight;
            }
            
            // Determine if next tide is high or low
            boolean isNextTideHigh = nextTideHeight > previousTideHeight;
            
            int secondsUntilNextTide = nextTideTime - currentTimeSeconds;
            
            return new NextTideInfo(isNextTideHigh, nextTideTime, nextTideHeight, secondsUntilNextTide);
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading tide data file for port: " + port, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error calculating tide from file for port: " + port, e);
            return null;
        }
    }
    
    /**
     * Calculates current tide height and rate using lazy loading
     */
    public TideCalculation calculateCurrentTide(String port, long currentTimeSeconds) {
        // Use lazy loading - check if already loaded
        if (!repository.isPortReady(port)) {
            Log.d(TAG, "Port " + port + " not ready, will need to load first");
            // Port not loaded yet - return null for now
            // The caller should trigger loading and try again later
            return null;
        }
        
        TideCalculation result = calculateCurrentTideFromCache(port, currentTimeSeconds);
        if (result != null) {
            return result;
        }
        
        Log.w(TAG, "Current tide calculation not available for port: " + port);
        return null;
    }
    
    /**
     * Calculates current tide using cached data
     */
    private TideCalculation calculateCurrentTideFromCache(String port, long currentTimeSeconds) {
        try {
            TideDataCache cache = repository.getCache(port);
            if (cache == null) {
                return null;
            }
            
            TideInterval interval = cache.getTideInterval(port, currentTimeSeconds);
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
            Log.e(TAG, "Error calculating current tide from cache for port: " + port, e);
            return null;
        }
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
