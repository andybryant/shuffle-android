package org.dodgybits.shuffle.android.list.activity;

import android.os.Bundle;
import android.view.View;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.view.Location;

public class ContextListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.ContextList;
    }

    public void onClickFab(View view) {
        Location location = Location.newContext();
        mEventManager.fire(new NavigationRequestEvent(location));
    }

}
