package com.palliser.nztides;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Locale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

public class NZTides extends Activity {
	
    public static final int MENU_ITEM_CHOOSE_PORT = Menu.FIRST;
    public static final int MENU_ITEM_ABOUT = Menu.FIRST+1;
    public static final String PREFS_NAME = "NZTidesPrefsFile";//file to store prefs
    private static final String PREFS_RECENT_PORTS = "RecentPorts";
    private static final int RECENT_PORTS_COUNT = 3;
    
    private String currentPort;
    private String[] recentPorts = new String[0];

    private static final String[] PORT_DISPLAY_NAMES = {"Akaroa", "Anakakata Bay", "Anawhata", "Auckland", "Ben Gunn Wharf", "Bluff", "Castlepoint", "Charleston", "Dargaville", "Deep Cove", "Dog Island", "Dunedin", "Elaine Bay", "Elie Bay", "Fishing Rock - Raoul Island", "Flour Cask Bay", "Fresh Water Basin", "Gisborne", "Green Island", "Halfmoon Bay - Oban", "Havelock", "Helensville", "Huruhi Harbour", "Jackson Bay", "Kaikōura", "Kaingaroa - Chatham Island", "Kaiteriteri", "Kaituna River Entrance", "Kawhia", "Korotiti Bay", "Leigh", "Long Island", "Lottin Point - Wakatiri", "Lyttelton", "Mana Marina", "Man o'War Bay", "Manu Bay", "Māpua", "Marsden Point", "Matiatia Bay", "Motuara Island", "Moturiki Island", "Napier", "Nelson", "New Brighton Pier", "North Cape - Otou", "Oamaru", "Ōkukari Bay", "Omaha Bridge", "Ōmokoroa", "Onehunga", "Opononi", "Ōpōtiki Wharf", "Opua", "Owenga - Chatham Island", "Paratutae Island", "Picton", "Port Chalmers", "Port Ōhope Wharf", "Port Taranaki", "Pouto Point", "Raglan", "Rangatira Point", "Rangitaiki River Entrance", "Richmond Bay", "Riverton - Aparima", "Scott Base", "Spit Wharf", "Sumner Head", "Tamaki River", "Tarakohe", "Tauranga", "Te Weka Bay", "Thames", "Timaru", "Town Basin", "Waihopai River Entrance", "Waitangi - Chatham Island", "Weiti River Entrance", "Welcombe Bay", "Wellington", "Westport", "Whakatāne", "Whanganui River Entrance", "Whangārei", "Whangaroa", "Whitianga", "Wilson Bay"};
	
