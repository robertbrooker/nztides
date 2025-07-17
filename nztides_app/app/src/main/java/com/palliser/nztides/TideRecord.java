package com.palliser.nztides;

/**
 * Immutable tide record representing a single tide event
 * Optimized for memory efficiency and fast lookups
 */
public final class TideRecord {
    public final long timestamp;
    public final float height;
    public final boolean isHighTide;
    
    public TideRecord(long timestamp, float height, boolean isHighTide) {
        this.timestamp = timestamp;
        this.height = height;
        this.isHighTide = isHighTide;
    }
    
    /**
     * Creates a new TideRecord with the specified high/low tide flag
     * (for compatibility with old TideData.withTideType method)
     */
    public TideRecord withTideType(boolean isHighTide) {
        return new TideRecord(this.timestamp, this.height, isHighTide);
    }
    
    // Getter methods for compatibility with old TideData usage
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TideRecord that = (TideRecord) obj;
        return timestamp == that.timestamp && 
               Float.compare(that.height, height) == 0 && 
               isHighTide == that.isHighTide;
    }
    
    @Override
    public int hashCode() {
        int result = Long.hashCode(timestamp);
        result = 31 * result + Float.hashCode(height);
        result = 31 * result + Boolean.hashCode(isHighTide);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("TideRecord{timestamp=%d, height=%.2f, isHighTide=%s}", 
                timestamp, height, isHighTide);
    }
}
