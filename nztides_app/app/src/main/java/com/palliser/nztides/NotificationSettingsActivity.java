package com.palliser.nztides;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Button;

/**
 * Simple activity for managing notification preferences
 */
public class NotificationSettingsActivity extends Activity {
    
    private CheckBox enableNotificationsCheckbox;
    private RadioGroup updateFrequencyGroup;
    private RadioButton freq5min, freq10min, freq15min, freq30min;
    private TextView statusText;
    private Button saveButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_settings);
        
        initializeViews();
        loadSettings();
        setupListeners();
    }
    
    private void initializeViews() {
        enableNotificationsCheckbox = findViewById(R.id.enable_notifications);
        updateFrequencyGroup = findViewById(R.id.update_frequency_group);
        freq5min = findViewById(R.id.freq_5min);
        freq10min = findViewById(R.id.freq_10min);
        freq15min = findViewById(R.id.freq_15min);
        freq30min = findViewById(R.id.freq_30min);
        statusText = findViewById(R.id.status_text);
        saveButton = findViewById(R.id.save_button);
    }
    
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        
        boolean notificationsEnabled = prefs.getBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, false);
        enableNotificationsCheckbox.setChecked(notificationsEnabled);
        
        int updateFrequency = prefs.getInt(Constants.PREFS_UPDATE_FREQUENCY, Constants.DEFAULT_UPDATE_INTERVAL_MINUTES);
        
        switch (updateFrequency) {
            case 5:
                freq5min.setChecked(true);
                break;
            case 10:
                freq10min.setChecked(true);
                break;
            case 15:
                freq15min.setChecked(true);
                break;
            case 30:
                freq30min.setChecked(true);
                break;
            default:
                freq10min.setChecked(true);
        }
        
        updateFrequencyGroup.setEnabled(notificationsEnabled);
        updateStatus();
    }
    
    private void setupListeners() {
        enableNotificationsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateFrequencyGroup.setEnabled(isChecked);
            updateStatus();
        });
        
        saveButton.setOnClickListener(v -> saveSettings());
    }
    
    private void updateStatus() {
        NotificationChannelManager channelManager = new NotificationChannelManager(this);
        
        if (channelManager.doesNotHaveNotificationPermission()) {
            statusText.setText(getString(R.string.error_notification_permission));
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if (enableNotificationsCheckbox.isChecked()) {
            statusText.setText("Notifications will be enabled");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusText.setText("Notifications are disabled");
            statusText.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        }
    }
    
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        boolean notificationsEnabled = enableNotificationsCheckbox.isChecked();
        editor.putBoolean(Constants.PREFS_NOTIFICATIONS_ENABLED, notificationsEnabled);
        
        int selectedFrequency = getSelectedFrequency();
        editor.putInt(Constants.PREFS_UPDATE_FREQUENCY, selectedFrequency);
        
        editor.apply();
        
        // Apply changes
        if (notificationsEnabled) {
            TideNotificationService.startService(this);
            TideUpdateReceiver.scheduleNotificationUpdates(this);
        } else {
            TideNotificationService.stopService(this);
            TideUpdateReceiver.cancelNotificationUpdates(this);
        }
        
        // Close activity
        finish();
    }
    
    private int getSelectedFrequency() {
        if (freq5min.isChecked()) return 5;
        if (freq10min.isChecked()) return 10;
        if (freq15min.isChecked()) return 15;
        if (freq30min.isChecked()) return 30;
        return Constants.DEFAULT_UPDATE_INTERVAL_MINUTES;
    }
}
