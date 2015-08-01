package org.dodgybits.shuffle.android.view.activity;

import android.content.Intent;
import android.os.Bundle;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;

public class TaskViewActivity extends AbstractMainActivity {

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        checkActivity();
        super.onCreate(savedState);
    }

    private void checkActivity() {
        if (!UiUtilities.hideListOnViewTask(getResources())) {
            startActivity(new Intent(this, TaskListViewActivity.class));
            finish();
        }
    }


}
