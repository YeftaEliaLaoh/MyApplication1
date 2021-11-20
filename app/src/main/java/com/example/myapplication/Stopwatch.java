package com.example.myapplication;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class Stopwatch {

    private TextView secondTextView;
    private TextView minuteTextView;
    private TextView hourTextView;

    private long start, current, elapsedTime;
    private boolean started, paused, logEnabled;
    private long clockDelay = 0;
    private Handler handler;

    private final Runnable runnable = this::run;

    public Stopwatch(long startTime) {
        start = System.currentTimeMillis();
        current = System.currentTimeMillis();
        elapsedTime = startTime;
        started = false;
        paused = false;
        handler = new Handler();
    }

    public Stopwatch() {
        start = System.currentTimeMillis();
        current = System.currentTimeMillis();
        elapsedTime = 0;
        started = false;
        paused = false;
        handler = new Handler();
    }

    void getFormattedTime(long elapsedTime) {
        //int milliseconds = (int) ((elapsedTime % 1000) / 10);
        int seconds = (int) ((elapsedTime / 1000) % 60);
        int minutes = (int) (elapsedTime / (60 * 1000) % 60);
        int hours = (int) (elapsedTime / (60 * 60 * 1000));

        if (String.valueOf(seconds).length() < 2) {
            secondTextView.setText("0" + seconds);
        } else {
            secondTextView.setText(String.valueOf(seconds));
        }
        if (String.valueOf(minutes).length() < 2) {
            minuteTextView.setText("0" + minutes);
        } else {
            minuteTextView.setText(String.valueOf(minutes));
        }
        if (String.valueOf(hours).length() < 2) {
            hourTextView.setText("0" + hours);
        } else {
            hourTextView.setText(String.valueOf(hours));
        }
    }

    /**
     * Returns true if the stopwatch has started
     *
     * @return true if the stopwatch has been started by calling start(). False otherwise
     * @since 1.0
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Returns true if the stopwatch is paused
     *
     * @return true if the stopwatch is paused. False otherwise
     * @since 1.0
     */
    public boolean isPaused() {
        return paused;
    }

    public void setTextView(@Nullable TextView seconds, TextView minute, TextView hour) {
        this.secondTextView = seconds;
        this.minuteTextView = minute;
        this.hourTextView = hour;
    }

    /**
     * Starts the stopwatch at the current time. Cannot be called again without calling stop() first.
     *
     * @throws IllegalStateException if the stopwatch has already been started.
     * @see #stop()
     * @see #isStarted()
     * @since 1.0
     */
    public void start() {
        if (started)
            throw new IllegalStateException("Already Started");
        else {
            started = true;
            paused = false;
            start = System.currentTimeMillis();
            current = System.currentTimeMillis();
            elapsedTime = 0;
            handler.post(runnable);
        }
    }

    /**
     * Stops the stopwatch. Stopwatch cannot be resumed from current time later.
     *
     * @throws IllegalStateException if stopwatch has not been started yet.
     * @see #start()
     * @see #isStarted()
     * @since 1.0
     */
    public void stop() {
        if (!started)
            throw new IllegalStateException("Not Started");
        else {
            updateElapsed(System.currentTimeMillis());
            started = false;
            paused = false;
            handler.removeCallbacks(runnable);
        }
    }

    /**
     * Pauses the stopwatch. Using this allows you to resume the stopwatch from the current state.
     *
     * @throws IllegalStateException if stopwatch is already paused or not started yet.
     * @see #resume()
     * @see #isPaused()
     * @since 1.0
     */
    public void pause() {
        if (paused)
            throw new IllegalStateException("Already Paused");
        else if (!started)
            throw new IllegalStateException("Not Started");
        else {
            updateElapsed(System.currentTimeMillis());
            paused = true;
            handler.removeCallbacks(runnable);
        }
    }

    /**
     * Used to resume the stopwatch from the current time after being paused.
     *
     * @throws IllegalStateException if stopwatch is not paused or not started yet.
     * @see #pause()
     * @see #isPaused()
     * @since 1.0
     */
    public void resume() {
        if (!paused)
            throw new IllegalStateException("Not Paused");
        else if (!started)
            throw new IllegalStateException("Not Started");
        else {
            paused = false;
            current = System.currentTimeMillis();
            handler.post(runnable);
        }
    }

    /**
     * Updates the time in elapsed and lap time and then updates the current time.
     *
     * @param time Current time in millis. Passing any other value may result in odd behaviour
     * @since 1.1
     */
    private void updateElapsed(long time) {
        elapsedTime += time - current;
        current = time;
    }

    /**
     * The main thread responsible for updating and displaying the time
     *
     * @since 1.1
     */
    private void run() {
        if (!started || paused) {
            handler.removeCallbacks(runnable);
            return;
        }
        updateElapsed(System.currentTimeMillis());
        handler.postDelayed(runnable, clockDelay);

        if (logEnabled)
            Log.d("STOPWATCH", elapsedTime / 1000 + " seconds, " + elapsedTime % 1000 + " milliseconds");

        if (secondTextView != null) {
            getFormattedTime(elapsedTime);
        }
    }
}
