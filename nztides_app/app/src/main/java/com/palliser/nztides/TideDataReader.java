package com.palliser.nztides;

/**
 * Utility class for reading tide data from input streams
 * Handles the I/O concerns separate from the TideRecord model
 */
public class TideDataReader {

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
}
