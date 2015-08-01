package org.dodgybits.shuffle.android.view.activity;

import android.content.Intent;
import android.os.Bundle;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.activity.TaskListActivity;

public class TaskListViewActivity extends TaskListActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (UiUtilities.hideListOnViewTask(getResources())) {
            if (mLocation.isListView()) {
                startActivity(new Intent(this, TaskListActivity.class));
            } else {
                startActivity(new Intent(this, TaskViewActivity.class));
            }
            finish();
        }
    }

    protected void checkActivity() {
        // check after super.onCreate to make sure location is set
    }



}
