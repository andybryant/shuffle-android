package org.dodgybits.shuffle.android.list.activity;

import android.os.Bundle;

import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.view.Location;

public class ContextListActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.ContextList;
    }
}
