package com.palliser.nztides;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads tide data from asset files into memory cache
 * Handles parsing of .tdat files and construction of TideDataCache
 */
public class TideDataLoader {
    private static final String TAG = "TideDataLoader";

    /**
     * Swaps byte order for endianness conversion
     * Centralized implementation to avoid duplication
     */
    public static int swapBytes(int value) {
        int b1 = (value) & 0xff;
        int b2 = (value >>  8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;
        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }

    /**
     * Loads tide data for a specific port from a .tdat file
     * @param assetManager AssetManager to read from
     * @param filename Name of the .tdat file
     * @return List of TideRecord objects, or null if loading fails
     */
    private static List<TideRecord> loadPortData(AssetManager assetManager, String filename) {
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
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading tide data file: " + filename, e);
            return null;
        }
        
        return tideRecords;
    }
    
    /**
     * Loads tide data for a single port on-demand (fallback method)
     * @param assetManager AssetManager to read from
     * @param portName Name of the port (without .tdat extension)
     * @return List of TideRecord objects, or null if loading fails
     */
    public static List<TideRecord> loadSinglePort(AssetManager assetManager, String portName) {
        String filename = portName + ".tdat";
        return loadPortData(assetManager, filename);
    }
    
    /**
     * Loads tide data for a single port and returns as TideDataCache
     * @param assetManager AssetManager to read from
     * @param portName Name of the port (without .tdat extension)
     * @return TideDataCache containing only this port's data, or null if loading fails
     */
    public static TideDataCache loadSinglePortAsCache(AssetManager assetManager, String portName) {
        List<TideRecord> portTides = loadSinglePort(assetManager, portName);
        if (portTides == null || portTides.isEmpty()) {
            Log.w(TAG, "No tide data loaded for port: " + portName);
            return null;
        }
        
        // Validate we have sufficient data (at least a few tides)
        if (portTides.size() < 4) {
            Log.w(TAG, "Insufficient tide data for port " + portName + " (only " + portTides.size() + " records)");
            return null;
        }
        
        // Check data time range
        long currentTime = System.currentTimeMillis() / 1000;
        TideRecord firstTide = portTides.get(0);
        TideRecord lastTide = portTides.get(portTides.size() - 1);
        
        if (lastTide.timestamp < currentTime) {
            Log.w(TAG, "Tide data for port " + portName + " is expired (last tide: " + 
                  new java.util.Date(lastTide.timestamp * 1000L) + ")");
            // Still return data but with warning - it might be useful for historical reference
        }
        
        Log.i(TAG, "Successfully loaded " + portTides.size() + " tide records for " + portName + 
              " (valid from " + new java.util.Date(firstTide.timestamp * 1000L) + 
              " to " + new java.util.Date(lastTide.timestamp * 1000L) + ")");
        
        Map<String, List<TideRecord>> portData = new HashMap<>();
        portData.put(portName, portTides);
        
        return new TideDataCache(portData);
    }
}
