package com.palliser.nztides;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Utility class for reading tide data from input streams
 * Handles the I/O concerns separate from the TideData model
 */
public class TideDataReader {
    
    /**
     * Reads a tide record from a DataInputStream
     * @param stream The input stream to read from
     * @return A new TideData object with the read values
     * @throws IOException if there's an error reading from the stream
     */
    public static TideData readFromStream(DataInputStream stream) throws IOException {
        // Swap bytes to handle endianness
        int rawTimestamp = stream.readInt();
        int swappedTimestamp = swapBytes(rawTimestamp);
        
        // Read height as byte and convert to float
        float height = (float) (stream.readByte()) / 10.0f;
        
        // Note: isHighTide will be determined by context (alternating pattern)
        // The caller should use withTideType() to set the correct tide type
        return new TideData(swappedTimestamp, height, false);
    }
    
    /**
     * Swaps byte order for endianness conversion
     */
    private static int swapBytes(int value) {
        int b1 = (value >>  0) & 0xff;
        int b2 = (value >>  8) & 0xff;
        int b3 = (value >> 16) & 0xff;
        int b4 = (value >> 24) & 0xff;
        return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
    }
}
