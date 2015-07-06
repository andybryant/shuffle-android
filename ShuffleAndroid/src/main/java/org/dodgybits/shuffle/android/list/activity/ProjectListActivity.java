package org.dodgybits.shuffle.android.list.activity;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.view.Location;

public class ProjectListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.ProjectList;
    }

    @Override
    protected int contentView(boolean isTablet) {
        return R.layout.entity_list_activity;
    }
}
