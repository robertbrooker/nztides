package com.palliser.nztides;

import android.content.Context;
import android.util.Log;

import com.palliser.nztides.models.TideData;
import com.palliser.nztides.models.TideRecord;
import com.palliser.nztides.models.TideInterval;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading and enriching tide data with current time validation
 */
public class CurrentTidesService {
    private static final String TAG = "CurrentTidesService";
    private static final long THIRTY_DAYS_SECONDS = 30 * 24 * 3600;
    
    private final Context context;
    private final TideService tideService;
    
    public CurrentTidesService(Context context) {
        this.context = context;
        this.tideService = TideService.getInstance();
    }
    
    /**
     * Loads complete tide data for a port at the current time
     */
    public TideData loadTideData(String port, long currentTimeSeconds) throws TideDataException {
        try {
            // Load raw tide data from assets
            String filename = port + ".tdat";
            List<TideRecord> allTides;
            try (InputStream inputStream = context.getAssets().open(filename, 1)) {
                allTides = tideService.loadPortData(inputStream);
            }
            
            return buildTideData(port, allTides, currentTimeSeconds);
                              
        } catch (Exception e) {
            if (e instanceof TideDataException) {
                throw (TideDataException) e;
            }
            Log.e(TAG, "Error loading tide data for " + port, e);
            throw new TideDataException("Error loading tide data for " + port + ": " + e.getMessage());
        }
    }

    /**
     * Loads complete tide data for a port at the current time from provided InputStream
     * This overload is useful for testing with mock data
     */
    public TideData loadTideData(String port, InputStream inputStream, long currentTimeSeconds) throws TideDataException {
        try {
            // Load raw tide data from provided stream
            List<TideRecord> allTides = tideService.loadPortData(inputStream);
            
            return buildTideData(port, allTides, currentTimeSeconds);
                              
        } catch (Exception e) {
            if (e instanceof TideDataException) {
                throw (TideDataException) e;
            }
            Log.e(TAG, "Error loading tide data for " + port, e);
            throw new TideDataException("Error loading tide data for " + port + ": " + e.getMessage());
        }
    }
    
    /**
     * Refreshes tide data using existing tide records with new current time
     */
    public TideData refreshTideData(String port, List<TideRecord> allTides, long currentTimeSeconds) 
            throws TideDataException {
        return buildTideData(port, allTides, currentTimeSeconds);
    }
    
    private TideData buildTideData(String port, List<TideRecord> allTides, long currentTimeSeconds) 
            throws TideDataException {
        try {
            // Validate we have sufficient data for current time
            validateCurrentTimeData(allTides, currentTimeSeconds, port);
            
            // Validate we have sufficient upcoming data (30 days)
            validateUpcomingData(allTides, currentTimeSeconds, port);
            
            // Get current tide interval with timing information
            TideInterval currentInterval = tideService.getTideInterval(allTides, currentTimeSeconds);
            
            // Calculate current tide
            TideData.TideCalculation currentTideCalc = calculateCurrentTide(currentInterval, currentTimeSeconds);
            
            // Find last tide in data
            TideRecord lastTideInData = findLastTideInData(allTides);
            
            // Get upcoming tides for display
            long endTime = currentTimeSeconds + THIRTY_DAYS_SECONDS;
            TideRecord[] upcomingTides = getUpcomingTides(allTides, currentTimeSeconds, endTime);
            
            return new TideData(port, currentTimeSeconds, allTides, currentInterval, 
                              currentTideCalc, lastTideInData, upcomingTides);
                              
        } catch (Exception e) {
            if (e instanceof TideDataException) {
                throw (TideDataException) e;
            }
            Log.e(TAG, "Error building tide data for " + port, e);
            throw new TideDataException("Error building tide data for " + port + ": " + e.getMessage());
        }
    }
    
    private void validateCurrentTimeData(List<TideRecord> tides, long currentTimeSeconds, String port) 
            throws TideDataException {
        // Find the latest tide timestamp
        long latestTimestamp = 0;
        for (TideRecord tide : tides) {
            if (tide.timestamp > latestTimestamp) {
                latestTimestamp = tide.timestamp;
            }
        }
        
        if (currentTimeSeconds > latestTimestamp) {
            throw new TideDataException("Tide data has expired. Please update the app for current predictions.");
        }
    }
    
    private void validateUpcomingData(List<TideRecord> tides, long currentTimeSeconds, String port) 
            throws TideDataException {
        long thirtyDaysFromNow = currentTimeSeconds + THIRTY_DAYS_SECONDS;
        
        boolean hasDataFor30Days = false;
        for (TideRecord tide : tides) {
            if (tide.timestamp >= thirtyDaysFromNow) {
                hasDataFor30Days = true;
                break;
            }
        }
        
        if (!hasDataFor30Days) {
            throw new TideDataException("Insufficient tide data available for " + port + 
                ". Missing data for next 30 days.");
        }
    }
    
    private TideData.TideCalculation calculateCurrentTide(TideInterval interval, long currentTimeSeconds) 
            throws TideDataException {
        // Cosine interpolation between tides
        double omega = 2 * Math.PI / ((interval.next.timestamp - interval.previous.timestamp) * 2);
        double amplitude = (interval.previous.height - interval.next.height) / 2;
        double mean = (interval.next.height + interval.previous.height) / 2;
        
        double currentHeight = amplitude * Math.cos(omega * (currentTimeSeconds - interval.previous.timestamp)) + mean;
        double riseRate = -amplitude * omega * Math.sin(omega * (currentTimeSeconds - interval.previous.timestamp)) * 3600; // cm/hr
        
        return new TideData.TideCalculation(currentHeight, riseRate);
    }
    
    private TideRecord findLastTideInData(List<TideRecord> tides) {
        TideRecord lastTide = null;
        for (TideRecord tide : tides) {
            if (lastTide == null || tide.timestamp > lastTide.timestamp) {
                lastTide = tide;
            }
        }
        return lastTide;
    }
    
    private TideRecord[] getUpcomingTides(List<TideRecord> tides, long startTime, long endTime) {
        List<TideRecord> result = new ArrayList<>();
        for (TideRecord tide : tides) {
            if (tide.timestamp >= startTime && tide.timestamp <= endTime) {
                result.add(tide);
            }
        }
        
        return result.toArray(new TideRecord[0]);
    }
    
    /**
     * Exception for tide data validation errors
     */
    public static class TideDataException extends Exception {
        public TideDataException(String message) {
            super(message);
        }
    }
}
