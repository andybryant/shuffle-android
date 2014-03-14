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
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import org.dodgybits.shuffle.android.preference.model.Preferences;

import java.util.Date;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * Gets notified when events happen that would require a sync and triggers
 * a sync when it deems it necessary.
 */
public class SyncSchedulingService extends IntentService {

    public static final String SOURCE_EXTRA = "source";
    public static final String SYNC_FAILED_SOURCE = "syncFailed";
    public static final String MANUAL_SOURCE = "manual";
    public static final String ALARM_SOURCE = "alarm";
    public static final String LOCAL_CHANGE_SOURCE = "localChange";
    public static final String GCM_SOURCE = "gcm";
    public static final String CAUSE_EXTRA = "cause";
    public static final int NO_RESPONSE_CAUSE = 1;
    public static final int FAILED_STATUS_CAUSE = 2;
    public static final int INVALID_AUTH_TOKEN_CAUSE = 3;
    public static final int INVALID_SYNC_ID_CAUSE = 4;


    public SyncSchedulingService() {
        super("SyncSchedulingService");
    }
    public static final String TAG = "SyncSchedulingService";

    @Override
    protected void onHandleIntent(Intent intent) {
        SyncState state = SyncState.restore(this);
        state.process(this, intent.getExtras());

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent syncIntent = new Intent(this, SyncReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, state.nextScheduledSync, pendingIntent);

        // Release the wake lock provided by the WakefulBroadcastReceiver
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    private static class SyncState {
        private long previousSuccess;
        private long nextScheduledSync;
        private long previousFailure;

        void process(Context context, Bundle bundle) {
            String source = bundle.getString(SOURCE_EXTRA);
            Log.i(TAG, "Received intent from " + source + " " + bundle);
            final long now = System.currentTimeMillis();
            switch (source) {
                case SYNC_FAILED_SOURCE:
                    Preferences.getEditor(context)
                            .putLong(Preferences.SYNC_LAST_SYNC_FAILURE_DATE, now)
                            .commit();
                    if (previousFailure > 0L) {
                        // exponential back-off
                        long msAgo = now - previousFailure;
                        nextScheduledSync = now + Math.max(msAgo * 2L, 5 * MINUTE_IN_MILLIS);
                    } else {
                        nextScheduledSync = now + 5 * MINUTE_IN_MILLIS;
                    }
                    break;
                case MANUAL_SOURCE:
                    nextScheduledSync = now + 5 * SECOND_IN_MILLIS;
                    break;
                case ALARM_SOURCE:
                    long msAgo = now - previousSuccess;
                    if (previousSuccess > 0L && msAgo < 10 * MINUTE_IN_MILLIS) {
                        Log.d(TAG, "Sync " + msAgo + "ms ago - do nothing");
                    } else {
                        nextScheduledSync = now + 2 * MINUTE_IN_MILLIS;
                    }
                    break;
                case LOCAL_CHANGE_SOURCE:
                case GCM_SOURCE:
                    nextScheduledSync = now + 5 * MINUTE_IN_MILLIS;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown source " + source);
            }

            if (Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Next sync at " + new Date(nextScheduledSync));
            }
        }

        static SyncState restore(Context context) {
            SyncState state = new SyncState();
            state.previousSuccess = Preferences.getLastSyncLocalDate(context);
            state.previousFailure = Preferences.getLastSyncFailureDate(context);
            return state;
        }
    }

}
