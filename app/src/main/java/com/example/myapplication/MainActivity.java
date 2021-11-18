package com.example.myapplication;

import android.os.Bundle;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        stopwatch.setTextView(seconds);
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
                    seconds.setText(null);
                } else if (stopwatch.isPaused())
                    stopwatch.resume();
            }
        });
    }
}