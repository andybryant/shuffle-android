package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.CacheUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.TitleHandler;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class TitleUpdater {

    private AppCompatActivity mActivity;

    @Inject
    private TitleHandler mTitleHandler;

    @Inject
    public TitleUpdater(Activity activity) {
        mActivity = (AppCompatActivity) activity;
    }

    private Location mLocation;

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
        updateTitle();
    }

    private void onCacheUpdated(@Observes CacheUpdatedEvent event) {
        updateTitle();
    }

    private void updateTitle() {
        if (mLocation == null || mLocation.getViewMode() == null) {
            return;
        }
        mActivity.setTitle(mTitleHandler.getTitle(mActivity, mLocation));
    }

}
