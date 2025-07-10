package com.palliser.nztides;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ScrollView;
import android.widget.TextView;

public class NZTides extends Activity {
	
    public static final int MENU_ITEM_CHOOSE_PORT = Menu.FIRST;
    public static final int MENU_ITEM_ABOUT = Menu.FIRST+1;
    public static final String PREFS_NAME = "NZTidesPrefsFile";//file to store prefs
    private static final String PREFS_RECENT_PORTS = "RecentPorts";
    private static final int RECENT_PORTS_COUNT = 3;
    
    private String currentport;
    private String[] recentPorts = new String[0];

    final private String[] portdisplaynames = {"Akaroa", "Anakakata Bay", "Anawhata", "Auckland", "Ben Gunn Wharf", "Bluff", "Castlepoint", "Charleston", "Dargaville", "Deep Cove", "Dog Island", "Dunedin", "Elaine Bay", "Elie Bay", "Fishing Rock - Raoul Island", "Flour Cask Bay", "Fresh Water Basin", "Gisborne", "Green Island", "Halfmoon Bay - Oban", "Havelock", "Helensville", "Huruhi Harbour", "Jackson Bay", "Kaikōura", "Kaingaroa - Chatham Island", "Kaiteriteri", "Kaituna River Entrance", "Kawhia", "Korotiti Bay", "Leigh", "Long Island", "Lottin Point - Wakatiri", "Lyttelton", "Mana Marina", "Man o'War Bay", "Manu Bay", "Māpua", "Marsden Point", "Matiatia Bay", "Motuara Island", "Moturiki Island", "Napier", "Nelson", "New Brighton Pier", "North Cape - Otou", "Oamaru", "Ōkukari Bay", "Omaha Bridge", "Ōmokoroa", "Onehunga", "Opononi", "Ōpōtiki Wharf", "Opua", "Owenga - Chatham Island", "Paratutae Island", "Picton", "Port Chalmers", "Port Ōhope Wharf", "Port Taranaki", "Pouto Point", "Raglan", "Rangatira Point", "Rangitaiki River Entrance", "Richmond Bay", "Riverton - Aparima", "Scott Base", "Spit Wharf", "Sumner Head", "Tamaki River", "Tarakohe", "Tauranga", "Te Weka Bay", "Thames", "Timaru", "Town Basin", "Waihopai River Entrance", "Waitangi - Chatham Island", "Weiti River Entrance", "Welcombe Bay", "Wellington", "Westport", "Whakatāne", "Whanganui River Entrance", "Whangārei", "Whangaroa", "Whitianga", "Wilson Bay"};
	
