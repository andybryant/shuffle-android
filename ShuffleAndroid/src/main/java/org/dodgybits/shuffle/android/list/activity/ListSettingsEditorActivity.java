package org.dodgybits.shuffle.android.list.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.OSUtils;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import org.dodgybits.shuffle.android.preference.model.ListSettings;
import roboguice.activity.RoboPreferenceActivity;

public class ListSettingsEditorActivity extends RoboPreferenceActivity {
    private static final String TAG = "ListSettingsEditor";

    public static final String LIST_QUERY_EXTRA = "listQuery";
    
    private static final String[] CONTEXT_PROJECTION = new String[] {
            ContextProvider.Contexts._ID,
            ContextProvider.Contexts.NAME
    };

    private static final String[] PROJECT_PROJECTION = new String[] {
            ProjectProvider.Projects._ID,
            ProjectProvider.Projects.NAME
    };

    private ListSettings mSettings;
    private ListQuery mListQuery;
    private boolean mPrefsChanged;
    private ListListener mListListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = ListSettings.fromIntent(getIntent());
        mListQuery = ListQuery.valueOf(getIntent().getStringExtra(LIST_QUERY_EXTRA));
        mListListener = new ListListener();
        
        setupScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        mPrefsChanged = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPrefsChanged) {
            Intent intent = new Intent(ListSettings.LIST_PREFERENCES_UPDATED);
            intent.putExtra(LIST_QUERY_EXTRA, mListQuery.name());
            sendBroadcast(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }

    private void setupScreen() {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);
        int screenId = getStringId("title_" + mSettings.getPrefix());
        String title = getString(screenId) + " " + getString(R.string.list_settings_title);
        screen.setTitle(title);

        screen.addPreference(createList(
                R.array.list_preferences_active_labels,
                R.string.active_items_title,
                mSettings.getActive(this).name(),
                ListSettings.LIST_FILTER_ACTIVE,
                mSettings.getDefaultActive().name(),
                mSettings.isActiveEnabled()
        ));

        screen.addPreference(createList(
                R.array.list_preferences_pending_labels,
                R.string.pending_items_title,
                mSettings.getPending(this).name(),
                ListSettings.LIST_FILTER_PENDING,
                mSettings.getDefaultPending().name(),
                mSettings.isPendingEnabled()
        ));

        screen.addPreference(createList(
                R.array.list_preferences_completed_labels,
                R.string.completed_items_title,
                mSettings.getCompleted(this).name(),
                ListSettings.LIST_FILTER_COMPLETED,
                mSettings.getDefaultCompleted().name(),
                mSettings.isCompletedEnabled()
        ));

        screen.addPreference(createList(
                R.array.list_preferences_deleted_labels,
                R.string.deleted_items_title,
                mSettings.getDeleted(this).name(),
                ListSettings.LIST_FILTER_DELETED,
                mSettings.getDefaultDeleted().name(),
                mSettings.isDeletedEnabled()
        ));

        screen.addPreference(createContextList());
        screen.addPreference(createProjectList());

        screen.addPreference(createQuickAdd());

        setPreferenceScreen(screen);
    }

    private int getStringId(String id) {
        return getResources().getIdentifier(id, "string", getPackageName());
    }

    private ListPreference createList(
            int entries, int title, String value, String keySuffix, Object defaultValue, boolean enabled) {
        ListPreference listPreference = new ListPreference(this);
        listPreference.setEntryValues(R.array.list_preferences_flag_values);
        listPreference.setEntries(entries);
        listPreference.setTitle(title);
        String key = mSettings.getPrefix() + keySuffix;
        listPreference.setKey(key);
        listPreference.setDefaultValue(defaultValue);
        listPreference.setOnPreferenceChangeListener(mListListener);
        listPreference.setEnabled(enabled);

        CharSequence[] entryStrings = listPreference.getEntries();
        int index = listPreference.findIndexOfValue(value);
        if (index > -1) {
            listPreference.setSummary(entryStrings[index]);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String message = String.format("Creating list preference key=%s value=%s default=%s title=%s",
                    key, value, defaultValue, title);
            Log.d(TAG, message);
        }

        return listPreference;
    }

    @SuppressLint("NewApi")
    private Preference createQuickAdd() {
        Preference quickAddPref = new SwitchPreference(this);
        quickAddPref.setTitle(R.string.quick_add_title);
        quickAddPref.setDefaultValue(mSettings.getDefaultQuickAdd());
        quickAddPref.setKey(mSettings.getPrefix() + ListSettings.LIST_FILTER_QUICK_ADD);
        quickAddPref.setEnabled(mSettings.isQuickAddEnabled());
        quickAddPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                mPrefsChanged = true;
                return true;
            }

        });
        return quickAddPref;       
    }
    
    private Preference createContextList() {
        Cursor contextCursor = getContentResolver().query(
                ContextProvider.Contexts.CONTENT_URI, CONTEXT_PROJECTION,
                null, null, null);
        int arraySize = contextCursor.getCount() + 1;
        String[] values = new String[arraySize];
        values[0] = String.valueOf(0);
        String[] names = new String[arraySize];
        names[0] = getText(R.string.show_all).toString();
        for (int i = 1; i < arraySize; i++) {
            contextCursor.moveToNext();
            values[i] = contextCursor.getString(0);
            names[i] = contextCursor.getString(1);
        }
        ListPreference listPreference = new ListPreference(this);
        listPreference.setEntryValues(values);
        listPreference.setEntries(names);
        listPreference.setTitle(R.string.context_title);
        String key = mSettings.getPrefix() + ListSettings.LIST_FILTER_CONTEXT;
        listPreference.setKey(key);
        listPreference.setDefaultValue(String.valueOf(0));
        listPreference.setOnPreferenceChangeListener(mListListener);
        listPreference.setEnabled(mSettings.isContextEnabled());

        CharSequence[] entryStrings = listPreference.getEntries();
        int index = listPreference.findIndexOfValue(String.valueOf(mSettings.getContextId(this).getId()));
        if (index > -1) {
            listPreference.setSummary(entryStrings[index]);
        }

        return listPreference;
    }

    private Preference createProjectList() {
        Cursor projectCursor = getContentResolver().query(
                ProjectProvider.Projects.CONTENT_URI, PROJECT_PROJECTION,
                null, null, null);
        int arraySize = projectCursor.getCount() + 1;
        String[] values = new String[arraySize];
        values[0] = String.valueOf(0);
        String[] names = new String[arraySize];
        names[0] = getText(R.string.show_all).toString();
        for (int i = 1; i < arraySize; i++) {
            projectCursor.moveToNext();
            values[i] = projectCursor.getString(0);
            names[i] = projectCursor.getString(1);
        }
        ListPreference listPreference = new ListPreference(this);
        listPreference.setEntryValues(values);
        listPreference.setEntries(names);
        listPreference.setTitle(R.string.project_title);
        String key = mSettings.getPrefix() + ListSettings.LIST_FILTER_PROJECT;
        listPreference.setKey(key);
        listPreference.setDefaultValue(String.valueOf(0));
        listPreference.setOnPreferenceChangeListener(mListListener);
        listPreference.setEnabled(mSettings.isProjectEnabled());

        CharSequence[] entryStrings = listPreference.getEntries();
        int index = listPreference.findIndexOfValue(String.valueOf(mSettings.getProjectId(this).getId()));
        if (index > -1) {
            listPreference.setSummary(entryStrings[index]);
        }

        return listPreference;
    }

    private class ListListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            ListPreference listPreference = (ListPreference)preference;
            int index = listPreference.findIndexOfValue((String)o);
            preference.setSummary(listPreference.getEntries()[index]);
            mPrefsChanged = true;
            return true;
        }

    }
    
}