/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dodgybits.shuffle.android.preference.activity;

import android.Manifest;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.CalendarUtils;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.activity.RoboPreferenceActivity;

public class PreferencesActivity extends RoboPreferenceActivity {
    private static final String TAG = "PreferencesActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 1;

    private AsyncQueryHandler mQueryHandler;
    private ListPreference mPreference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        setCalendarPreferenceEntries();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CALENDAR: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted,
                    queryCalendars();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    mPreference.setEnabled(false);
                }
                break;
            }
        }
    }

    private void setCalendarPreferenceEntries() {
        mPreference = (ListPreference)findPreference(Preferences.CALENDAR_ID_KEY);
        // disable the pref until we load the values (if at all)
        mPreference.setEnabled(false);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG, "Requesting permission to read calendar");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    MY_PERMISSIONS_REQUEST_READ_CALENDAR);
        } else {
            queryCalendars();
        }

    }

    private void queryCalendars() {
        // start a query in the background to read the list of calendars
        mQueryHandler = new QueryHandler(getContentResolver());
        CalendarUtils.startQuery(mQueryHandler);
    }
    
    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null) {
                int selectedIndex = -1;
                final String currentValue = String.valueOf(
                        Preferences.getCalendarId(PreferencesActivity.this));
                
                final int numCalendars = cursor.getCount();
                final String[] values = new String[numCalendars];
                final String[] names = new String[numCalendars];
                for(int i = 0; i < numCalendars; i++) {
                    cursor.moveToPosition(i);
                    values[i] = cursor.getString(0);
                    names[i] = cursor.getString(1);
                    
                    if (currentValue.equals(values[i])) {
                        selectedIndex = i;
                    }
                }
                cursor.close();
                
                mPreference.setEntryValues(values);
                mPreference.setEntries(names);
                if (selectedIndex >= 0) {
                    mPreference.setValueIndex(selectedIndex);
                }
                mPreference.setEnabled(true);                
            } else {
                Log.e(TAG, "Failed to fetch calendars - setting disabled.");
            }
        }
    }
}
