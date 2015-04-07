package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.content.Intent;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.list.event.EditListSettingsEvent;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;

import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class ListSettingsListener {

    private Activity mActivity;

    @Inject
    public ListSettingsListener(Activity activity) {
        mActivity = activity;
    }

    private void onEditListSettings(@Observes EditListSettingsEvent event) {
        Intent intent = ListSettingsCache.createListSettingsEditorIntent(mActivity, event.getListQuery());
        event.getActivity().startActivityForResult(intent, event.getRequestCode());
    }

}
