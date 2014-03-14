package org.dodgybits.shuffle.android.server.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.ALARM_SOURCE;

public class SyncAlarmReceiver extends BroadcastReceiver {
    public static final String TAG = "SyncAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received intent " + intent.getExtras());
        SyncUtils.scheduleSync(context, ALARM_SOURCE);
    }
}
