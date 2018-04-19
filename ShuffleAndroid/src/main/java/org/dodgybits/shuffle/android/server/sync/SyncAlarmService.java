/*
 * Copyright (C) 2013 Android Shuffle Open Source Project
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

package org.dodgybits.shuffle.android.server.sync;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateUtils;
import android.util.Log;

import org.dodgybits.shuffle.android.preference.model.Preferences;

import java.util.Date;

/**
 * Kicks off a recurring alarm to ensure sync happens periodically
 * regardless of whether changes have been detected.
 */
public class SyncAlarmService extends IntentService {

    private static final long SYNC_PERIOD = DateUtils.DAY_IN_MILLIS;

    public SyncAlarmService() {
        super("SyncAlarmService");
    }
    public static final String TAG = "SyncAlarmService";

    @Override
    protected void onHandleIntent(Intent intent) {
        long lastSyncDate = Preferences.getLastSyncLocalDate(this);
        long nextSyncDate = Math.max(
                System.currentTimeMillis() + 5000L,
                lastSyncDate + SYNC_PERIOD);

        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "Next sync at " + new Date(nextSyncDate));
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent syncIntent = new Intent(this, SyncAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setRepeating(AlarmManager.RTC, nextSyncDate,
                SYNC_PERIOD,
                pendingIntent);

        // Release the wake lock provided by the WakefulBroadcastReceiver
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

}
