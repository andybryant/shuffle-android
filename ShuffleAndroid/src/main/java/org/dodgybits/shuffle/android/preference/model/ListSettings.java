package org.dodgybits.shuffle.android.preference.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;

public class ListSettings {
    private static final String TAG = "ListSettings";
    
    public static final String LIST_FILTER_ACTIVE = ".list_active.2";
    public static final String LIST_FILTER_COMPLETED = ".list_completed.2";

    private String mPrefix;
    private Flag mDefaultCompleted = Flag.no;
    private Flag mDefaultDeleted = Flag.no;
    private Flag mDefaultActive = Flag.yes;


    private boolean mCompletedEnabled = false;
    private boolean mActiveEnabled = false;

    public ListSettings(String prefix) {
        this.mPrefix = prefix;
    }

    public String getPrefix() {
        return mPrefix;
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Flag getDefaultCompleted() {
        return mDefaultCompleted;
    }

    public Flag getDefaultDeleted() {
        return mDefaultDeleted;
    }

    public Flag getDefaultActive() {
        return mDefaultActive;
    }

    public ListSettings setDefaultCompleted(Flag value) {
        mDefaultCompleted = value;
        return this;
    }

    public ListSettings setDefaultDeleted(Flag value) {
        mDefaultDeleted = value;
        return this;
    }

    public ListSettings setDefaultActive(Flag value) {
        mDefaultActive = value;
        return this;
    }

    public boolean isCompletedEnabled() {
        return mCompletedEnabled;
    }

    public ListSettings enableCompleted() {
        mCompletedEnabled = true;
        return this;
    }

    public boolean isActiveEnabled() {
        return mActiveEnabled;
    }

    public ListSettings enableActive() {
        mActiveEnabled = true;
        return this;
    }

    public Flag getActive(Context context) {
        return getFlag(context, LIST_FILTER_ACTIVE, mDefaultActive);
    }

    public void setActive(Context context, Flag flag) {
        getSharedPreferences(context).edit()
                .putString(mPrefix + LIST_FILTER_ACTIVE, flag.name())
                .commit();
    }

    public Flag getCompleted(Context context) {
        return getFlag(context, LIST_FILTER_COMPLETED, mDefaultCompleted);
    }

    public void setCompleted(Context context, Flag flag) {
        getSharedPreferences(context).edit()
                .putString(mPrefix + LIST_FILTER_COMPLETED, flag.name())
                .commit();
    }

    private Flag getFlag(Context context, String setting, Flag defaultValue) {
        String valueStr = getSharedPreferences(context).getString(mPrefix + setting, defaultValue.name());
        Flag value = defaultValue;
        try {
            value = Flag.valueOf(valueStr);
        } catch (IllegalArgumentException e) {
            String message = String.format("Unrecognized flag setting %s for settings %s using default %s", 
                    valueStr, setting, defaultValue);
            Log.e(TAG, message);
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String message = String.format("Got value %s for settings %s%s with default %s",
                    value, mPrefix, setting, defaultValue);
            Log.d(TAG, message);
        }
        return value;
    }

}
