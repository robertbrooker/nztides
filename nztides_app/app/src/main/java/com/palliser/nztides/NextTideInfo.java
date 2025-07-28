package com.palliser.nztides;

/**
 * Represents the next tide event with timing information
 */
public class NextTideInfo {
    private final TideRecord tideRecord;
    private final int secondsUntilTide;
    
    public NextTideInfo(TideRecord tideRecord, int secondsUntilTide) {
        this.tideRecord = tideRecord;
        this.secondsUntilTide = secondsUntilTide;
    }
    
    public boolean isHighTide() {
        return tideRecord.isHighTide;
    }

    public long getTimestamp() {
        return tideRecord.timestamp;
    }
    
    public float getHeight() {
        return tideRecord.height;
    }
    
    public TideRecord getTideRecord() {
        return tideRecord;
    }
    
    public int getSecondsUntilTide() {
        return secondsUntilTide;
    }

    /**
     * Gets a formatted string for the tide type
     */
    public String getTideTypeString() {
        return tideRecord.isHighTide ? "High Tide" : "Low Tide";
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
                getTideTypeString(), TideFormatter.formatHourMinute(tideRecord.timestamp), 
                tideRecord.height, secondsUntilTide);
    }
}
