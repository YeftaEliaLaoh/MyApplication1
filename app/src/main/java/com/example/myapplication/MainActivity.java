package com.example.myapplication;

import static com.example.myapplication.StopwatchNotificationService.ACTION_UI_UPDATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding activityMainBinding;
    ImageButton startButton;

    TextView hourTextView;
    TextView minuteTextView;
    TextView secondsTextView;
    StopwatchNotificationService stopwatchNotificationService;

    Intent serviceIntent;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_DOWN_TIME = "down_time";
    public static final String KEY_CHRONOMETER_START = "chronometer_start";

    private String TAG = "StopwatchMainActivity";
    private boolean flag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        sharedPreferences.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
        stopwatchNotificationService = new StopwatchNotificationService();
        registerReceiver(uiUpdated, new IntentFilter(ACTION_UI_UPDATE));
        bindViews();
        Log.d(TAG, "onCreate(@Nullable Bundle savedInstanceState)");
    }

    private void bindViews() {
        startButton = findViewById(R.id.startButton);
        hourTextView = findViewById(R.id.hour);
        minuteTextView = findViewById(R.id.minute);
        secondsTextView = findViewById(R.id.seconds);

        serviceIntent = new Intent(this, StopwatchNotificationService.class);
        startService(serviceIntent);
        startButton.setOnClickListener(view -> {
            serviceIntent.setAction(StopwatchNotificationService.ACTION_START_PAUSE);
            startService(serviceIntent);
        });
    }

    private void syncFabIconWithStopwatchState(boolean running) {
        startButton.setImageResource(running ? R.drawable.pause : R.drawable.ic_play);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean start = sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false);
            syncFabIconWithStopwatchState(start);
        }
    };

    private final BroadcastReceiver uiUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            secondsTextView.setText(intent.getExtras().getString("secondTextView"));
            minuteTextView.setText(intent.getExtras().getString("minuteTextView"));
            hourTextView.setText(intent.getExtras().getString("hourTextView"));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editor = sharedPreferences.edit();
        editor.putLong(KEY_DOWN_TIME, System.currentTimeMillis());
        editor.commit();
        Log.d(TAG, "onDestroy()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        serviceIntent.setAction(StopwatchNotificationService.ACTION_PAUSE);
        startService(serviceIntent);
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean start = sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false);
        if (flag && start) {
            serviceIntent.setAction(StopwatchNotificationService.ACTION_RESUME);
            startService(serviceIntent);
        }
        flag = true;
        Log.d(TAG, "onResume()");
    }
}