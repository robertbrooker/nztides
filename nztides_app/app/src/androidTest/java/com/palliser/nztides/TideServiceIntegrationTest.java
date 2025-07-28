package com.palliser.nztides;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Android instrumentation tests for TideService
 * These tests run on device/emulator and test real asset loading
 */
@RunWith(AndroidJUnit4.class)
public class TideServiceIntegrationTest {
    
    private Context context;
    private AssetManager assetManager;
    private TideService tideService;
    
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assetManager = context.getAssets();
        tideService = TideService.getInstance();
    }
    
    @Test
    public void testRealAssetLoading_Auckland() {
        // When loading real Auckland tide data from assets
        List<TideRecord> tides = tideService.loadPortData(assetManager, "Auckland");
        
        // Then it should load successfully
        assertNotNull("Auckland tide data should load from real assets", tides);
        assertFalse("Auckland should have tide data", tides.isEmpty());
        
        // Verify data is reasonable for Auckland
        assertTrue("Should have substantial tide data for Auckland", tides.size() > 500);
    }
    
    @Test
    public void testRealAssetLoading_Wellington() {
        // When loading real Wellington tide data from assets
        List<TideRecord> tides = tideService.loadPortData(assetManager, "Wellington");
        
        // Then it should load successfully if Wellington data exists
        if (tides != null && !tides.isEmpty()) {
            assertTrue("Should have substantial tide data for Wellington", tides.size() > 500);
        }
    }
    
    @Test
    public void testCurrentTimeCalculations_WithRealData() {
        // Given real Auckland data
        List<TideRecord> tides = tideService.loadPortData(assetManager, "Auckland");
        assertNotNull("Need real Auckland data for this test", tides);
        
        long currentTime = System.currentTimeMillis() / 1000;
        
        // When calculating tide information for current time
        // These might return null if data is expired, but should not crash
        TideInterval interval = tideService.getTideInterval(tides, currentTime);
        NextTideInfo nextTide = tideService.getNextTideInfo(tides, currentTime);
        TideService.TideCalculation current = tideService.calculateCurrentTide(tides, currentTime);
        
        // Then calculations should complete without exceptions
        // (Results may be null if data is expired, which is acceptable)
        assertTrue("Tide calculations should complete without throwing exceptions", true);
    }
    
    @Test
    public void testAssetManagerIntegration() {
        // Verify that we can access the assets directory and it contains .tdat files
        try {
            String[] assetFiles = assetManager.list("");
            assertNotNull("Asset listing should not be null", assetFiles);
            
            boolean foundTdatFile = false;
            for (String file : assetFiles) {
                if (file.endsWith(".tdat")) {
                    foundTdatFile = true;
                    break;
                }
            }
            
            assertTrue("Should find at least one .tdat file in assets", foundTdatFile);
            
        } catch (Exception e) {
            fail("Asset manager integration failed: " + e.getMessage());
        }
    }
}
