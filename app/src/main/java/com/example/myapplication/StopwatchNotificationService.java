package com.example.myapplication;

import static com.example.myapplication.MainActivity.KEY_CHRONOMETER_START;
import static com.example.myapplication.MainActivity.KEY_DOWN_TIME;
import static com.example.myapplication.MainActivity.KEY_START_TIME;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class StopwatchNotificationService extends Service {

    public static final String ACTION_START_PAUSE = "com.example.myapplication.timers.action.START_PAUSE";
    public static final String ACTION_RESUME = "com.example.myapplication.timers.action.START";
    public static final String ACTION_PAUSE = "com.example.myapplication.timers.action.PAUSE";
    public static final String ACTION_UI_UPDATE = "com.example.myapplication.timers.action.UI_UPDATE";

    private String TAG = "StopwatchNotificationService";

    private long current, elapsedTime;
    private boolean logEnabled;
    private Handler handler;

    private final Runnable runnable = this::run;

    private SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public void setBase(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public StopwatchNotificationService() {
    }

    void getFormattedTime(long elapsedTime) {
        int seconds = (int) ((elapsedTime / 1000) % 60);
        int minutes = (int) (elapsedTime / (60 * 1000) % 60);
        int hours = (int) (elapsedTime / (60 * 60 * 1000));

        Intent i = new Intent(ACTION_UI_UPDATE);
        if (String.valueOf(seconds).length() < 2) {
            i.putExtra("secondTextView", "0" + seconds);
        } else {
            i.putExtra("secondTextView", String.valueOf(seconds));
        }
        if (String.valueOf(minutes).length() < 2) {
            i.putExtra("minuteTextView", "0" + minutes);
        } else {
            i.putExtra("minuteTextView", String.valueOf(minutes));
        }
        if (String.valueOf(hours).length() < 2) {
            i.putExtra("hourTextView", "0" + hours);
        } else {
            i.putExtra("hourTextView", String.valueOf(minutes));
        }
        sendBroadcast(i);

        final StringBuilder displayTime = new StringBuilder();
        NumberFormat f = new DecimalFormat("00");

        displayTime.append(hours).append(":").append(f.format(minutes)).append(":").append(f.format(seconds));
        startForeground(R.id.stopwatch_notification_service, getNotification(displayTime));
    }

    public void pause() {
        handler.removeCallbacks(runnable);
    }

    public void resume() {
        current = System.currentTimeMillis();
        handler.post(runnable);
    }

    /**
     * Updates the time in elapsed and lap time and then updates the current time.
     *
     * @param time Current time in millis. Passing any other value may result in odd behaviour
     * @since 1.1
     */
    private void updateElapsed(long time) {
        elapsedTime += time - current;
        editor = sharedPreferences.edit();
        editor.putLong(KEY_START_TIME, elapsedTime);
        editor.commit();
        current = time;
    }

    /**
     * The main thread responsible for updating and displaying the time
     *
     * @since 1.1
     */
    private void run() {
        updateElapsed(System.currentTimeMillis());
        handler.postDelayed(runnable, 0);
        getFormattedTime(elapsedTime);
        if (logEnabled)
            Log.d("STOPWATCH", elapsedTime / 1000 + " seconds, " + elapsedTime % 1000 + " milliseconds");
    }

    public Notification getNotification(StringBuilder message1) {
        String message2 = sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false) ? "start" : "pause";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "channel_01")
                .setContentTitle(message2)
                .setContentText(message1)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.logo)
                .setWhen(System.currentTimeMillis())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message1))
                .build();

        notificationManager.notify(R.id.stopwatch_notification_service, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel("channel_01", name, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        return notification;
    }

    protected void handleStartPauseAction() throws InterruptedException {
        boolean start = sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false);
        editor = sharedPreferences.edit();
        if (start) {
            handlePauseAction();
        } else {
            handleResumeAction();
        }
    }

    private void handlePauseAction() throws InterruptedException {
        editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CHRONOMETER_START, false);
        editor.commit();
        pause();
        Thread.sleep(500);
        getFormattedTime(elapsedTime);
    }

    private void handleResumeAction() {
        editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CHRONOMETER_START, true);
        editor.commit();
        resume();
    }

    @Override
    public void onCreate() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        handler = new Handler(Looper.getMainLooper());
        logEnabled = false;

        Log.d(TAG, "onCreate()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind(Intent intent)");
        return null;
    }

    @CallSuper
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action != null) {
                try {
                    switch (action) {
                        case ACTION_START_PAUSE:
                            handleStartPauseAction();
                            break;
                        case ACTION_RESUME:
                            handleResumeAction();
                            break;
                        case ACTION_PAUSE:
                            handlePauseAction();
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                getFormattedTime(elapsedTime);
                Log.d(TAG, "onStartCommand: elapsedTime getFormattedTime "+elapsedTime);

                if ((System.currentTimeMillis() - sharedPreferences.getLong(KEY_DOWN_TIME, 0)) / (60 * 1000) % 60 > 1) {
                    editor = sharedPreferences.edit();
                    editor.putLong(KEY_START_TIME, 0);
                    editor.putBoolean(KEY_CHRONOMETER_START, false);
                    editor.commit();
                    elapsedTime = 0;
                    getFormattedTime(elapsedTime);
                    Log.d(TAG, "onStartCommand: elapsedTime currentTimeMillis "+elapsedTime);

                }
                long startTime = sharedPreferences.getLong(KEY_START_TIME, 0);
                Log.d(TAG, "onStartCommand: elapsedTime "+startTime);

                if (startTime > 0) {
                    setBase(startTime);
                    editor = sharedPreferences.edit();
                    editor.putBoolean(KEY_CHRONOMETER_START, true);
                    editor.commit();
                    resume();
                    Log.d(TAG, "onStartCommand: resume "+startTime);

                }
            }
        }
        Log.d(TAG, "onStartCommand(Intent intent, int flags, int startId)");
        return super.onStartCommand(intent, flags, startId);
    }
}