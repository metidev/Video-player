package com.example.video.modals;

import android.annotation.SuppressLint;

public abstract class Utility {
    @SuppressLint("DefaultLocale")
    public static String timeConversion(Long millis) {
        if (millis != null) {
            long seconds = (millis / 1000);
            long sec = seconds % 60;
            long min = (seconds / 60) % 60;
            long hrs = (seconds / (60 * 60)) % 24;
            if (hrs > 0) {
                return String.format("%02d:%02d:%02d", hrs, min, sec);
            } else {
                return String.format("%02d:%02d", min, sec);
            }
        } else {

            return null;
        }
    }
}
