package com.example.myapplication;

import static com.example.myapplication.MainActivity.KEY_CHRONOMETER_FLAG;
import static com.example.myapplication.MainActivity.KEY_CHRONOMETER_START;
import static com.example.myapplication.MainActivity.KEY_START_TIME;
import static com.example.myapplication.MainActivity.downTime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
        handler.postDelayed(runnable, 1000);
        getFormattedTime(elapsedTime);
        if (logEnabled)
            Log.d("STOPWATCH", elapsedTime / 1000 + " seconds, " + elapsedTime % 1000 + " milliseconds");
    }

    public Notification getNotification(StringBuilder message1) {

        Intent intentSelf = new Intent(this, StopwatchNotificationService.class);
        intentSelf.setAction(ACTION_START_PAUSE);
        String message2, message3;
        int icon;
        if (sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false)) {
            message2 = "start";
            message3 = "pause";
            icon = R.drawable.pause;
        } else {
            message2 = "pause";
            message3 = "start";
            icon = R.drawable.ic_play;
        }
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intentSelf, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "channel_01")
                .addAction(icon, message3, servicePendingIntent)
                .setContentIntent(activityPendingIntent)
                .setContentTitle(message2)
                .setContentText(message1)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.logo)
                .setWhen(System.currentTimeMillis())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message1))
                .setTimeoutAfter(1000)
                .build();

        notificationManager.notify(R.id.stopwatch_notification_service, notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel("channel_01", name, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        return notification;
    }

    protected void handleStartPauseAction(Intent intent) throws InterruptedException {
        boolean start = sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false);
        if (start) {
            handlePauseAction(intent);
        } else {
            handleResumeAction(intent);
        }
    }

    private void handlePauseAction(Intent intent) throws InterruptedException {
        editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CHRONOMETER_START, false);
        if (intent != null)
            editor.putString(KEY_CHRONOMETER_FLAG, "pause");
        editor.commit();
        pause();
        Thread.sleep(500);
        getFormattedTime(elapsedTime);
    }

    private void handleResumeAction(Intent intent) {
        editor = sharedPreferences.edit();
        editor.putBoolean(KEY_CHRONOMETER_START, true);
        if (intent != null)
            editor.putString(KEY_CHRONOMETER_FLAG, "resume");
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
                            handleStartPauseAction(intent);
                            break;
                        case ACTION_RESUME:
                            handleResumeAction(null);
                            break;
                        case ACTION_PAUSE:
                            handlePauseAction(null);
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                getFormattedTime(elapsedTime);

                if (downTime > 0) {
                    editor = sharedPreferences.edit();
                    editor.putLong(KEY_START_TIME, 0);
                    editor.putBoolean(KEY_CHRONOMETER_START, false);
                    editor.commit();
                    elapsedTime = 0;
                    getFormattedTime(elapsedTime);
                }
                long startTime = sharedPreferences.getLong(KEY_START_TIME, 0);

                if (startTime > 0 && sharedPreferences.getBoolean(KEY_CHRONOMETER_START, false)) {
                    setBase(startTime);
                }
            }
        }
        Log.d(TAG, "onStartCommand(Intent intent, int flags, int startId)");
        return super.onStartCommand(intent, flags, startId);
    }
}