	public static int swap (int value)
	{
	  int b1 = (value >>  0) & 0xff;
	  int b2 = (value >>  8) & 0xff;
	  int b3 = (value >> 16) & 0xff;
	  int b4 = (value >> 24) & 0xff;

	  return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

	private String formatLine(float height, boolean isHighTide, long timestamp, DecimalFormat nformat1, SimpleDateFormat dformat) {
		return nformat1.format(height) + (isHighTide ? " H " : " L ") + dformat.format(new Date(1000 * timestamp)) + '\n';
	}

	private String formatTideRecord(float height, boolean isHighTide, long timestamp, DecimalFormat nformat1, SimpleDateFormat timeFormat) {
		// Format: " HH:mm H/L height"
		return " " + timeFormat.format(new Date(1000 * timestamp)) + (isHighTide ? " H " : " L ") + nformat1.format(height) + "m\n";
	}

	private String getDayLabel(long timestamp) {
		SimpleDateFormat dayFormat = new SimpleDateFormat("E");
		return dayFormat.format(new Date(1000 * timestamp)) + "\n";
	}

	private String getMonthLabel(long timestamp) {
		SimpleDateFormat monthFormat = new SimpleDateFormat("MMM YYYY");
		return monthFormat.format(new Date(1000 * timestamp)) + "\n";
	}

	public String calc_outstring(String port){
	       
		AssetManager am = getAssets();
		StringBuffer outstring =  new StringBuffer("");
	        
		int num_rows=10;
	    int num_cols=40;
		int t = 0,told;
		float h=0;
		float hold;
		Date now = new Date();
		int nowsecs = (int)(now.getTime()/1000);
		int lasttide;
		char [][] graph = new char[num_rows][num_cols+1];


		
		
	    try {
		DecimalFormat nformat1 = new DecimalFormat(" 0.00;-0.00");
		DecimalFormat nformat2 = new DecimalFormat("0.00");
		DecimalFormat nformat3 = new DecimalFormat("00");
		DecimalFormat nformat4 = new DecimalFormat(" 0.0;-0.0");
		SimpleDateFormat dformat = new SimpleDateFormat("HH:mm E dd/MM/yy zzz");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
		DataInputStream tidedat = new DataInputStream(am.open(port+".tdat",1));


			String stationname_tofu = tidedat.readLine(); //stationname with unicode stuff ups
	       // byte[] stationnamebytes = stationname_tofu.getBytes(Charset.defaultCharset());
			//String stationname = new String(stationnamebytes, "UTF-8");
	    	//read timestamp for last tide in datafile
	    	lasttide = swap(tidedat.readInt());
	    	 
	    	//nrecs = swap(tidedat.readInt()); //Number of records in datafile
	        tidedat.readInt(); //Read number of records in datafile
	       
	    	told = swap(tidedat.readInt());
	        hold = (float) (tidedat.readByte())/(float)(10.0);

			if(told>nowsecs){
				outstring.append("The first tide in this datafile doesn't occur until ");
				outstring.append(dformat.format(new Date(1000*(long)told)));
				outstring.append(". The app should start working properly about then.");
			} else {

				//look thru tidedatfile for current time
				for (; ; ) {
					t = swap(tidedat.readInt());
					h = (float) (tidedat.readByte()) / (float) (10.0);
					if (t > nowsecs) {
						break;
					}
					told = t;
					hold = h;
				}


				//parameters of cosine wave used to interpolate between tides
				//We assume that the tides vaires cosinusoidally
				//between the last tide and the next one
				//see NZ Nautical almanac for more details,
				double omega = 2 * Math.PI / ((t - told) * 2);
				double amp = (hold - h) / 2;
				double mn = (h + hold) / 2;
				double x, phase;

				// make ascii art plot

				for (int k = 0; k < num_rows; k++) {
					for (int j = 0; j < num_cols; j++) {
						graph[k][j] = ' ';
					}
					graph[k][num_cols] = '\n';
				}

				for (int k = 0; k < num_cols; k++) {
					x = (1.0 + (hold > h ? -1 : 1) * Math.sin(k * 2 * Math.PI / (num_cols - 1))) / 2.0;
					x = ((num_rows - 1) * x + 0.5);
					graph[(int) x][k] = '*';
					//graph[k%num_rows][k]='*';
				}

				phase = omega * (nowsecs - told);
				x = (phase + Math.PI / 2) / (2.0 * Math.PI);
				x = ((num_cols - 1) * x + 0.5);
				for (int j = 0; j < num_rows; j++) {
					graph[j][(int) x] = '|';
				}


				double currentht = amp * Math.cos(omega * (nowsecs - told)) + mn;
				double riserate = -amp * omega * Math.sin(omega * (nowsecs - told)) * 60 * 60;


				//Start populating outstring
				outstring.append("[" + port + "] " + nformat4.format(currentht) + "m");
				//display up arrow or down arrow depending on weather tide is rising or falling
				if (hold < h)
					outstring.append(" \u2191");//up arrow
				else
					outstring.append(" \u2193");//down arrow

				outstring.append(nformat2.format(Math.abs(riserate * 100)) + " cm/hr\n");
				outstring.append("---------------\n");

				int time_to_previous = (nowsecs - told);
				int time_to_next = (t - nowsecs);
				boolean hightidenext = (h > hold);

				if (time_to_previous < time_to_next) {
					if (hightidenext) {
						outstring.append("Low tide (" + hold + "m) " + (int) (time_to_previous / 3600) +
								"h" + nformat3.format((int) (time_to_previous / 60) % 60) + "m ago\n");
					} else {
						outstring.append("High tide (" + hold + "m) " + (int) (time_to_previous / 3600) +
								"h" + nformat3.format((int) (time_to_previous / 60) % 60) + "m ago\n");
					}
				} else {
					if (hightidenext) {
						outstring.append("High tide (" + h + "m) in " + (int) (time_to_next / 3600) +
								"h" + nformat3.format((int) (time_to_next / 60) % 60) + "m\n");
					} else {
						outstring.append("Low tide (" + h + "m) in " + (int) (time_to_next / 3600) +
								"h" + nformat3.format((int) (time_to_next / 60) % 60) + "m\n");
					}

				}
				//outstring.append("---------------\n");
				//int num_minutes=(int)((nowsecs-told)/(60));
				//outstring.append("Last tide " + hold + "m,    "+num_minutes/60  + "h" +nformat3.format(num_minutes%60) +"m ago\n");
				//num_minutes=(int)((t -nowsecs)/(60));
				//outstring.append("Next tide " + h + "m, in " +num_minutes/60  + "h" +nformat3.format(num_minutes%60) +"m\n");
				//outstring.append("---------------\n");
				outstring.append("\n");

				for (int k = 0; k < num_rows; k++) {
					for (int j = 0; j < num_cols + 1; j++) {
						outstring.append(graph[k][j]);
					}
				}

				//outstring.append("---------------\n");
				outstring.append("\n");

				// Display tide records grouped by day
				String lastDay = "";
				long currentTimestamp = told;
				float currentHeight = hold;
				boolean currentIsHigh = !hightidenext;
				String lastMonth = getMonthLabel(currentTimestamp);

				// First tide record
				String dayLabel = getDayLabel(currentTimestamp);
				if (!dayLabel.equals(lastDay)) {
					outstring.append(dayLabel);
					lastDay = dayLabel;
				}
				outstring.append(formatTideRecord(currentHeight, currentIsHigh, currentTimestamp, nformat1, timeFormat));

				// Second tide record  
				currentTimestamp = t;
				currentHeight = h;
				currentIsHigh = hightidenext;
				dayLabel = getDayLabel(currentTimestamp);
				String monthLabel = getMonthLabel(currentTimestamp);

				if (!dayLabel.equals(lastDay)) {
					outstring.append(dayLabel);
					lastDay = dayLabel;
					if(!monthLabel.equals(lastMonth)) {
						outstring.append(monthLabel);
						lastMonth = monthLabel;
					}
				}
				outstring.append(formatTideRecord(currentHeight, currentIsHigh, currentTimestamp, nformat1, timeFormat));

				// Remaining tide records
				for (int k = 0; k < 35 * 4; k++) {
					currentIsHigh = !currentIsHigh;
					currentTimestamp = swap(tidedat.readInt());
					currentHeight = (float) (tidedat.readByte()) / (float) (10.0);
					
					dayLabel = getDayLabel(currentTimestamp);
					if (!dayLabel.equals(lastDay)) {
						outstring.append(dayLabel);
						lastDay = dayLabel;

						monthLabel = getMonthLabel(currentTimestamp);
						if(!monthLabel.equals(lastMonth)) {
							outstring.append(monthLabel);
							lastMonth = monthLabel;
						}
					}
					outstring.append(formatTideRecord(currentHeight, currentIsHigh, currentTimestamp, nformat1, timeFormat));
				}
				outstring.append("The last tide in this datafile occurs at:\n");
				outstring.append(dformat.format(new Date(1000 * (long) lasttide)));
			}
	            
	        }catch (IOException e) {
	        	outstring.append("Problem reading tide data\n\n Try selecting the port again, some times the ports available change with and upgrade. If this doesn't work it is either because the tide data is out of date or you've found some bug, try looking for an update.");
	        }
	        return outstring.toString();
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //restore current port from settings file
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        currentport = settings.getString("CurrentPort","Auckland" );
        loadRecentPorts();
        
        // If no recent ports, add the current port
        if (recentPorts.length == 0) {
            updateRecentPorts(currentport);
        }
        
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
        java.util.LinkedList<String> list = new java.util.LinkedList<>();
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
        java.util.HashSet<String> recentSet = new java.util.HashSet<>();
        for (String p : recentPorts) recentSet.add(p);
        for (int k = 0; k < portdisplaynames.length; k++) {
            if (!recentSet.contains(portdisplaynames[k])) {
                menu.add(0, menuIndex++, 0, portdisplaynames[k]);
            }
        }
        menu.add(0, MENU_ITEM_ABOUT, 0, "About");
        return true;
    }

    public boolean  onOptionsItemSelected  (MenuItem  item){
        int id = item.getItemId();
        String title = (String) item.getTitle();
        // Check if selected port is in recentPorts or portdisplaynames
        for (String port : portdisplaynames) {
            if (port.equals(title)) {
                currentport = port;
                updateRecentPorts(port);
                invalidateOptionsMenu(); // Rebuild menu with updated recent ports
                this.onResume();
                return true;
            }
        }
        for (String port : recentPorts) {
            if (port.equals(title)) {
                currentport = port;
                updateRecentPorts(port);
                invalidateOptionsMenu(); // Rebuild menu with updated recent ports
                this.onResume();
                return true;
            }
        }
    	switch (id) {
    	  case MENU_ITEM_ABOUT:
    		TextView tv = new TextView(this);
    		//tv.setTypeface(Typeface.MONOSPACE);
    		tv.setText(R.string.AboutString);//+now.format2445());
    		ScrollView sv = new ScrollView(this);
    		sv.addView(tv);
    		setContentView(sv);   
    	    //quit();
    	    return true;
    	  default:
    	    return super.onOptionsItemSelected(item);
    	  }
    	}
    
    @Override
    protected void onResume(){
        String outstring = calc_outstring(currentport);
        TextView tv = new TextView(this);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setText(outstring);//+now.format2445());
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        setContentView(sv);   
    	super.onResume();
    }

    @Override
    protected void onStop(){
       super.onStop();
    
      // Save user preferences. We need an Editor object to
      // make changes. All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString("CurrentPort", currentport);
      editor.putString(PREFS_RECENT_PORTS, String.join(",", recentPorts));
      editor.commit();
    }


}
