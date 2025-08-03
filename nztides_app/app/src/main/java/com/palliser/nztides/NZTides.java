package com.palliser.nztides;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.palliser.nztides.models.TideData;
import com.palliser.nztides.models.TideRecord;
import com.palliser.nztides.notification.NotificationChannelManager;
import com.palliser.nztides.notification.TideNotificationService;
import com.palliser.nztides.notification.TideUpdateReceiver;
import com.palliser.nztides.notification.NotificationSettingsActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class NZTides extends Activity {

    public static final int MENU_ITEM_ABOUT = Menu.FIRST + 1;
    public static final int MENU_ITEM_NOTIFICATIONS = Menu.FIRST + 2;
    public static final int MENU_ITEM_FIRST_PORT = MENU_ITEM_NOTIFICATIONS + 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TAG = "NZTides";

    private String currentPort;
    private String[] recentPorts = new String[0];

    private List<TideRecord> currentPortTides;
    private CurrentTidesService currentTidesService;

    private static final String[] PORT_DISPLAY_NAMES = {"Akaroa", "Anakakata Bay", "Anawhata", "Auckland", "Ben Gunn Wharf", "Bluff", "Castlepoint", "Charleston", "Dargaville", "Deep Cove", "Dog Island", "Dunedin", "Elaine Bay", "Elie Bay", "Fishing Rock - Raoul Island", "Flour Cask Bay", "Fresh Water Basin", "Gisborne", "Green Island", "Halfmoon Bay - Oban", "Havelock", "Helensville", "Huruhi Harbour", "Jackson Bay", "Kaikōura", "Kaingaroa - Chatham Island", "Kaiteriteri", "Kaituna River Entrance", "Kawhia", "Korotiti Bay", "Leigh", "Long Island", "Lottin Point - Wakatiri", "Lyttelton", "Mana Marina", "Man o'War Bay", "Manu Bay", "Māpua", "Marsden Point", "Matiatia Bay", "Motuara Island", "Moturiki Island", "Napier", "Nelson", "New Brighton Pier", "North Cape - Otou", "Oamaru", "Ōkukari Bay", "Omaha Bridge", "Ōmokoroa", "Onehunga", "Opononi", "Ōpōtiki Wharf", "Opua", "Owenga - Chatham Island", "Paratutae Island", "Picton", "Port Chalmers", "Port Ōhope Wharf", "Port Taranaki", "Pouto Point", "Raglan", "Rangatira Point", "Rangitaiki River Entrance", "Richmond Bay", "Riverton - Aparima", "Scott Base", "Spit Wharf", "Sumner Head", "Tamaki River", "Tarakohe", "Tauranga", "Te Weka Bay", "Thames", "Timaru", "Town Basin", "Waihopai River Entrance", "Waitangi - Chatham Island", "Weiti River Entrance", "Welcombe Bay", "Wellington", "Westport", "Whakatāne", "Whanganui River Entrance", "Whangārei", "Whangaroa", "Whitianga", "Wilson Bay"};

    public void loadDataForPortAndDisplayOnScreen(String port) {

        String outputString;

        try {
            // Always load fresh data for new port
            if (currentTidesService == null) {
                currentTidesService = new CurrentTidesService(this);
            }
            TideData tideData = currentTidesService.loadTideData(port, System.currentTimeMillis() / 1000);
            currentPortTides = tideData.allTides;

            outputString = getTideScreenDisplayText(tideData);

        } catch (CurrentTidesService.TideDataException e) {
            outputString = e.getMessage();
        } catch (Exception e) {
            Log.e(TAG, "Error loading tide data for " + port, e);
            outputString = "Error loading tide data for " + port + ": " + e.getMessage();
        }

        setContentView(R.layout.main);
        TextView tideTextView = findViewById(R.id.tide_text_view);
        tideTextView.setText(outputString);
    }
    
    private String refreshTideOutput() throws CurrentTidesService.TideDataException {
        TideData tideData = currentTidesService.refreshTideData(currentPort, currentPortTides, 
            System.currentTimeMillis() / 1000);
        return getTideScreenDisplayText(tideData);
    }
    
    /**
     * Calculate tide output using loaded and validated tide data
     * @param tideData Complete validated tide data
     * @return Formatted tide output string
     */
    public String getTideScreenDisplayText(TideData tideData) {
        try {
            StringBuilder outputString = new StringBuilder();

            generateCurrentTideDetails(tideData, outputString);

            generateTideGraph(tideData, outputString);

            generateTideList(tideData, outputString);

            return outputString.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating tide output from data", e);
            return "Error calculating tide data for " + tideData.port + ". Please try again.";
        }
    }

    private void generateTideList(TideData tideData, StringBuilder outputString) {
        // Display tide records from loaded data
        displayTideRecordsFromData(outputString, tideData.upcomingTides);

        outputString.append("The last tide in this datafile occurs at:\n");
        outputString.append(TideFormatter.formatFullDate(tideData.lastTideInData.timestamp));
    }

    private static void generateTideGraph(TideData tideData, StringBuilder outputString) {
        // Generate tide graph
        // Display ASCII tide graph
        String tideGraphStr = TideGraphGenerator.generateTideGraph(
                tideData.currentInterval.previous, tideData.currentInterval.next, tideData.currentTimeSeconds);
        outputString.append(tideGraphStr);
    }

    private void generateCurrentTideDetails(TideData tideData, StringBuilder outputString) {
        // Start populating output string
        outputString.append("[").append(tideData.port).append("] ")
                   .append(TideFormatter.formatCurrentHeight(tideData.currentTideCalculation.height)).append("m");

        // Display up arrow or down arrow depending on whether tide is rising or falling
        if (tideData.currentInterval.incomingTide)
            outputString.append(" ↑"); // up arrow
        else
            outputString.append(" ↓"); // down arrow

        outputString.append(TideFormatter.formatRiseRate(Math.abs(tideData.currentTideCalculation.riseRate * 100)))
                   .append(" cm/hr\n");
        outputString.append("---------------\n");

        displayTideTimings(outputString, tideData);
        outputString.append("\n");
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        // Restore current port from settings file
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        currentPort = settings.getString("CurrentPort", "Auckland");
        loadRecentPorts();
        Log.d(TAG, "Current port loaded: " + currentPort);
        Log.d(TAG, "Recent ports loaded: " + Arrays.toString(recentPorts));

        // If no recent ports, add the current port
        if (recentPorts.length == 0) {
            updateRecentPorts(currentPort);
        }

        Log.d(TAG, "Calculating tide output for current port: " + currentPort);
        loadDataForPortAndDisplayOnScreen(currentPort);
        Log.d(TAG, "Tide output calculated for port: " + currentPort);

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
            // Create notification channel (required for Android 8.0+)
            NotificationChannelManager channelManager = new NotificationChannelManager(this);
            channelManager.createTideNotificationChannel();
            
            // Check if notifications are enabled in preferences
            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
            boolean notificationsEnabled = settings.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, Constants.DEFAULT_NOTIFICATIONS_ENABLED);
            
            if (notificationsEnabled) {
                // Start notification service and schedule periodic updates
                TideNotificationService.startService(this);
                TideUpdateReceiver.scheduleNotificationUpdates(this);
                Log.d(TAG, "Notification system initialised and enabled");
            } else {
                Log.d(TAG, "Notification system initialised but disabled by user preference");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialise notification system", e);
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
        int menuIndex = MENU_ITEM_FIRST_PORT;
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
                // Handle port selection
                currentPort = title;
                updateRecentPorts(title);
                invalidateOptionsMenu(); // Rebuild menu with updated recent ports

                loadDataForPortAndDisplayOnScreen(currentPort);

                return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.main);

        TextView tideTextView = findViewById(R.id.tide_text_view);
        try {
            String outputString = refreshTideOutput();
            tideTextView.setText(outputString);
        } catch (CurrentTidesService.TideDataException e) {
            tideTextView.setText(e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing tide data", e);
            tideTextView.setText("Error refreshing tide data: " + e.getMessage());
        }
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
     * Display timing information for tides using TideData
     */
    private void displayTideTimings(StringBuilder outputString, TideData tideData) {
        TideRecord closestTide = tideData.currentInterval.closestTide;
        boolean isClosestInFuture = tideData.currentInterval.isClosestTideInTheFuture;
        long timeToClosest = isClosestInFuture ? tideData.currentInterval.timeToNext : tideData.currentInterval.timeToPrevious;
        
        String tideType = closestTide.isHighTide ? "HIGH tide" : "Low tide";
        String timeText = isClosestInFuture ? "in " : "";
        String agoText = isClosestInFuture ? "" : " ago";
        
        outputString.append(tideType)
                   .append(" ")
                   .append(TideFormatter.formatHourMinute(closestTide.timestamp))
                   .append(" (")
                   .append(closestTide.height)
                   .append("m) ")
                   .append(timeText)
                   .append(TideFormatter.formatDuration(timeToClosest))
                   .append(agoText)
                   .append("\n");
    }

    /**
     * Display tide records using pre-loaded upcoming tides
     */
    private void displayTideRecordsFromData(StringBuilder outputString, TideRecord[] upcomingTides) {
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
            outputString.append(TideFormatter.formatTideRecord(tide));
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
