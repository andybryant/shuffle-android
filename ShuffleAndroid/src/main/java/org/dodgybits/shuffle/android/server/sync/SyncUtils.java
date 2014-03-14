package org.dodgybits.shuffle.android.server.sync;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import org.dodgybits.shuffle.android.preference.model.Preferences;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.*;


public class SyncUtils {

    public static boolean isSyncOn(Context context) {
        return Preferences.isSyncEnabled(context) && Preferences.getSyncAccount(context) != null;
    }

    public static void scheduleSync(Context context, String source) {
        Intent syncIntent = new Intent(context, SyncSchedulingService.class);
        syncIntent.putExtra(SOURCE_EXTRA, source);
        WakefulBroadcastReceiver.startWakefulService(context, syncIntent);

    }

    public static void scheduleSyncAfterError(Context context, int cause) {
        Intent syncIntent = new Intent(context, SyncSchedulingService.class);
        syncIntent.putExtra(SOURCE_EXTRA, SYNC_FAILED_SOURCE);
        syncIntent.putExtra(CAUSE_EXTRA, cause);
        context.startService(syncIntent);
    }


}
