package org.dodgybits.shuffle.android.server.sync;

import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.protocol.EntityDirectory;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.sync.event.ResetSyncSettingsEvent;
import org.dodgybits.shuffle.android.server.sync.listener.SyncListener;
import org.dodgybits.shuffle.android.server.sync.processor.ContextSyncProcessor;
import org.dodgybits.shuffle.android.server.sync.processor.ProjectSyncProcessor;
import org.dodgybits.shuffle.android.server.sync.processor.TaskSyncProcessor;
import org.dodgybits.shuffle.dto.ShuffleProtos;
import roboguice.event.EventManager;
import roboguice.inject.ContextSingleton;

import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.FAILED_STATUS_CAUSE;
import static org.dodgybits.shuffle.android.server.sync.SyncSchedulingService.INVALID_SYNC_ID_CAUSE;
import static org.dodgybits.shuffle.android.server.sync.SyncUtils.scheduleSyncAfterError;

@ContextSingleton
public class SyncResponseProcessor {
    private static final String TAG = "SyncResponseProcessor";

    public static final String INVALID_SYNC_ID = "INVALID_SYNC_ID";
    public static final String INVALID_CLIENT_VERSION = "INVALID_CLIENT_VERSION";

    @Inject
    private android.content.Context mContext;
    @Inject
    private ContextSyncProcessor mContextSyncProcessor;
    @Inject
    private ProjectSyncProcessor mProjectSyncProcessor;
    @Inject
    private TaskSyncProcessor mTaskSyncProcessor;
    @Inject
    private EventManager mEventManager;
    @Inject
    private SyncListener mSyncListener;

    public void process(ShuffleProtos.SyncResponse response) {
        if (response.hasErrorCode()) {
            handleError(response);
            return;
        }

        String syncId = response.getSyncId();
        long currentGaeDate = response.getCurrentGaeDate();
        int count = Preferences.getSyncCount(mContext);

        long now = System.currentTimeMillis();
        EntityDirectory<Context> contextLocator = mContextSyncProcessor.processContexts(response);
        now = logTime(now, "process contexts");
        EntityDirectory<Project> projectLocator = mProjectSyncProcessor.processProjects(response, contextLocator);
        now = logTime(now, "process projects");
        mTaskSyncProcessor.processTasks(response, contextLocator, projectLocator);
        now = logTime(now, "process tasks");

        Preferences.getEditor(mContext)
                .putString(Preferences.SYNC_LAST_SYNC_ID, syncId)
                .putLong(Preferences.SYNC_LAST_SYNC_GAE_DATE, currentGaeDate)
                .putLong(Preferences.SYNC_LAST_SYNC_LOCAL_DATE, System.currentTimeMillis())
                .putInt(Preferences.SYNC_COUNT, count + 1)
                .remove(Preferences.SYNC_LAST_SYNC_FAILURE_DATE)
                .commit();
    }

    private long logTime(long lastStop, String message) {
        long now = System.currentTimeMillis();
        Log.d(TAG, "Took " + (now - lastStop) + "ms to " + message);
        return now;
    }

    private void handleError(ShuffleProtos.SyncResponse response) {
        String errorCode = response.getErrorCode();
        String errorMessage = response.getErrorMessage();
        Log.e(TAG, "Sync failed with error code " +
                errorCode + " message: " + errorMessage );

        switch (errorCode) {
            case INVALID_SYNC_ID:
                // device out of sync with server - clear all sync data and request new sync
                mEventManager.fire(new ResetSyncSettingsEvent());
                scheduleSyncAfterError(mContext, INVALID_SYNC_ID_CAUSE);
                break;

            case INVALID_CLIENT_VERSION:
                // TODO tell user to upgrade to latest client
                // include link to Google Play page
                break;

            default:
                scheduleSyncAfterError(mContext, FAILED_STATUS_CAUSE);
                break;
        }
    }


}
