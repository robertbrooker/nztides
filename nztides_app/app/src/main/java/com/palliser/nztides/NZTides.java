package com.palliser.nztides;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;

public class NZTides extends Activity {

    public static final int MENU_ITEM_ABOUT = Menu.FIRST + 1;
    public static final int MENU_ITEM_NOTIFICATIONS = Menu.FIRST + 2;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "NZTides";

    private String currentPort;
    private String[] recentPorts = new String[0];

    private static final String[] PORT_DISPLAY_NAMES = {"Akaroa", "Anakakata Bay", "Anawhata", "Auckland", "Ben Gunn Wharf", "Bluff", "Castlepoint", "Charleston", "Dargaville", "Deep Cove", "Dog Island", "Dunedin", "Elaine Bay", "Elie Bay", "Fishing Rock - Raoul Island", "Flour Cask Bay", "Fresh Water Basin", "Gisborne", "Green Island", "Halfmoon Bay - Oban", "Havelock", "Helensville", "Huruhi Harbour", "Jackson Bay", "Kaikōura", "Kaingaroa - Chatham Island", "Kaiteriteri", "Kaituna River Entrance", "Kawhia", "Korotiti Bay", "Leigh", "Long Island", "Lottin Point - Wakatiri", "Lyttelton", "Mana Marina", "Man o'War Bay", "Manu Bay", "Māpua", "Marsden Point", "Matiatia Bay", "Motuara Island", "Moturiki Island", "Napier", "Nelson", "New Brighton Pier", "North Cape - Otou", "Oamaru", "Ōkukari Bay", "Omaha Bridge", "Ōmokoroa", "Onehunga", "Opononi", "Ōpōtiki Wharf", "Opua", "Owenga - Chatham Island", "Paratutae Island", "Picton", "Port Chalmers", "Port Ōhope Wharf", "Port Taranaki", "Pouto Point", "Raglan", "Rangatira Point", "Rangitaiki River Entrance", "Richmond Bay", "Riverton - Aparima", "Scott Base", "Spit Wharf", "Sumner Head", "Tamaki River", "Tarakohe", "Tauranga", "Te Weka Bay", "Thames", "Timaru", "Town Basin", "Waihopai River Entrance", "Waitangi - Chatham Island", "Weiti River Entrance", "Welcombe Bay", "Wellington", "Westport", "Whakatāne", "Whanganui River Entrance", "Whangārei", "Whangaroa", "Whitianga", "Wilson Bay"};

    /**
     * Calculate current tide height and rate using cosine interpolation
     */
    private TideCalculation calculateCurrentTide(long currentTimeSeconds, TideRecord previousTide, TideRecord nextTide) {
        double omega = 2 * Math.PI / ((nextTide.getTimestamp() - previousTide.getTimestamp()) * 2);
        double amplitude = (previousTide.getHeight() - nextTide.getHeight()) / 2;
        double mean = (nextTide.getHeight() + previousTide.getHeight()) / 2;

        double currentHeight = amplitude * Math.cos(omega * (currentTimeSeconds - previousTide.getTimestamp())) + mean;
        double riseRate = -amplitude * omega * Math.sin(omega * (currentTimeSeconds - previousTide.getTimestamp())) * 3600; // cm/hr

        return new TideCalculation(currentHeight, riseRate);
    }

    /**
     * Inner class to hold tide calculation results
     */
    private static class TideCalculation {
        final double height;
        final double riseRate;

        TideCalculation(double height, double riseRate) {
            this.height = height;
            this.riseRate = riseRate;
        }
    }

    public String calculateTideOutput(String port) {
        // Try cached calculation first
        TideRepository repository = TideRepository.getInstance();
        if (repository.isReady()) {
            String result = calculateTideOutputFromCache(port);
            if (result != null) {
                return result;
            }
            Log.w("NZTides", "Cache calculation failed for port: " + port + ", falling back to file");
        }
        
        // Fallback to original file-based calculation
        return calculateTideOutputFromFile(port);
    }
    
