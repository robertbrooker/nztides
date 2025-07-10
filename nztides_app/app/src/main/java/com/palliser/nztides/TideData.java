package com.palliser.nztides;

/**
 * Represents a single tide record with timestamp, height, and tide type
 */
public class TideData {
    private final long timestamp;
    private final float height;
    private final boolean isHighTide;
    
    public TideData(long timestamp, float height, boolean isHighTide) {
        this.timestamp = timestamp;
        this.height = height;
        this.isHighTide = isHighTide;
    }
    
    /**
     * Creates a new TideData with the specified high/low tide flag
     */
    public TideData withTideType(boolean isHighTide) {
        return new TideData(this.timestamp, this.height, isHighTide);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public float getHeight() {
        return height;
    }
    
    public boolean isHighTide() {
        return isHighTide;
    }
    
    @Override
    public String toString() {
        return String.format("TideData{timestamp=%d, height=%.2f, isHighTide=%s}", 
                timestamp, height, isHighTide);
    }
}
