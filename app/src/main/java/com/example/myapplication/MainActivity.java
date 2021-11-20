package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding activityMainBinding;
    ImageButton startButton;
    TextView hour;
    TextView minute;
    TextView seconds;
    Boolean flag = false;
    Stopwatch stopwatch;

    private SharedPreferences mPrefs;

    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_PAUSE_TIME = "pause_time";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());
        stopwatch = new Stopwatch();
        bindViews();
    }

    private void bindViews() {
        startButton = findViewById(R.id.startButton);
        hour = findViewById(R.id.hour);
        minute = findViewById(R.id.minute);
        seconds = findViewById(R.id.seconds);

        stopwatch.setTextView(seconds, minute, hour);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
        startButton.setOnClickListener(view -> {
            if (flag) {
                startButton.setImageResource(R.drawable.ic_play);
                flag = false;
                if (stopwatch.isStarted() && !stopwatch.isPaused())
                    stopwatch.pause();
            } else {
                startButton.setImageResource(R.drawable.pause);
                flag = true;
                if (!stopwatch.isStarted()) {
                    stopwatch.start();
                } else if (stopwatch.isPaused())
                    stopwatch.resume();
            }
        });
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener = (sharedPreferences, key) -> {
        long startTime = sharedPreferences.getLong(KEY_START_TIME, 0);
        long pauseTime = sharedPreferences.getLong(KEY_PAUSE_TIME, 0);
        if (startTime == 0) {
            startTime = SystemClock.elapsedRealtime();
        }
        stopwatch = new Stopwatch(startTime);

    };
}