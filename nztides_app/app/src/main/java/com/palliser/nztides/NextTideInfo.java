package com.palliser.nztides;

/**
 * Represents the next tide event with timing information
 */
public class NextTideInfo {
    private final boolean isHighTide;
    private final long timestamp;
    private final float height;
    private final int secondsUntilTide;
    
    public NextTideInfo(boolean isHighTide, long timestamp, float height, int secondsUntilTide) {
        this.isHighTide = isHighTide;
        this.timestamp = timestamp;
        this.height = height;
        this.secondsUntilTide = secondsUntilTide;
    }
    
    public boolean isHighTide() {
        return isHighTide;
    }

    public long getTimestamp() {
        return timestamp;
    }
    
    public float getHeight() {
        return height;
    }
    
    public int getSecondsUntilTide() {
        return secondsUntilTide;
    }

    /**
     * Gets a formatted string for the tide type
     */
    public String getTideTypeString() {
        return isHighTide ? "High Tide" : "Low Tide";
    }
    
    /**
     * Gets a formatted time remaining string
     */
    public String getTimeRemainingString() {
        if (secondsUntilTide <= 0) {
            return "Now";
        }

        return "In " + TideFormatter.formatDuration(secondsUntilTide);        
    }

    @Override
    public String toString() {
        return String.format("NextTideInfo{%s at %s, height=%.2fm, in %ds}", 
                getTideTypeString(), TideFormatter.formatHourMinute(timestamp), 
                height, secondsUntilTide);
    }
}
