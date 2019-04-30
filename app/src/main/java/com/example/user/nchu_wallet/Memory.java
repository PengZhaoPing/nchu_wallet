package com.example.user.nchu_wallet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Memory {
    private static SharedPreferences appSharedPrefs;
    private static Editor prefsEditor;

    @SuppressLint("CommitPrefEdits")
    public static void init(Context context) {
        appSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefsEditor = appSharedPrefs.edit();
    }

    public static int getInt(Context context, String key, int defValue) {
        init(context);

        return appSharedPrefs.getInt(key, defValue);
    }

    public static void setInt(Context context, String key, int value) {
        init(context);

        prefsEditor.putInt(key, value);

    }


    public static long getLong(Context context, String key, long defValue) {
        init(context);

        return appSharedPrefs.getLong(key, defValue);
    }

    public static void setLong(Context context, String key, long value) {
        init(context);

        prefsEditor.putLong(key, value);
        prefsEditor.commit();
    }

    public static float getFloat(Context context, String key, float defValue) {
        init(context);

        return appSharedPrefs.getFloat(key, defValue);
    }

    public static void setFloat(Context context, String key, float value) {
        init(context);

        prefsEditor.putFloat(key, value);
        prefsEditor.commit();
    }

    public static String getString(Context context, String key, String defValue) {
        init(context);

        return appSharedPrefs.getString(key, defValue);
    }

    public static void setString(Context context, String key, String data) {
        init(context);

        prefsEditor.putString(key, data);
        prefsEditor.commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        init(context);

        return appSharedPrefs.getBoolean(key, defValue);
    }

    public static boolean setBoolean(Context context, String key, boolean data) {
        init(context);

        prefsEditor.putBoolean(key, data);
        prefsEditor.commit();
        return data;
    }



    public static Set<String> getStringSet(Context context, String key) {
        init(context);

        return appSharedPrefs.getStringSet(key, new HashSet<String>());
    }

    public static void setStringSet(Context context, String key, Set<String> data_set) {
        init(context);

        prefsEditor.putStringSet(key, data_set);
        prefsEditor.commit();
    }

    public static ArrayList<String> getStringArray(Context context, String key, String defValue) {
        init(context);
        String data = appSharedPrefs.getString(key, defValue);
        ArrayList<String> string_array = new ArrayList<>();

        if (data != null) {
            for (int i = 0; i < data.split("\n").length; i++) {
                string_array.add(data.split("\n")[i]);
//                Toast.makeText(context, data.split("\n")[i], Toast.LENGTH_SHORT).show();
            }
        }
        return string_array;
    }

    public static void setStringArray(Context context, String key, ArrayList<String> data) {
        init(context);
        String string_array = "";
        if (data != null) {
            for (int i = 0; i < data.size() - 1; i++) {
                string_array = string_array + data.get(i) + "\n";
            }
        } else string_array = null;
//        Toast.makeText(context, string_array, Toast.LENGTH_SHORT).show();
        prefsEditor.putString(key, string_array);
        prefsEditor.commit();
    }

    public static ArrayList<Float> getFloatArray(Context context, String key, String defValue) {
        init(context);
        String data = appSharedPrefs.getString(key, defValue);
        ArrayList<Float> string_array = new ArrayList<>();

        if (data != null) {
            for (int i = 0; i < data.split("\n").length - 1; i++) {
                string_array.add(Float.parseFloat(data.split("\n")[i]));

//                Toast.makeText(context, data.split("\n")[i], Toast.LENGTH_SHORT).show();
            }
        }
        return string_array;
    }

    public static void setFloatArray(Context context, String key, ArrayList<Float> data) {
        init(context);
        String string_array = "";
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                string_array = string_array + data.get(i) + "\n";
            }
        } else string_array = null;
//        Toast.makeText(context, string_array, Toast.LENGTH_SHORT).show();
        prefsEditor.putString(key, string_array);
        prefsEditor.commit();
    }
}