    /**
     * Calculate tide output using cached data (new optimized version)
     */
    private String calculateTideOutputFromCache(String port) {
        try {
            TideDataCache cache = TideRepository.getInstance().getCache();
            if (cache == null || !cache.hasDataForPort(port)) {
                return null;
            }
            
            StringBuilder outputString = new StringBuilder();
            Date currentTime = new Date();
            long currentTimeSeconds = currentTime.getTime() / 1000;
            
            // Check if we have valid data for current time
            if (!cache.isValidAt(currentTimeSeconds)) {
                outputString.append("Tide data has expired. Please update the app for current predictions.");
                return outputString.toString();
            }
            
            TideInterval interval = cache.getTideInterval(port, currentTimeSeconds);
            if (interval == null || !interval.isValid()) {
                outputString.append("No tide data available for the current time at ").append(port);
                return outputString.toString();
            }
            
            // Calculate current tide using cached data
            CachedTideCalculationService calcService = new CachedTideCalculationService(getAssets());
            CachedTideCalculationService.TideCalculation currentTideCalc = 
                calcService.calculateCurrentTide(port, currentTimeSeconds);
            
            if (currentTideCalc == null) {
                return null; // Fall back to file-based calculation
            }
            
            // Generate tide graph
            String tideGraphStr = TideGraphGenerator.generateTideGraph(
                interval.previous.height, interval.next.height, 
                currentTimeSeconds, interval.previous.timestamp, interval.next.timestamp);
            
            // Start populating output string
            outputString.append("[").append(port).append("] ")
                       .append(TideFormatter.formatCurrentHeight(currentTideCalc.height)).append("m");
            
            // Display up arrow or down arrow depending on whether tide is rising or falling
            if (interval.previous.height < interval.next.height)
                outputString.append(" ↑"); // up arrow
            else
                outputString.append(" ↓"); // down arrow
            
            outputString.append(TideFormatter.formatRiseRate(Math.abs(currentTideCalc.riseRate * 100)))
                       .append(" cm/hr\n");
            outputString.append("---------------\n");
            
            displayTideTimings(outputString, currentTimeSeconds, 
                               interval.previous.timestamp, interval.next.timestamp,
                               interval.previous.height, interval.next.height);
            outputString.append("\n");
            
            // Display ASCII tide graph
            outputString.append(tideGraphStr);
            
            // Display tide records from cache
            displayTideRecordsFromCache(outputString, cache, port, currentTimeSeconds);
            
            outputString.append("The last tide in this datafile occurs at:\n");
            outputString.append(TideFormatter.formatFullDate(cache.getDataValidUntil()));
            
            return outputString.toString();
            
        } catch (Exception e) {
            Log.e("NZTides", "Error calculating tide output from cache", e);
            return null;
        }
    }
    
