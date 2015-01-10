package com.sferadev.speedreading.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.SettingNotFoundException;

import static com.sferadev.speedreading.App.getContext;

public class PreferenceUtils {
    // Get String Preference
    public static String getPreference(String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(key, defaultValue);
    }

    // Get Boolean Preference
    public static boolean getPreference(String key, boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(key, defaultValue);
    }

    // Get Integer Preference
    public static int getPreference(String key, int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(key, defaultValue);
    }

    // Return System Preference
    public static int getSystemPreference(String preferenceName) {
        try {
            return android.provider.Settings.System.getInt(
                    getContext().getContentResolver(), preferenceName);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Set String Preference
    public static void setPreference(String key, String value) {
        SharedPreferences.Editor mEditor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
        mEditor.putString(key, value);
        mEditor.apply();
    }

    // Set Boolean Preference
    public static void setPreference(String key, boolean value) {
        SharedPreferences.Editor mEditor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
        mEditor.putBoolean(key, value);
        mEditor.apply();
    }

    // Set Integer Preference
    public static void setPreference(String key, int value) {
        SharedPreferences.Editor mEditor = PreferenceManager
                .getDefaultSharedPreferences(getContext()).edit();
        mEditor.putInt(key, value);
        mEditor.apply();
    }

    // Set System Preference
    public static void setSystemPreference(String preferenceName, int value) {
        android.provider.Settings.System.putInt(getContext().getContentResolver(),
                preferenceName, value);
    }
}
