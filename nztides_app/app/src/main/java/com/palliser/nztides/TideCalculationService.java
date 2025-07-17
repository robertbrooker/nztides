package com.palliser.nztides;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Service class for calculating tide information
 * Extracted from NZTides activity for reuse in background services
 */
public class TideCalculationService {
    
    private static final String TAG = "TideCalculationService";
    private final AssetManager assetManager;
    
    public TideCalculationService(AssetManager assetManager) {
        this.assetManager = assetManager;
    }
    
    /**
     * Calculates the next tide information for a given port
     * @param port The port name to calculate tides for
     * @return NextTideInfo object or null if calculation fails
     */
    public NextTideInfo getNextTideInfo(String port) {
        if (port == null || port.trim().isEmpty()) {
            Log.w(TAG, "Port name is null or empty");
            return null;
        }
        
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
                // We need to determine if this is high or low tide
                // Read the next tide to compare heights
                int nextTideTime = TideDataReader.swapBytes(tideDataStream.readInt());
                float nextTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
                
                // If previous height < next height, then previous is low tide
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
            Log.e(TAG, "Error reading tide data for port: " + port, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error calculating tide for port: " + port, e);
            return null;
        }
    }
}