    /**
     * Original file-based calculation (fallback)
     */
    private String calculateTideOutputFromFile(String port) {
        AssetManager assetManager = getAssets();
        StringBuilder outputString = new StringBuilder();

        long nextTideTime, previousTideTime;
        float nextTideHeight;
        float previousTideHeight;
        Date currentTime = new Date();
        long currentTimeSeconds = currentTime.getTime() / 1000;
        long lastTideInFile;


        try (DataInputStream tideDataStream = new DataInputStream(assetManager.open(port + ".tdat", 1))) {
            // Read station name (currently not used due to encoding issues)
            tideDataStream.readLine();

            // Read timestamp for last tide in datafile
            lastTideInFile = TideDataReader.swapBytes(tideDataStream.readInt());

            // Skip number of records in datafile
            tideDataStream.readInt();

            previousTideTime = TideDataReader.swapBytes(tideDataStream.readInt());
            previousTideHeight = (float) (tideDataStream.readByte()) / 10.0f;

            if (previousTideTime > currentTimeSeconds) {
                outputString.append("The first tide in this datafile doesn't occur until ");
                outputString.append(TideFormatter.formatFullDate(previousTideTime));
                outputString.append(". The app should start working properly about then.");
            } else {

                // Look through tide data file for current time
                while (true) {
                    nextTideTime = TideDataReader.swapBytes(tideDataStream.readInt());
                    nextTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
                    if (nextTideTime > currentTimeSeconds) {
                        break;
                    }
                    previousTideTime = nextTideTime;
                    previousTideHeight = nextTideHeight;
                }

                // Create TideRecord objects for calculation
                TideRecord previousTide = new TideRecord(previousTideTime, previousTideHeight, false); // tide type determined later
                TideRecord nextTide = new TideRecord(nextTideTime, nextTideHeight, false); // tide type determined later

                String tideGraphStr = TideGraphGenerator.generateTideGraph(
                    previousTideHeight, nextTideHeight, 
                    currentTimeSeconds, previousTideTime, nextTideTime);

                TideCalculation currentTideCalc = calculateCurrentTide(currentTimeSeconds, previousTide, nextTide);

                // Start populating output string
                outputString.append("[").append(port).append("] ").append(TideFormatter.formatCurrentHeight(currentTideCalc.height)).append("m");

                // Display up arrow or down arrow depending on whether tide is rising or falling
                if (previousTideHeight < nextTideHeight)
                    outputString.append(" ↑"); // up arrow
                else
                    outputString.append(" ↓"); // down arrow

                outputString.append(TideFormatter.formatRiseRate(Math.abs(currentTideCalc.riseRate * 100))).append(" cm/hr\n");
                outputString.append("---------------\n");

                displayTideTimings(outputString, currentTimeSeconds, previousTideTime, nextTideTime, previousTideHeight, nextTideHeight);
                outputString.append("\n");

                // Display ASCII tide graph
                outputString.append(tideGraphStr);            // Display tide records grouped by day
                displayTideRecords(outputString, tideDataStream, previousTideTime, previousTideHeight, nextTideTime, nextTideHeight);
                outputString.append("The last tide in this datafile occurs at:\n");
                outputString.append(TideFormatter.formatFullDate(lastTideInFile));
            }

        } catch (IOException e) {
            outputString.append("Problem reading tide data\n\nTry selecting the port again, sometimes the ports available change with an upgrade. If this doesn't work it is either because the tide data is out of date or you've found some bug, try looking for an update.");
        }
        return outputString.toString();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore current port from settings file
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        currentPort = settings.getString("CurrentPort", "Auckland");
        loadRecentPorts();

        // If no recent ports, add the current port
        if (recentPorts.length == 0) {
            updateRecentPorts(currentPort);
        }

        setContentView(R.layout.main);

        // Initialize notification system
        requestNotificationPermissionIfNeeded();
        initializeNotificationSystem();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void initializeNotificationSystem() {
        try {
            // Create notification channel
            NotificationChannelManager channelManager = new NotificationChannelManager(this);
            channelManager.createTideNotificationChannel();
            
            // Start notification service if enabled
            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
            boolean notificationsEnabled = settings.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, true);
            if (notificationsEnabled) {
                TideUpdateReceiver.scheduleNotificationUpdates(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize notification system", e);
        }
    }

    private void loadRecentPorts() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        String recent = settings.getString(Constants.PREFS_RECENT_PORTS, "");
        if (!recent.isEmpty()) {
            recentPorts = recent.split(",");
        } else {
            recentPorts = new String[0];
        }
    }

    private void saveRecentPorts() {
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Constants.PREFS_RECENT_PORTS, String.join(",", recentPorts));
        editor.apply();
    }

