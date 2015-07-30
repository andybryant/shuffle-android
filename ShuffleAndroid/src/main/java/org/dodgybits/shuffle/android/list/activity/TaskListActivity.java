package org.dodgybits.shuffle.android.list.activity;

import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.View;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;

import java.util.List;
import java.util.Set;

public class TaskListActivity extends AbstractMainActivity
        implements EntityPickerDialogHelper.OnEntitiesSelected {

    private EntityPickerDialogHelper.OnEntitiesSelected mSelectionHandler;

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskList;
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (mActionBarToolbar != null) {
            ViewCompat.setElevation(mActionBarToolbar, 0f);
            ViewCompat.setElevation((View)mActionBarToolbar.getParent(), 0f);
        }

    }

    @Override
    protected boolean handleBackPress() {
        if (mLocation != null && mLocation.getViewMode() == ViewMode.TASK && UiUtilities.isListCollapsible(getResources())) {
            mEventManager.fire(new NavigationRequestEvent(mLocation.parent()));
            return true;
        }
        return false;
    }

    @Override
    protected int contentView(boolean isTablet) {
        return R.layout.task_list_activity;
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
