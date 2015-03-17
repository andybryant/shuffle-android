package org.dodgybits.shuffle.android.preference.model;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;

public class ListSettings {
    private static final String TAG = "ListSettings";
    
    public static final String LIST_PREFERENCES_UPDATED = "org.dodgybits.shuffle.android.LIST_PREFERENCES_UPDATE";

    public static final String LIST_FILTER_ACTIVE = ".list_active";
    public static final String LIST_FILTER_COMPLETED = ".list_completed";
    public static final String LIST_FILTER_DELETED = ".list_deleted";
    public static final String LIST_FILTER_PENDING = ".list_pending";
    public static final String LIST_FILTER_PROJECT = ".list_project";
    public static final String LIST_FILTER_CONTEXT = ".list_context";

    private static final String PREFIX = "mPrefix";
    private static final String BUNDLE = "list-preference-settings";
    private static final String DEFAULT_COMPLETED = "defaultCompleted";
    private static final String DEFAULT_PENDING = "defaultPending";
    private static final String DEFAULT_DELETED = "defaultDeleted";
    private static final String DEFAULT_ACTIVE = "defaultActive";

    private static final String COMPLETED_ENABLED = "completedEnabled";
    private static final String PENDING_ENABLED = "pendingEnabled";
    private static final String DELETED_ENABLED = "deletedEnabled";
    private static final String ACTIVE_ENABLED = "activeEnabled";
    private static final String PROJECT_ENABLED = "projectEnabled";
    private static final String CONTEXT_ENABLED = "contextEnabled";


    private String mPrefix;
    private Flag mDefaultCompleted = Flag.no;
    private Flag mDefaultPending = Flag.ignored;
    private Flag mDefaultDeleted = Flag.no;
    private Flag mDefaultActive = Flag.yes;

    private boolean mCompletedEnabled = true;
    private boolean mPendingEnabled = true;
    private boolean mDeletedEnabled = true;
    private boolean mActiveEnabled = true;
    private boolean mProjectEnabled = true;
    private boolean mContextEnabled = true;

    public ListSettings(String prefix) {
        this.mPrefix = prefix;
    }

    public void addToIntent(Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putString(PREFIX, mPrefix);
        bundle.putString(DEFAULT_COMPLETED, mDefaultCompleted.name());
        bundle.putString(DEFAULT_PENDING, mDefaultPending.name());
        bundle.putString(DEFAULT_DELETED, mDefaultDeleted.name());
        bundle.putString(DEFAULT_ACTIVE, mDefaultActive.name());
        bundle.putBoolean(COMPLETED_ENABLED, mCompletedEnabled);
        bundle.putBoolean(PENDING_ENABLED, mPendingEnabled);
        bundle.putBoolean(DELETED_ENABLED, mDeletedEnabled);
        bundle.putBoolean(ACTIVE_ENABLED, mActiveEnabled);
        bundle.putBoolean(PROJECT_ENABLED, mProjectEnabled);
        bundle.putBoolean(CONTEXT_ENABLED, mContextEnabled);
        intent.putExtra(BUNDLE, bundle);
    }

    public static ListSettings fromIntent(Intent intent) {
        Bundle bundle = intent.getBundleExtra(BUNDLE);
        ListSettings settings = new ListSettings(bundle.getString(PREFIX));
        settings.mDefaultCompleted = Flag.valueOf(bundle.getString(DEFAULT_COMPLETED));
        settings.mDefaultPending = Flag.valueOf(bundle.getString(DEFAULT_PENDING));
        settings.mDefaultDeleted = Flag.valueOf(bundle.getString(DEFAULT_DELETED));
        settings.mDefaultActive = Flag.valueOf(bundle.getString(DEFAULT_ACTIVE));
        settings.mCompletedEnabled = bundle.getBoolean(COMPLETED_ENABLED, true);
        settings.mPendingEnabled = bundle.getBoolean(PENDING_ENABLED, true);
        settings.mDeletedEnabled = bundle.getBoolean(DELETED_ENABLED, true);
        settings.mActiveEnabled = bundle.getBoolean(ACTIVE_ENABLED, true);
        settings.mProjectEnabled = bundle.getBoolean(PROJECT_ENABLED, true);
        settings.mContextEnabled = bundle.getBoolean(CONTEXT_ENABLED, true);
        return settings;
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

    public ListSettings disableActive() {
        mActiveEnabled = false;
        return this;
    }

    public boolean isProjectEnabled() {
        return mProjectEnabled;
    }

    public ListSettings disableProject() {
        mProjectEnabled = false;
        return this;
    }

    public boolean isContextEnabled() {
        return mContextEnabled;
    }

    public ListSettings disableContext() {
        mContextEnabled = false;
        return this;
    }

    public Flag getActive(Context context) {
        return getFlag(context, LIST_FILTER_ACTIVE, mDefaultActive);
    }

    public Flag getCompleted(Context context) {
        return getFlag(context, LIST_FILTER_COMPLETED, mDefaultCompleted);
    }

    public Flag getDeleted(Context context) {
        return getFlag(context, LIST_FILTER_DELETED, mDefaultDeleted);
    }

    public Flag getPending(Context context) {
        return getFlag(context, LIST_FILTER_PENDING, mDefaultPending);
    }

    public Id getProjectId(Context context) {
        return getId(context, LIST_FILTER_PROJECT);
    }

    public Id getContextId(Context context) {
        return getId(context, LIST_FILTER_CONTEXT);
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

    private boolean getBoolean(Context context, String setting, boolean defaultValue) {
        boolean value = getSharedPreferences(context).getBoolean(mPrefix + setting, defaultValue);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String message = String.format("Got value %s for settings %s%s with default %s",
                    value, mPrefix, setting, defaultValue);
            Log.d(TAG, message);
        }
        return value;
    }
    
    private Id getId(Context context, String setting) {
        long value = Long.valueOf(getSharedPreferences(context).getString(mPrefix + setting, "0"));
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String message = String.format("Got value %s for settings %s%s",
                    value, mPrefix, setting);
            Log.d(TAG, message);
        }
        return value == 0L ? Id.NONE : Id.create(value);
    }


}
