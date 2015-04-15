package org.dodgybits.shuffle.android.list.activity;

import android.os.Bundle;
import android.util.Log;

import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.view.Location;

public class ContextListActivity extends AbstractMainActivity {
    private static final String TAG = "ContextListActivity";

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Log.d(TAG, "+onCreate");
    }

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.ContextList;
    }
}
