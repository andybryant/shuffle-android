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
    public static final String LIST_FILTER_DELETED = ".list_deleted.2";
    public static final String LIST_FILTER_PENDING = ".list_pending.2";

    private String mPrefix;
    private Flag mDefaultCompleted = Flag.no;
    private Flag mDefaultPending = Flag.no;
    private Flag mDefaultDeleted = Flag.no;
    private Flag mDefaultActive = Flag.yes;


    private boolean mQuickAddEnabled = false;
    private boolean mCompletedEnabled = true;
    private boolean mPendingEnabled = true;
    private boolean mDeletedEnabled = true;
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

    public Flag getDefaultPending() {
        return mDefaultPending;
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

    public ListSettings setDefaultPending(Flag value) {
        mDefaultPending = value;
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

    public ListSettings disableCompleted() {
        mCompletedEnabled = false;
        return this;
    }

    public boolean isPendingEnabled() {
        return mPendingEnabled;
    }

    public ListSettings disablePending() {
        mPendingEnabled = false;
        return this;
    }

    public boolean isDeletedEnabled() {
        return mDeletedEnabled;
    }

    public ListSettings disableDeleted() {
        mDeletedEnabled = false;
        return this;
    }

    public boolean isActiveEnabled() {
        return mActiveEnabled;
    }

    public ListSettings enableActive() {
        mActiveEnabled = true;
        return this;
    }

    public boolean isQuickAddEnabled() {
        return mQuickAddEnabled;
    }

    public ListSettings enableQuickAdd() {
        mQuickAddEnabled = true;
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

    public Flag getDeleted(Context context) {
        return getFlag(context, LIST_FILTER_DELETED, mDefaultDeleted);
    }

    public Flag getPending(Context context) {
        return getFlag(context, LIST_FILTER_PENDING, mDefaultPending);
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
