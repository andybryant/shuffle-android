package org.dodgybits.shuffle.android.list.activity;

import android.util.Log;
import android.view.View;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.view.activity.TaskListViewActivity;
import org.dodgybits.shuffle.android.view.activity.TaskViewActivity;

import java.util.List;
import java.util.Set;

public class TaskListActivity extends AbstractMainActivity
        implements EntityPickerDialogHelper.OnEntitiesSelected {
    private static final String TAG = "TaskListActivity";


    private EntityPickerDialogHelper.OnEntitiesSelected mSelectionHandler;

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    protected void validateActivity() {
    }

    @Override
    protected void validateLocation(Location location) {
        if (!location.isListView()) {
            if (UiUtilities.showListOnViewTask(getResources())) {
                Log.d(TAG, "Switching to TaskListViewActivity");
                redirect(TaskListViewActivity.class);
            } else {
                Log.d(TAG, "Switching to TaskViewActivity");
                redirect(TaskViewActivity.class);
            }
        }
    }

    @Override
    protected boolean handleBackPress() {
        if (mLocation != null && mLocation.getViewMode() == ViewMode.TASK && !UiUtilities.showListOnViewTask(getResources())) {
            mEventManager.fire(new NavigationRequestEvent(mLocation.parent()));
            return true;
        }
        return false;
    }

    public void onClickFab(View view) {
        Location location = Location.newTaskFromTaskListContext(TaskListContext.create(mLocation));
        mEventManager.fire(new NavigationRequestEvent(location));
    }

    public void showContextPicker() {
        new EntityPickerDialogHelper.ContextPickerDialog()
                .show(getSupportFragmentManager(), "contexts");
    }

    public void setSelectionHandler(EntityPickerDialogHelper.OnEntitiesSelected selectionHandler) {
        mSelectionHandler = selectionHandler;
    }

    @Override
    public List<Id> getInitialSelection() {
        return  mSelectionHandler.getInitialSelection();
    }


    @Override
    public void onSelected(List<Id> selectedIds, Set<Id> modifiedIds) {
        mSelectionHandler.onSelected(selectedIds, modifiedIds);
    }

    @Override
    public void onCancel() {
        mSelectionHandler.onCancel();
    }
}
