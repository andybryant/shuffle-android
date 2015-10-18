package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.event.ActiveToggleEvent;
import org.dodgybits.shuffle.android.core.event.CompletedToggleEvent;
import org.dodgybits.shuffle.android.core.event.LoadListCursorEvent;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class ListSettingsListener {
    private static final String TAG = "ListSettingsListener";

    private Activity mActivity;
    private EventManager mEventManager;

    @Inject
    public ListSettingsListener(Activity activity, EventManager eventManager) {
        mActivity = activity;
        mEventManager = eventManager;
    }

    private void onCompletedToggle(@Observes CompletedToggleEvent event) {
        Flag flag = event.isChecked() ? Flag.yes : Flag.no;
        Log.d(TAG, "Received event " + event);
        ListQuery listQuery = event.getLocation().getListQuery();
        ListSettingsCache.findSettings(listQuery).setCompleted(mActivity, flag);
        mEventManager.fire(new LoadListCursorEvent(event.getLocation()));
    }

    private void onActiveToggle(@Observes ActiveToggleEvent event) {
        Flag flag = event.isChecked() ? Flag.no : Flag.yes;
        Log.d(TAG, "Received event " + event);
        ListQuery listQuery = event.getLocation().getListQuery();
        ListSettingsCache.findSettings(listQuery).setActive(mActivity, flag);
        mEventManager.fire(new LoadListCursorEvent(event.getLocation()));
    }

}
