package kg.delletenebre.callsassistant.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import kg.delletenebre.callsassistant.App;

public class Prefs {

    SharedPreferences mPrefs;
    Context mContext;

    public Prefs(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public int getInt(String key, String defaultValue) {
        int result = Integer.parseInt(defaultValue);
        try {
            result = Integer.parseInt(mPrefs.getString(key, defaultValue));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return result;
    }

    public int getInt(String key) {
        return getInt(key,
                mContext.getString(getStringIdentifier(mContext, "pref_default_" + key)));
    }


    public static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getInstance());
        return prefs.getBoolean(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key,
                App.getInstance().getResources().getBoolean(getBooleanIdentifier(mContext, "pref_default_" + key)));
    }

    public String getString(String key, String defaultValue) {
        return mPrefs.getString(key, defaultValue);
    }

    public String getString(String key) {
        return getString(key,
                mContext.getResources().getString(getStringIdentifier(mContext, "pref_default_" + key)));
    }

    public int getStringIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "string", context.getPackageName());
    }

    public int getBooleanIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "bool", context.getPackageName());
    }

    public int getIdIdentifier(Context context, String name) {
        return context.getResources().getIdentifier(name, "id", context.getPackageName());
    }
}
