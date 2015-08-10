package org.dodgybits.shuffle.android.list.view.task;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.content.TaskCursorLoader;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.util.CollectionUtils;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import roboguice.inject.ContextScopedProvider;

import java.util.HashSet;
import java.util.Set;

public class TaskListAdaptor extends CursorAdapter {
    private static final String TAG = "TaskListAdaptor";

    private static final String STATE_CHECKED_ITEMS =
            "org.dodgybits.shuffle.android.list.view.task.TaskListAdaptor.checkedItems";
    private static final String SHOW_PROJECT_NAMES =
            "org.dodgybits.shuffle.android.list.view.task.TaskListAdaptor.showProjectNames";

    /**
     * Set of selected task IDs.
     */
    private final HashSet<Long> mSelectedSet = new HashSet<Long>();

    private final TaskPersister mPersister;

    private final ContextScopedProvider<TaskListItem> mTaskListItemProvider;

    private boolean mProjectNameVisible = true;


    /**
     * Callback from TaskListAdaptor.  All methods are called on the UI thread.
     */
    public interface Callback {
        /** Called when the user selects/unselects a task */
        void onAdapterSelectedChanged(TaskListItem itemView, boolean newSelected,
                                      int mSelectedCount);

        /** Called when previously selected tasks are no longer in the list */
        void onAdaptorSelectedRemoved(Set<Long> removedIds);
    }

    private Callback mCallback;

    @Inject
    public TaskListAdaptor(Context context, TaskPersister persister,
                           ContextScopedProvider<TaskListItem> taskListItemProvider) {
        super(context, null, 0 /* no auto requery */);
        mPersister = persister;
        mTaskListItemProvider = taskListItemProvider;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setProjectNameVisible(boolean projectNameVisible) {
        this.mProjectNameVisible = projectNameVisible;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putLongArray(STATE_CHECKED_ITEMS, CollectionUtils.toPrimitiveLongArray(getSelectedSet()));
        outState.putBoolean(SHOW_PROJECT_NAMES, mProjectNameVisible);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor oldCursor = super.swapCursor(newCursor);
        return oldCursor;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        checkSelection();
    }

    public void loadState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(STATE_CHECKED_ITEMS)) {
            long[] checkedIds = savedInstanceState.getLongArray(STATE_CHECKED_ITEMS);
            Set<Long> checkedSet = getSelectedSet();
            checkedSet.clear();
            for (long l : checkedIds) {
                checkedSet.add(l);
            }
        }
        mProjectNameVisible = savedInstanceState.getBoolean(SHOW_PROJECT_NAMES, false);
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedSet() {
        return mSelectedSet;
    }
    
    public boolean isFirstTaskSelected() {
        boolean selected = false;
        Cursor c = getCursor();
        if (!getSelectedSet().isEmpty() && c != null && !c.isClosed() && c.moveToFirst()) {
            long id = c.getLong(mRowIDColumn);
            if (getSelectedSet().contains(id)) {
                selected = true;
            }
        }
        return selected;
    }
    
    public boolean isLastTaskSelected() {
        boolean selected = false;
        Cursor c = getCursor();
        if (!getSelectedSet().isEmpty() && c != null && !c.isClosed() && c.moveToLast()) {
            long id = c.getLong(mRowIDColumn);
            if (getSelectedSet().contains(id)) {
                selected = true;
            }
        }
        return selected;
    }

    /**
     * Clear the selection.  It's preferable to calling {@link Set#clear()} on
     * {@link #getSelectedSet()}, because it also notifies observers.
     */
    public void clearSelection() {
        Set<Long> checkedSet = getSelectedSet();
        if (checkedSet.size() > 0) {
            checkedSet.clear();
            notifyDataSetChanged();
        }
    }

    public boolean isSelected(TaskListItem itemView) {
        return getSelectedSet().contains(itemView.mTaskId);
    }

    /**
     * After the data has changed, make sure all selected items are still present.
     */
    private void checkSelection() {
        boolean selectedUpdated = false;
        Set<Long> removedIds = Sets.newHashSet(getSelectedSet());
        Cursor c = getCursor();
        if (!removedIds.isEmpty() && c != null && !c.isClosed()) {
            c.moveToPosition(-1);
            while (c.moveToNext()) {
                long id = c.getLong(mRowIDColumn);
                removedIds.remove(Long.valueOf(id));
            }
            selectedUpdated = !removedIds.isEmpty();
            if (selectedUpdated && Log.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "Removed following task ids from selection " + removedIds);
            }
        }
        if (selectedUpdated && mCallback != null) {
            getSelectedSet().removeAll(removedIds);
            mCallback.onAdaptorSelectedRemoved(removedIds);
        }
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Reset the view (in case it was recycled) and prepare for binding
        TaskListItem itemView = (TaskListItem) view;
//        itemView.bindViewInit(this);

        Task task = mPersister.read(cursor);
        itemView.setTask(task, mProjectNameVisible);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        TaskListItem item = mTaskListItemProvider.get(context);
        item.setVisibility(View.VISIBLE);
        return item;
    }

    public void toggleSelected(TaskListItem itemView) {
        updateSelected(itemView, !isSelected(itemView));
    }

    /**
     * This is used as a callback from the list items, to set the selected state
     *
     * <p>Must be called on the UI thread.
     *
     * @param itemView the item being changed
     * @param newSelected the new value of the selected flag (checkbox state)
     */
    private void updateSelected(TaskListItem itemView, boolean newSelected) {
        if (newSelected) {
            mSelectedSet.add(itemView.mTaskId);
        } else {
            mSelectedSet.remove(itemView.mTaskId);
        }
        if (mCallback != null) {
            mCallback.onAdapterSelectedChanged(itemView, newSelected, mSelectedSet.size());
        }
    }

}
