package org.dodgybits.shuffle.android.view.activity;

import android.os.Bundle;
import android.util.Log;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.activity.TaskListActivity;

public class TaskListViewActivity extends TaskListActivity {
    private static final String TAG = "TaskListViewActivity";


    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

    }

    @Override
    protected void validateActivity() {
    }

    @Override
    protected void validateLocation(Location location) {
        if (location.isListView()) {
            Log.d(TAG, "Switching to TaskListActivity");
            redirect(TaskListActivity.class);
        } else if (!UiUtilities.showListOnViewTask(getResources())) {
            Log.d(TAG, "Switching to TaskViewActivity");
            redirect(TaskViewActivity.class);
        }
    }

    @Override
    protected int contentView(boolean isTablet) {
        return R.layout.task_list_view_activity;
    }

}
