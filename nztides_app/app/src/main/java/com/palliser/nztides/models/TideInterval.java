package com.palliser.nztides.models;

/**
 * Represents an interval between two tides for interpolation calculations
 * Used for cosine interpolation of current tide height
 */
public final class TideInterval {
    public final TideRecord previous;
    public final TideRecord next;
    public final boolean incomingTide;
    public final long timeToPrevious;
    public final long timeToNext;
    public final TideRecord closestTide;
    public final boolean isClosestTideInTheFuture;
    
    public TideInterval(TideRecord previous, TideRecord next, long currentTimeSeconds) {
        this.previous = previous;
        this.next = next;
        this.incomingTide = previous.height < next.height;
        this.timeToPrevious = currentTimeSeconds - previous.timestamp;
        this.timeToNext = next.timestamp - currentTimeSeconds;
        
        if (timeToPrevious < timeToNext) {
            this.closestTide = previous;
            this.isClosestTideInTheFuture = false;
        } else {
            this.closestTide = next;
            this.isClosestTideInTheFuture = true;
        }
    }
    
    /**
     * Checks if this interval is valid for calculations
     */
    public boolean isValid() {
        return previous != null && next != null;
    }

    @Override
    public String toString() {
        return String.format("TideInterval{previous=%s, next=%s, incomingTide=%s}", 
                previous, next, incomingTide);
    }
}
