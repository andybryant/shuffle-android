package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class NavigationController {
    private static final String TAG = "NavigationController";

    private Location mLocation;

    @Inject
    private EventManager mEventManager;

    @Inject
    private LocationParser mLocationParser;

    private FragmentActivity mFragmentActivity;

    @Inject
    public NavigationController(Activity activity) {
        mFragmentActivity = (FragmentActivity) activity;
    }

    private void onLocationUpdated(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
    }

    private void onNavRequest(@Observes NavigationRequestEvent event) {
        Location newLocation = event.getLocation();
        if (shouldStartNewActivity(newLocation) ) {
            Log.i(TAG, "Loading location in new activity: " + newLocation);
            Intent intent = LocationParser.createIntent(mFragmentActivity, newLocation);
            mFragmentActivity.startActivity(intent);
        } else {
            Log.i(TAG, "Loading location in same activity: " + newLocation);
            mEventManager.fire(new LocationUpdatedEvent(newLocation));
        }
    }

    private boolean shouldStartNewActivity(Location newLocation) {
        return (mLocation.getLocationActivity() != newLocation.getLocationActivity() ||
                mLocation.getListQuery() != newLocation.getListQuery() ||
                mLocation.isListView() != newLocation.isListView());
    }

}