	public static int swapBytes(int value) {
		int b1 = (value >>  0) & 0xff;
		int b2 = (value >>  8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

	private String formatLine(float height, boolean isHighTide, long timestamp, DecimalFormat nformat1, SimpleDateFormat dformat) {
		return nformat1.format(height) + (isHighTide ? " H " : " L ") + dformat.format(new Date(1000 * timestamp)) + '\n';
	}

	private String formatTideRecord(float height, boolean isHighTide, long timestamp, DecimalFormat heightFormat, SimpleDateFormat timeFormat) {
		// Format: " HH:mm H/L height"
		return " " + timeFormat.format(new Date(1000 * timestamp)) + (isHighTide ? " H " : " L ") + heightFormat.format(height) + "m\n";
	}

	private String getDayLabel(long timestamp) {
		SimpleDateFormat dayFormat = new SimpleDateFormat("E dd", Locale.getDefault());
		return dayFormat.format(new Date(1000 * timestamp));
	}

	private String getMonthLabel(long timestamp) {
		SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
		return monthFormat.format(new Date(1000 * timestamp));
	}

	/**
	 * Calculate current tide height and rate using cosine interpolation
	 */
	private TideCalculation calculateCurrentTide(int currentTimeSeconds, TideData previousTide, TideData nextTide) {
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

	// Constants for ASCII art graph
	private static final int GRAPH_ROWS = 10;
	private static final int GRAPH_COLS = 40;
	private static final int RECORDS_TO_DISPLAY = 35 * 4; // About 35 days of tides
	
	public String calculateTideOutput(String port) {
		AssetManager assetManager = getAssets();
		StringBuilder outputString = new StringBuilder();
		
		int nextTideTime = 0, previousTideTime;
		float nextTideHeight = 0;
		float previousTideHeight;
		Date currentTime = new Date();
		int currentTimeSeconds = (int)(currentTime.getTime() / 1000);
		int lastTideInFile;
		char[][] tideGraph = new char[GRAPH_ROWS][GRAPH_COLS + 1];


		
		
		try (DataInputStream tideDataStream = new DataInputStream(assetManager.open(port + ".tdat", 1))) {
			// Read station name (currently not used due to encoding issues)
			String stationNameRaw = tideDataStream.readLine();
			
			// Read timestamp for last tide in datafile
			lastTideInFile = swapBytes(tideDataStream.readInt());
			
			// Skip number of records in datafile
			tideDataStream.readInt();
			
			previousTideTime = swapBytes(tideDataStream.readInt());
			previousTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
			
		if (previousTideTime > currentTimeSeconds) {
			outputString.append("The first tide in this datafile doesn't occur until ");
			outputString.append(TideFormatter.formatFullDate(previousTideTime));
			outputString.append(". The app should start working properly about then.");
		} else {

				// Look through tide data file for current time
				while (true) {
					nextTideTime = swapBytes(tideDataStream.readInt());
					nextTideHeight = (float) (tideDataStream.readByte()) / 10.0f;
					if (nextTideTime > currentTimeSeconds) {
						break;
					}
					previousTideTime = nextTideTime;
					previousTideHeight = nextTideHeight;
				}

				// Create TideData objects for calculation
				TideData previousTide = new TideData(previousTideTime, previousTideHeight, false); // tide type determined later
				TideData nextTide = new TideData(nextTideTime, nextTideHeight, false); // tide type determined later

				// Parameters of cosine wave used to interpolate between tides
				// We assume that the tides vary cosinusoidally
				// between the last tide and the next one
				// See NZ Nautical almanac for more details
				double omega = 2 * Math.PI / ((nextTideTime - previousTideTime) * 2);
				double amplitude = (previousTideHeight - nextTideHeight) / 2;
				double mean = (nextTideHeight + previousTideHeight) / 2;
				double x, phase;

				// Create ASCII art plot
				initialiseGraph(tideGraph);
				createTideGraph(tideGraph, previousTideHeight, nextTideHeight);
				
				// Mark current time position on graph
				phase = omega * (currentTimeSeconds - previousTideTime);
				x = (phase + Math.PI / 2) / (2.0 * Math.PI);
				x = ((GRAPH_COLS - 1) * x + 0.5);
				for (int j = 0; j < GRAPH_ROWS; j++) {
					tideGraph[j][(int) x] = '|';
				}


				TideCalculation currentTideCalc = calculateCurrentTide(currentTimeSeconds, previousTide, nextTide);

				// Start populating output string
				DecimalFormat currentHeightFormat = new DecimalFormat(" 0.0;-0.0");
				outputString.append("[" + port + "] " + currentHeightFormat.format(currentTideCalc.height) + "m");
				
				// Display up arrow or down arrow depending on whether tide is rising or falling
				if (previousTideHeight < nextTideHeight)
					outputString.append(" \u2191"); // up arrow
				else
					outputString.append(" \u2193"); // down arrow

				DecimalFormat riseRateFormat = new DecimalFormat("0");
				outputString.append(riseRateFormat.format(Math.abs(currentTideCalc.riseRate * 100)) + " cm/hr\n");
				outputString.append("---------------\n");

				displayTideTimings(outputString, currentTimeSeconds, previousTideTime, nextTideTime, previousTideHeight, nextTideHeight);
				outputString.append("\n");

				// Display ASCII tide graph
				for (int row = 0; row < GRAPH_ROWS; row++) {
					for (int col = 0; col < GRAPH_COLS + 1; col++) {
						outputString.append(tideGraph[row][col]);
					}
				}
				outputString.append("\n");			// Display tide records grouped by day
			displayTideRecords(outputString, tideDataStream, previousTideTime, previousTideHeight, nextTideTime, nextTideHeight);
			outputString.append("The last tide in this datafile occurs at:\n");
			outputString.append(TideFormatter.formatFullDate(lastTideInFile));
		}
	            
		} catch (IOException e) {
			outputString.append("Problem reading tide data\n\nTry selecting the port again, sometimes the ports available change with an upgrade. If this doesn't work it is either because the tide data is out of date or you've found some bug, try looking for an update.");
		}
		return outputString.toString();
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore current port from settings file
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        currentPort = settings.getString("CurrentPort", "Auckland");
        loadRecentPorts();
        
        // If no recent ports, add the current port
        if (recentPorts.length == 0) {
            updateRecentPorts(currentPort);
        }
        
        setContentView(R.layout.main);
        
    //    setContentView(R.layout.main);
    }
    
    private void loadRecentPorts() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String recent = settings.getString(PREFS_RECENT_PORTS, "");
        if (!recent.isEmpty()) {
            recentPorts = recent.split(",");
        } else {
            recentPorts = new String[0];
        }
    }

    private void saveRecentPorts() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFS_RECENT_PORTS, String.join(",", recentPorts));
        editor.commit();
    }

