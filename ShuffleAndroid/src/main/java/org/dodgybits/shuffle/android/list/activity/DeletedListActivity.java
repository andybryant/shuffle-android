package org.dodgybits.shuffle.android.list.activity;

import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.view.Location;

public class DeletedListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.DeletedList;
    }

}
