package org.dodgybits.shuffle.android.core.view;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class NavigationController {
    private Location mLocation;

    @Inject
    private EventManager mEventManager;

    @Inject
    private LocationParser mLocationParser;

    private FragmentActivity mFragmentActivity;

    public NavigationController(FragmentActivity fragmentActivity) {
        mFragmentActivity = fragmentActivity;
    }

    private void onLocationUpdated(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
    }

    private void onNavRequest(@Observes NavigationRequestEvent event) {
        Location newLocation = event.getLocation();
        if (newLocation.isSameActivity(mLocation)) {
           mEventManager.fire(new LocationUpdatedEvent(newLocation));
        } else {
            Intent intent = LocationParser.createIntent(mFragmentActivity, newLocation);
            mFragmentActivity.startActivity(intent);
        }
    }

}
