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
     * Loads all tide data from assets into a TideDataCache
     * @param assetManager Android AssetManager to read .tdat files
     * @return TideDataCache containing all tide data, or null if loading fails
     */
    public static TideDataCache loadFromAssets(AssetManager assetManager) {
        Map<String, List<TideRecord>> allPortData = new HashMap<>();
        
        // Get list of all .tdat files
        String[] assetFiles;
        try {
            assetFiles = assetManager.list("");
            if (assetFiles == null) {
                Log.e(TAG, "Failed to list asset files");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing asset files", e);
            return null;
        }
        
        int loadedPorts = 0;
        int failedPorts = 0;
        
        // Load each .tdat file
        for (String filename : assetFiles) {
            if (filename.endsWith(".tdat")) {
                String portName = filename.substring(0, filename.length() - 5); // Remove .tdat extension
                
                try {
                    List<TideRecord> portTides = loadPortData(assetManager, filename);
                    if (portTides != null && !portTides.isEmpty()) {
                        allPortData.put(portName, portTides);
                        loadedPorts++;
                        Log.d(TAG, "Loaded " + portTides.size() + " tides for port: " + portName);
                    } else {
                        Log.w(TAG, "No tide data loaded for port: " + portName);
                        failedPorts++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load port data: " + portName, e);
                    failedPorts++;
                }
            }
        }
        
        Log.i(TAG, "Tide data loading complete. Loaded: " + loadedPorts + ", Failed: " + failedPorts);
        
        if (allPortData.isEmpty()) {
            Log.e(TAG, "No tide data loaded successfully");
            return null;
        }
        
        return new TideDataCache(allPortData);
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
            int lastTideTimestamp = TideDataReader.swapBytes(stream.readInt());
            
            // Read number of records
            int numRecords = TideDataReader.swapBytes(stream.readInt());
            
            Log.d(TAG, "Loading " + numRecords + " records for " + filename);
            
            boolean isHighTide = false; // Tides alternate between high and low
            
            // Read all tide records
            for (int i = 0; i < numRecords; i++) {
                try {
                    int timestamp = TideDataReader.swapBytes(stream.readInt());
                    float height = (float) (stream.readByte()) / 10.0f;
                    
                    // Alternate between high and low tides
                    isHighTide = !isHighTide;
                    
                    TideRecord record = new TideRecord(timestamp, height, isHighTide);
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
}