    private void updateRecentPorts(String port) {
        LinkedList<String> list = new LinkedList<>();
        list.add(port);
        for (String p : recentPorts) {
            if (!p.equals(port)) list.add(p);
        }
        while (list.size() > RECENT_PORTS_COUNT) list.removeLast();
        recentPorts = list.toArray(new String[0]);
        saveRecentPorts();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        int menuIndex = Menu.FIRST;
        if (recentPorts.length > 0) {
            for (int i = 0; i < recentPorts.length; i++) {
                menu.add(0, menuIndex++, 0, recentPorts[i]);
            }
            // Add a disabled separator instead of a submenu
            MenuItem sep = menu.add(0, menuIndex++, 0, "──────────");
            sep.setEnabled(false);
        }
        HashSet<String> recentSet = new HashSet<>();
        for (String p : recentPorts) recentSet.add(p);
        for (int k = 0; k < PORT_DISPLAY_NAMES.length; k++) {
            if (!recentSet.contains(PORT_DISPLAY_NAMES[k])) {
                menu.add(0, menuIndex++, 0, PORT_DISPLAY_NAMES[k]);
            }
        }
        menu.add(0, MENU_ITEM_ABOUT, 0, "About");
        return true;
    }

    public boolean  onOptionsItemSelected  (MenuItem  item){
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
    protected void onStop(){
       super.onStop();
    
        // Save user preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("CurrentPort", currentPort);
        editor.putString(PREFS_RECENT_PORTS, String.join(",", recentPorts));
        editor.commit();
    }

    private void initialiseGraph(char[][] graph) {
		for (int row = 0; row < GRAPH_ROWS; row++) {
			for (int col = 0; col < GRAPH_COLS; col++) {
				graph[row][col] = ' ';
			}
			graph[row][GRAPH_COLS] = '\n';
		}
	}
	
	private void createTideGraph(char[][] graph, float previousHeight, float nextHeight) {
		for (int k = 0; k < GRAPH_COLS; k++) {
			double x = (1.0 + (previousHeight > nextHeight ? -1 : 1) * 
					   Math.sin(k * 2 * Math.PI / (GRAPH_COLS - 1))) / 2.0;
			x = ((GRAPH_ROWS - 1) * x + 0.5);
			graph[(int) x][k] = '*';
		}
	}

	/**
	 * Display timing information for tides
	 */
	private void displayTideTimings(StringBuilder outputString, int currentTimeSeconds, 
								   int previousTideTime, int nextTideTime, 
								   float previousTideHeight, float nextTideHeight) {
		int timeToPrevious = (currentTimeSeconds - previousTideTime);
		int timeToNext = (nextTideTime - currentTimeSeconds);
		boolean isHighTideNext = (nextTideHeight > previousTideHeight);

		if (timeToPrevious < timeToNext) {
			if (isHighTideNext) {
				outputString.append("Low tide (" + previousTideHeight + "m) " + TideFormatter.formatDuration(timeToPrevious) + " ago\n");
			} else {
				outputString.append("High tide (" + previousTideHeight + "m) " + TideFormatter.formatDuration(timeToPrevious) + " ago\n");
			}
		} else {
			if (isHighTideNext) {
				outputString.append("High tide (" + nextTideHeight + "m) in " + TideFormatter.formatDuration(timeToNext) + "\n");
			} else {
				outputString.append("Low tide (" + nextTideHeight + "m) in " + TideFormatter.formatDuration(timeToNext) + "\n");
			}
		}
	}
	
	/**
	 * Display tide records grouped by day
	 */
	private void displayTideRecords(StringBuilder outputString, DataInputStream tideDataStream,
								   int previousTideTime, float previousTideHeight,
								   int nextTideTime, float nextTideHeight) throws IOException {
		String lastDay = "";
		
		// Create first two tide records
		boolean firstIsHigh = !(nextTideHeight > previousTideHeight);
		TideData firstTide = new TideData(previousTideTime, previousTideHeight, firstIsHigh);
		TideData secondTide = new TideData(nextTideTime, nextTideHeight, !firstIsHigh);
		
		String lastMonth = TideFormatter.formatMonth(firstTide.getTimestamp());

		// First tide record
		String dayLabel = TideFormatter.formatDay(firstTide.getTimestamp());
		if (!dayLabel.equals(lastDay)) {
			outputString.append(dayLabel + "\n");
			lastDay = dayLabel;
		}
		outputString.append(TideFormatter.formatTideRecord(firstTide));

		// Second tide record  
		dayLabel = TideFormatter.formatDay(secondTide.getTimestamp());
		String monthLabel = TideFormatter.formatMonth(secondTide.getTimestamp());

		if (!dayLabel.equals(lastDay)) {
			lastDay = dayLabel;
			monthLabel = TideFormatter.formatMonth(secondTide.getTimestamp());
			if (!monthLabel.equals(lastMonth)) {
				outputString.append("\n ==== " + dayLabel + " " + monthLabel + " ====\n");
				lastMonth = monthLabel;
			} else {
				outputString.append(dayLabel + "\n");
			}
		}
		outputString.append(TideFormatter.formatTideRecord(secondTide));

		// Remaining tide records
		boolean currentIsHigh = secondTide.isHighTide();
		for (int k = 0; k < RECORDS_TO_DISPLAY; k++) {
			currentIsHigh = !currentIsHigh;
			TideData currentTide = TideDataReader.readFromStream(tideDataStream).withTideType(currentIsHigh);
			
			dayLabel = TideFormatter.formatDay(currentTide.getTimestamp());
			if (!dayLabel.equals(lastDay)) {
				lastDay = dayLabel;
				monthLabel = TideFormatter.formatMonth(currentTide.getTimestamp());
				if (!monthLabel.equals(lastMonth)) {
					outputString.append("\n ==== " + dayLabel + " " + monthLabel + " ====\n");
					lastMonth = monthLabel;
				} else {
					outputString.append(dayLabel + "\n");
				}
			}
			outputString.append(TideFormatter.formatTideRecord(currentTide));
		}
	}
}
