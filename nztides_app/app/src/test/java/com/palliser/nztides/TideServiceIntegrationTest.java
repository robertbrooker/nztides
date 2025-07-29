package com.palliser.nztides;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for TideService using real tide data files
 * These tests run on the JVM without requiring an Android device
 */
public class TideServiceIntegrationTest {
    
    private TideService tideService;
    private File assetsDir;
    
    @Before
    public void setUp() {
        tideService = TideService.getInstance();
        
        // Find the assets directory
        String projectRoot = System.getProperty("user.dir");
        assetsDir = new File(projectRoot, "app/src/main/assets");
        
        if (!assetsDir.exists()) {
            // Try alternative path structure
            assetsDir = new File(projectRoot, "src/main/assets");
        }
        
        assertTrue("Assets directory should exist at: " + assetsDir.getAbsolutePath(), 
                   assetsDir.exists());
    }
    
    private List<TideRecord> loadPortDataFromFile(String port) {
        try {
            File dataFile = new File(assetsDir, port + ".tdat");
            if (!dataFile.exists()) {
                return null;
            }
            
            try (InputStream inputStream = new FileInputStream(dataFile)) {
                return tideService.loadPortData(inputStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load data for port " + port + ": " + e.getMessage());
            return null;
        }
    }
    
    @Test
    public void testTideService_Auckland_WithRealData() {
        // Given: Real Auckland tide data
        String port = "Auckland";
        List<TideRecord> tides = loadPortDataFromFile(port);
        
        if (tides == null || tides.isEmpty()) {
            System.out.println("Auckland data not available, skipping test");
            return;
        }
        
        // Use a timestamp from within the loaded data (50th record should be safe)
        assertTrue("Need at least 50 tide records for this test", tides.size() > 50);
        long testTime = tides.get(50).timestamp;
        
        // When: Test TideService methods directly
        assertTrue("Data should be valid at test time", 
                   tideService.isValidAt(tides, testTime));
        
        TideInterval interval = tideService.getTideInterval(tides, testTime);
        assertNotNull("Should get valid tide interval", interval);
        assertTrue("Interval should be valid", interval.isValid());
        
        TideService.TideCalculation calc = tideService.calculateCurrentTide(tides, testTime);
        assertNotNull("Should calculate current tide", calc);
        assertTrue("Height should be reasonable", calc.height > 0 && calc.height < 10);
        assertTrue("Rise rate should be reasonable", Math.abs(calc.riseRate) < 1.0);
        
        // Verify we can get tide range
        long endTime = testTime + (7 * 24 * 3600); // 7 days
        TideRecord[] tidesInRange = tideService.getTidesInRange(tides, testTime, endTime);
        assertTrue("Should have tides in 7-day range", tidesInRange.length > 0);
        
        System.out.println("=== Auckland Test Results ===");
        System.out.println("Port: " + port);
        System.out.println("Test time: " + new java.util.Date(testTime * 1000));
        System.out.println("Current height: " + calc.height + "m");
        System.out.println("Rise rate: " + calc.riseRate + "m/hr");
        System.out.println("Previous tide: " + new java.util.Date(interval.previous.timestamp * 1000) + " (" + interval.previous.height + "m)");
        System.out.println("Next tide: " + new java.util.Date(interval.next.timestamp * 1000) + " (" + interval.next.height + "m)");
        System.out.println("Tides in next 7 days: " + tidesInRange.length);
        System.out.println("=== End Auckland Test ===");
    }
    
    @Test
    public void testTideService_Wellington_WithRealData() {
        // Given: Real Wellington tide data (if available)
        String port = "Wellington";
        List<TideRecord> tides = loadPortDataFromFile(port);
        
        if (tides == null || tides.isEmpty()) {
            System.out.println("Wellington data not available, skipping test");
            return;
        }
        
        // Use a timestamp from within the loaded data
        assertTrue("Need at least 50 tide records for this test", tides.size() > 50);
        long testTime = tides.get(50).timestamp;
        
        // When: Test TideService methods
        assertTrue("Data should be valid at test time", 
                   tideService.isValidAt(tides, testTime));
        
        TideInterval interval = tideService.getTideInterval(tides, testTime);
        assertNotNull("Should get valid tide interval", interval);
        assertTrue("Interval should be valid", interval.isValid());
        
        TideService.TideCalculation calc = tideService.calculateCurrentTide(tides, testTime);
        assertNotNull("Should calculate current tide", calc);
        assertTrue("Height should be reasonable", calc.height > 0 && calc.height < 10);
        
        System.out.println("=== Wellington Test Results ===");
        System.out.println("Port: " + port);
        System.out.println("Current height: " + calc.height + "m");
        System.out.println("Rise rate: " + calc.riseRate + "m/hr");
        System.out.println("=== End Wellington Test ===");
    }
    
    @Test
    public void testTideService_WithExpiredData() {
        // Given: Real tide data but far future timestamp
        String port = "Auckland";
        List<TideRecord> tides = loadPortDataFromFile(port);
        
        if (tides == null || tides.isEmpty()) {
            System.out.println("Auckland data not available, skipping test");
            return;
        }
        
        // Use a timestamp far in the future (year 2030)
        long futureTime = 1893456000L; // 2030-01-01
        
        // When: Test with expired timestamp
        boolean isValid = tideService.isValidAt(tides, futureTime);
        
        // Then: Should indicate data is not valid at that time
        assertFalse("Data should not be valid for future timestamp", isValid);
        
        TideInterval interval = tideService.getTideInterval(tides, futureTime);
        if (interval != null) {
            assertFalse("Interval should not be valid for expired data", interval.isValid());
        }
        
        System.out.println("=== Expired Data Test ===");
        System.out.println("Future time: " + new java.util.Date(futureTime * 1000));
        System.out.println("Data valid: " + isValid);
        System.out.println("=== End Expired Test ===");
    }
    
    @Test
    public void testTideService_MultipleTimePoints() {
        // Given: Real Auckland tide data
        String port = "Auckland";
        List<TideRecord> tides = loadPortDataFromFile(port);
        
        if (tides == null || tides.isEmpty()) {
            System.out.println("Auckland data not available, skipping test");
            return;
        }
        
        assertTrue("Need sufficient data for multiple tests", tides.size() > 100);
        
        // Test multiple different timestamps within the data range
        long[] testTimes = {
            tides.get(10).timestamp,
            tides.get(50).timestamp,
            tides.get(90).timestamp
        };
        
        for (int i = 0; i < testTimes.length; i++) {
            // When: Test TideService for each time
            assertTrue("Data should be valid at time " + i, 
                       tideService.isValidAt(tides, testTimes[i]));
            
            TideInterval interval = tideService.getTideInterval(tides, testTimes[i]);
            assertNotNull("Should get interval for time " + i, interval);
            assertTrue("Interval should be valid for time " + i, interval.isValid());
            
            TideService.TideCalculation calc = tideService.calculateCurrentTide(tides, testTimes[i]);
            assertNotNull("Should calculate tide for time " + i, calc);
            assertTrue("Height should be reasonable for time " + i, 
                       calc.height > 0 && calc.height < 10);
            
            System.out.println("=== Time Point " + i + " ===");
            System.out.println("Time: " + new java.util.Date(testTimes[i] * 1000));
            System.out.println("Height: " + calc.height + "m");
            System.out.println("Rise rate: " + calc.riseRate + "m/hr");
        }
    }
    
    @Test
    public void testDataFreshness_AtLeastThreeMonthsFromToday() {
        // Given: Real Auckland tide data (use Auckland as reference port)
        String port = "Auckland";
        List<TideRecord> tides = loadPortDataFromFile(port);
        
        if (tides == null || tides.isEmpty()) {
            System.out.println("Auckland data not available, skipping freshness test");
            return;
        }
        
        // Calculate current time and 3 months from now
        long currentTime = System.currentTimeMillis() / 1000;
        long threeMonthsFromNow = currentTime + (90 * 24 * 3600L); // 90 days = ~3 months
        
        // Find the latest tide timestamp in the data
        long latestTideTimestamp = 0;
        for (TideRecord tide : tides) {
            if (tide.timestamp > latestTideTimestamp) {
                latestTideTimestamp = tide.timestamp;
            }
        }
        
        // Format dates for error message
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ENGLISH);
        String currentDateStr = dateFormat.format(new java.util.Date(currentTime * 1000));
        String threeMonthsDateStr = dateFormat.format(new java.util.Date(threeMonthsFromNow * 1000));
        String latestDataDateStr = dateFormat.format(new java.util.Date(latestTideTimestamp * 1000));
        
        // Calculate days remaining
        long daysRemaining = (latestTideTimestamp - currentTime) / (24 * 3600);
        
        // Then: Data should extend at least 3 months into the future
        assertTrue(
            String.format(
                "Tide data is getting stale! Data should extend at least 3 months from today.\n" +
                "Current date: %s\n" +
                "Required coverage until: %s\n" +
                "Latest data available until: %s\n" +
                "Days remaining: %d\n" +
                "ACTION NEEDED: Update tide data files using downloadcsvfiles.py and binaryise_csv_tidetables.py",
                currentDateStr, threeMonthsDateStr, latestDataDateStr, daysRemaining
            ),
            latestTideTimestamp >= threeMonthsFromNow
        );
        
        // Log success message with details
        System.out.println(String.format(
            "✅ Data freshness check PASSED: %d days of data remaining (until %s)",
            daysRemaining, latestDataDateStr
        ));
    }
}
