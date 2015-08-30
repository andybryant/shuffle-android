package org.dodgybits.shuffle.android.list.activity;

import android.view.View;

import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.view.Location;

public class ProjectListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.ProjectList;
    }

    public void onClickFab(View view) {
        Location location = Location.newProject();
        mEventManager.fire(new NavigationRequestEvent(location));
    }

}
