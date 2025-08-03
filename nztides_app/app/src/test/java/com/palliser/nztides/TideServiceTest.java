package com.palliser.nztides;

import com.palliser.nztides.models.TideRecord;
import com.palliser.nztides.models.TideInterval;
import com.palliser.nztides.models.NextTideInfo;
import com.palliser.nztides.TideService.TideDataException;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for TideService logic
 * These tests focus on business logic without requiring Android framework dependencies
 */
public class TideServiceTest {
    
    private TideService tideService;
    
    @Before
    public void setUp() {
        tideService = TideService.getInstance();
    }
    
    @Test
    public void testGetInstance_ReturnsSingleton() {
        // Given/When
        TideService instance1 = TideService.getInstance();
        TideService instance2 = TideService.getInstance();
        
        // Then
        assertNotNull("Instance should not be null", instance1);
        assertSame("Should return same instance (singleton)", instance1, instance2);
    }
    
    @Test
    public void testIsValidAt_EmptyList_ReturnsFalse() {
        // Given
        List<TideRecord> emptyTides = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When
        boolean isValid = tideService.isValidAt(emptyTides, currentTime);
        
        // Then
        assertFalse("Empty tide list should not be valid", isValid);
    }
    
    @Test
    public void testIsValidAt_NullList_ReturnsFalse() {
        // Given
        List<TideRecord> nullTides = null;
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When
        boolean isValid = tideService.isValidAt(nullTides, currentTime);
        
        // Then
        assertFalse("Null tide list should not be valid", isValid);
    }
    
    @Test
    public void testGetTidesInRange_EmptyList_ReturnsEmptyArray() {
        // Given
        List<TideRecord> emptyTides = new ArrayList<>();
        long startTime = System.currentTimeMillis() / 1000;
        long endTime = startTime + 3600; // 1 hour later
        
        // When
        TideRecord[] result = tideService.getTidesInRange(emptyTides, startTime, endTime);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty array", 0, result.length);
    }
    
    @Test
    public void testGetTidesInRange_NullList_ReturnsEmptyArray() {
        // Given
        List<TideRecord> nullTides = null;
        long startTime = System.currentTimeMillis() / 1000;
        long endTime = startTime + 3600;
        
        // When
        TideRecord[] result = tideService.getTidesInRange(nullTides, startTime, endTime);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty array", 0, result.length);
    }
    
    @Test
    public void testGetTidesInRange_WithValidData_FiltersCorrectly() {
        // Given
        List<TideRecord> tides = createSampleTideData();
        long startTime = 1000;
        long endTime = 3000;
        
        // When
        TideRecord[] result = tideService.getTidesInRange(tides, startTime, endTime);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Should return tides within range", 2, result.length);
        assertTrue("All returned tides should be within range", 
                   result[0].timestamp >= startTime && result[0].timestamp <= endTime);
        assertTrue("All returned tides should be within range", 
                   result[1].timestamp >= startTime && result[1].timestamp <= endTime);
    }
    
    @Test
    public void testGetTideInterval_EmptyList_ThrowsException() {
        // Given
        List<TideRecord> emptyTides = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When/Then
        try {
            TideInterval result = tideService.getTideInterval(emptyTides, currentTime);
            fail("Empty tide list should throw TideDataException");
        } catch (TideDataException e) {
            // Expected behavior
            assertTrue("Exception message should be meaningful", e.getMessage().contains("tide data"));
        }
    }
    
    @Test
    public void testGetNextTideInfo_EmptyList_ThrowsException() {
        // Given
        List<TideRecord> emptyTides = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When/Then
        try {
            NextTideInfo result = tideService.getNextTideInfo(emptyTides, currentTime);
            fail("Empty tide list should throw exception");
        } catch (TideDataException e) {
            // Expected behavior
            assertTrue("Exception message should be meaningful", e.getMessage().contains("tide"));
        } catch (RuntimeException e) {
            // Also expected in unit test environment due to unmocked Log.e
            assertTrue("Exception should be from Log.e", e.getMessage().contains("Log") || e.getMessage().contains("not mocked"));
        }
    }
    
    @Test
    public void testCalculateCurrentTide_EmptyList_ThrowsException() {
        // Given
        List<TideRecord> emptyTides = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When/Then
        try {
            TideService.TideCalculation result = tideService.calculateCurrentTide(emptyTides, currentTime);
            fail("Empty tide list should throw exception");
        } catch (TideDataException e) {
            // Expected behavior
            assertTrue("Exception message should be meaningful", e.getMessage().contains("tide"));
        } catch (RuntimeException e) {
            // Also expected in unit test environment due to unmocked Log.e
            assertTrue("Exception should be from Log.e", e.getMessage().contains("Log") || e.getMessage().contains("not mocked"));
        }
    }
    
    /**
     * Helper method to create sample tide data for testing
     */
    private List<TideRecord> createSampleTideData() {
        List<TideRecord> tides = new ArrayList<>();
        
        // Create some sample tide records
        tides.add(new TideRecord(500, 1.5f, true));   // Outside range (low)
        tides.add(new TideRecord(1500, 2.0f, false)); // Inside range
        tides.add(new TideRecord(2500, 1.0f, true));  // Inside range
        tides.add(new TideRecord(3500, 3.0f, false)); // Outside range (high)
        tides.add(new TideRecord(4500, 1.8f, true));  // Outside range (high)
        
        return tides;
    }
}
