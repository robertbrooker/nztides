package com.palliser.nztides;

/**
 * Represents an interval between two tides for interpolation calculations
 * Used for cosine interpolation of current tide height
 */
public final class TideInterval {
    public final TideRecord previous;
    public final TideRecord next;
    
    public TideInterval(TideRecord previous, TideRecord next) {
        this.previous = previous;
        this.next = next;
    }
    
    /**
     * Checks if this interval is valid for calculations
     */
    public boolean isValid() {
        return previous != null && next != null;
    }

    /**
     * Returns the duration of this interval in seconds
     */
    public long getDurationSeconds() {
        return isValid() ? next.timestamp - previous.timestamp : 0;
    }
    
    @Override
    public String toString() {
        return String.format("TideInterval{previous=%s, next=%s}", previous, next);
    }
}