    private void updateRecentPorts(String port) {
        LinkedList<String> list = new LinkedList<>();
        list.add(port);
        for (String p : recentPorts) {
            if (!p.equals(port)) list.add(p);
        }
        while (list.size() > Constants.RECENT_PORTS_COUNT) list.removeLast();
        recentPorts = list.toArray(new String[0]);
        saveRecentPorts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        int menuIndex = Menu.FIRST;
        if (recentPorts.length > 0) {
            for (String recentPort : recentPorts) {
                menu.add(0, menuIndex++, 0, recentPort);
            }
            // Add a disabled separator instead of a submenu
            MenuItem sep = menu.add(0, menuIndex++, 0, "──────────");
            sep.setEnabled(false);
        }
        HashSet<String> recentSet = new HashSet<>();
        Collections.addAll(recentSet, recentPorts);
        for (String portDisplayName : PORT_DISPLAY_NAMES) {
            if (!recentSet.contains(portDisplayName)) {
                menu.add(0, menuIndex++, 0, portDisplayName);
            }
        }
        menu.add(0, MENU_ITEM_ABOUT, 0, "About");
        menu.add(0, MENU_ITEM_NOTIFICATIONS, 0, "Tide Notifications");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        String title = (String) item.getTitle();
        // Check if selected port is in recentPorts or portdisplaynames
        for (String port : PORT_DISPLAY_NAMES) {
            if (port.equals(title)) {
                currentPort = port;
                updateRecentPorts(port);
                invalidateOptionsMenu(); // Rebuild menu with updated recent ports
                this.onResume();
                return true;
            }
        }
        for (String port : recentPorts) {
            if (port.equals(title)) {
                currentPort = port;
                updateRecentPorts(port);
                invalidateOptionsMenu(); // Rebuild menu with updated recent ports
                this.onResume();
                return true;
            }
        }
        switch (id) {
            case MENU_ITEM_ABOUT:
                TextView tv = new TextView(this);
                tv.setText(R.string.AboutString);
                ScrollView sv = new ScrollView(this);
                sv.addView(tv);
                setContentView(sv);
                return true;
            case MENU_ITEM_NOTIFICATIONS:
                startActivity(new Intent(this, NotificationSettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.main);

        TextView tideTextView = findViewById(R.id.tide_text_view);
        String outputString = calculateTideOutput(currentPort);
        tideTextView.setText(outputString);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save user preferences
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("CurrentPort", currentPort);
        editor.putString(Constants.PREFS_RECENT_PORTS, String.join(",", recentPorts));
        editor.apply();
    }

    /**
     * Display timing information for tides
     */
    private void displayTideTimings(StringBuilder outputString, long currentTimeSeconds,
                                    long previousTideTime, long nextTideTime,
                                    float previousTideHeight, float nextTideHeight) {
        long timeToPrevious = (currentTimeSeconds - previousTideTime);
        long timeToNext = (nextTideTime - currentTimeSeconds);
        boolean isHighTideNext = (nextTideHeight > previousTideHeight);

        if (timeToPrevious < timeToNext) {
            if (isHighTideNext) {
                outputString.append("Low tide ").append(TideFormatter.formatHourMinute(previousTideTime)).append(" (").append(previousTideHeight).append("m) ").append(TideFormatter.formatDuration(timeToPrevious)).append(" ago\n");
            } else {
                outputString.append("HIGH tide ").append(TideFormatter.formatHourMinute(previousTideTime)).append(" (").append(previousTideHeight).append("m) ").append(TideFormatter.formatDuration(timeToPrevious)).append(" ago\n");
            }
        } else {
            if (isHighTideNext) {
                outputString.append("HIGH tide ").append(TideFormatter.formatHourMinute(nextTideTime)).append(" (").append(nextTideHeight).append("m) in ").append(TideFormatter.formatDuration(timeToNext)).append("\n");
            } else {
                outputString.append("Low tide ").append(TideFormatter.formatHourMinute(nextTideTime)).append(" (").append(nextTideHeight).append("m) in ").append(TideFormatter.formatDuration(timeToNext)).append("\n");
            }
        }
    }

    /**
     * Display tide timings using cached data
     */
    private void displayTideTimingsFromCache(StringBuilder outputString, long currentTimeSeconds, TideInterval interval) {
        long timeToPrevious = currentTimeSeconds - interval.previous.timestamp;
        long timeToNext = interval.next.timestamp - currentTimeSeconds;
        
        if (timeToPrevious < timeToNext) {
            // Previous tide is closer
            if (interval.next.isHighTide) {
                outputString.append("Low tide (").append(TideFormatter.formatCurrentHeight(interval.previous.height))
                           .append("m) ").append(TideFormatter.formatTimeAgo(timeToPrevious)).append("\n");
            } else {
                outputString.append("High tide (").append(TideFormatter.formatCurrentHeight(interval.previous.height))
                           .append("m) ").append(TideFormatter.formatTimeAgo(timeToPrevious)).append("\n");
            }
        } else {
            // Next tide is closer or equal
            if (interval.next.isHighTide) {
                outputString.append("High tide (").append(TideFormatter.formatCurrentHeight(interval.next.height))
                           .append("m) in ").append(TideFormatter.formatTimeUntil(timeToNext)).append("\n");
            } else {
                outputString.append("Low tide (").append(TideFormatter.formatCurrentHeight(interval.next.height))
                           .append("m) in ").append(TideFormatter.formatTimeUntil(timeToNext)).append("\n");
            }
        }
    }
    
    /**
     * Display tide records using cached data
     */
    private void displayTideRecordsFromCache(StringBuilder outputString, TideDataCache cache, String port, long currentTimeSeconds) {
        // Get tides for next 30 days (roughly 120 tides)
        long endTime = currentTimeSeconds + (30 * 24 * 3600);
        TideRecord[] upcomingTides = cache.getTidesInRange(port, currentTimeSeconds, endTime);
        
        if (upcomingTides.length == 0) {
            outputString.append("\nNo upcoming tide data available.\n");
            return;
        }
        
        String lastDay = "";
        String lastMonth = TideFormatter.formatMonth(upcomingTides[0].timestamp);   // forces it to not display until the next month
        
        // Display the upcoming tides
        for (int i = 0; i < Math.min(upcomingTides.length, Constants.RECORDS_TO_DISPLAY); i++) {
            TideRecord tide = upcomingTides[i];
            
            String dayLabel = TideFormatter.formatDay(tide.timestamp);
            if (!dayLabel.equals(lastDay)) {
                lastDay = dayLabel;
                String monthLabel = TideFormatter.formatMonth(tide.timestamp);
                if (!monthLabel.equals(lastMonth)) {
                    outputString.append("\n---==== ").append(monthLabel).append(" ====---\n");
                    lastMonth = monthLabel;
                }
                outputString.append(dayLabel).append("\n");
            }
            
            // Format tide record using the same format as displayTideRecords
            TideRecord tideData = new TideRecord(tide.timestamp, tide.height, tide.isHighTide);
            outputString.append(TideFormatter.formatTideRecord(tideData));
        }
    }

    /**
     * Display tide records using file-based data (original fallback method)
     */
    private void displayTideRecords(StringBuilder outputString, DataInputStream tideDataStream,
                                    long previousTideTime, float previousTideHeight,
                                    long nextTideTime, float nextTideHeight) throws IOException {
        String lastDay = "";
        
        // Create first two tide records
        boolean firstIsHigh = !(nextTideHeight > previousTideHeight);
        TideRecord firstTide = new TideRecord(previousTideTime, previousTideHeight, firstIsHigh);
        TideRecord secondTide = new TideRecord(nextTideTime, nextTideHeight, !firstIsHigh);
        
        String lastMonth = TideFormatter.formatMonth(firstTide.getTimestamp());
        
        // First tide record
        String dayLabel = TideFormatter.formatDay(firstTide.getTimestamp());
        if (!dayLabel.equals(lastDay)) {
            outputString.append(dayLabel).append("\n");
            lastDay = dayLabel;
        }
        outputString.append(TideFormatter.formatTideRecord(firstTide));
        
        // Second tide record
        dayLabel = TideFormatter.formatDay(secondTide.getTimestamp());
        String monthLabel;
        
        if (!dayLabel.equals(lastDay)) {
            lastDay = dayLabel;
            monthLabel = TideFormatter.formatMonth(secondTide.getTimestamp());
            if (!monthLabel.equals(lastMonth)) {
                outputString.append("\n---==== ").append(monthLabel).append(" ====---\n");
                lastMonth = monthLabel;
            }
            outputString.append(dayLabel).append("\n");
        }
        outputString.append(TideFormatter.formatTideRecord(secondTide));
        
        // Remaining tide records
        boolean currentIsHigh = secondTide.isHighTide;
        for (int k = 0; k < Constants.RECORDS_TO_DISPLAY; k++) {
            currentIsHigh = !currentIsHigh;
            TideRecord currentTide = TideDataReader.readFromStream(tideDataStream).withTideType(currentIsHigh);
            
            dayLabel = TideFormatter.formatDay(currentTide.getTimestamp());
            if (!dayLabel.equals(lastDay)) {
                lastDay = dayLabel;
                monthLabel = TideFormatter.formatMonth(currentTide.getTimestamp());
                if (!monthLabel.equals(lastMonth)) {
                    outputString.append("\n---==== ").append(monthLabel).append(" ====---\n");
                    lastMonth = monthLabel;
                }
                outputString.append(dayLabel).append("\n");
            }
            outputString.append(TideFormatter.formatTideRecord(currentTide));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                initializeNotificationSystem();
            } else {
                Toast.makeText(this, "Notification permission denied. You can enable it in settings.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
