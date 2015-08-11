package org.dodgybits.shuffle.android.view.activity;

import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.activity.TaskListActivity;

public class TaskViewActivity extends AbstractMainActivity {
    private static final String TAG = "TaskViewActivity";


    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (mActionBarToolbar != null) {
            ViewCompat.setElevation(mActionBarToolbar, 0f);
            ViewCompat.setElevation((View) mActionBarToolbar.getParent(), 0f);
        }

    }

    @Override
    protected void validateActivity() {
        if (UiUtilities.showListOnViewTask(getResources())) {
            Log.d(TAG, "Switching to TaskListViewActivity");
            redirect(TaskListViewActivity.class);
        }
    }

    @Override
    protected void validateLocation(Location location) {
        if (location.isListView()) {
            Log.d(TAG, "Switching to TaskListActivity");
            redirect(TaskListActivity.class, location);
        }
    }


    @Override
    protected int contentView(boolean isTablet) {
        return R.layout.task_view_activity;
    }

}
