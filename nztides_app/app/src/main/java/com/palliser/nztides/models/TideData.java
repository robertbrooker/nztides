package com.palliser.nztides.models;

import java.util.List;

/**
 * Complete tide data structure containing all information needed for display
 * Validated and enriched data ready for consumption by UI components
 */
public final class TideData {
    public final String port;
    public final long currentTimeSeconds;
    public final List<TideRecord> allTides;
    public final TideInterval currentInterval;
    public final TideCalculation currentTideCalculation;
    public final TideRecord lastTideInData;
    public final TideRecord[] upcomingTides;
    
    public TideData(String port, long currentTimeSeconds, List<TideRecord> allTides,
                    TideInterval currentInterval, TideCalculation currentTideCalculation,
                    TideRecord lastTideInData, TideRecord[] upcomingTides) {
        this.port = port;
        this.currentTimeSeconds = currentTimeSeconds;
        this.allTides = allTides;
        this.currentInterval = currentInterval;
        this.currentTideCalculation = currentTideCalculation;
        this.lastTideInData = lastTideInData;
        this.upcomingTides = upcomingTides;
    }
    
    /**
     * Inner class to hold tide calculation results
     */
    public static class TideCalculation {
        public final double height;
        public final double riseRate;
        
        public TideCalculation(double height, double riseRate) {
            this.height = height;
            this.riseRate = riseRate;
        }
    }
}
