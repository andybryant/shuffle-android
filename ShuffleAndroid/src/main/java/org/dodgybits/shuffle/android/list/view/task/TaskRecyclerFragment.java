package org.dodgybits.shuffle.android.list.view.task;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.*;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.AbstractSwipeItemTouchHelperCallback;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskRecyclerFragment extends RoboFragment {
    private static final String TAG = "TaskRecyclerFragment";

    private static Bitmap sCompleteIcon;
    private static Bitmap sDeferIcon;

    @Inject
    private TaskPersister mTaskPersister;

    @Inject
    private ContextScopedProvider<TaskListItem> mTaskListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    @Inject
    private LocationProvider mLocationProvider;

    private Location mLocation;

    private TaskListContext mTaskListContext;

    private boolean mEnableTaskReordering = false;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private TaskListAdapter mListAdapter;
    private MultiSelector mMultiSelector = new MultiSelector();
    private ActionMode mActionMode = null;

    private ModalMultiSelectorCallback mEditMode = new ModalMultiSelectorCallback(mMultiSelector) {
        private MenuItem mMarkComplete;
        private MenuItem mMarkIncomplete;
        private MenuItem mMarkDelete;
        private MenuItem mMarkUndelete;

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.task_list_context_menu, menu);

            mMarkComplete = menu.findItem(R.id.action_mark_complete);
            mMarkIncomplete = menu.findItem(R.id.action_mark_incomplete);
            mMarkDelete = menu.findItem(R.id.action_delete);
            mMarkUndelete = menu.findItem(R.id.action_undelete);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mActionMode != null) {
                int num = mMultiSelector.getSelectedPositions().size();
                mActionMode.setTitle(getActivity().getResources().getQuantityString(
                        R.plurals.task_view_selected_message_count, num, num));
            }

            // Show appropriate menu items.
            boolean incompleteExists = doesSelectionContainIncompleteTasks();
            boolean undeletedExists = doesSelectionContainUndeletedTasks();
            mMarkComplete.setVisible(incompleteExists);
            mMarkIncomplete.setVisible(!incompleteExists);
            mMarkDelete.setVisible(undeletedExists);
            mMarkUndelete.setVisible(!undeletedExists);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            Set<Long> taskIds = new HashSet<>(getSelectedIds());
            switch (menuItem.getItemId()) {

                case R.id.action_delete:
                    mEventManager.fire(new UpdateTasksDeletedEvent(taskIds, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_undelete:
                    mEventManager.fire(new UpdateTasksDeletedEvent(taskIds, false));
                    mActionMode.finish();
                    return true;

                case R.id.action_mark_complete:
                    mEventManager.fire(new UpdateTasksCompletedEvent(taskIds, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_mark_incomplete:
                    mEventManager.fire(new UpdateTasksCompletedEvent(taskIds, false));
                    mActionMode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            mMultiSelector.clearSelections();
            mActionMode = null;
        }
    };

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Resources r = getActivity().getResources();
        sCompleteIcon = BitmapFactory.decodeResource(r, R.drawable.ic_done_white_24dp);
        sDeferIcon = BitmapFactory.decodeResource(r, R.drawable.ic_schedule_white_24dp);
    }

    @Override
    public void onResume() {
        super.onResume();

        onViewUpdate(mLocationProvider.getLocation());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mListAdapter = new TaskListAdapter();
        mListAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mListAdapter);

        // init swipe to dismiss logic
        ItemTouchHelper helper = new ItemTouchHelper(new TaskCallback());
        helper.attachToRecyclerView(mRecyclerView);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateCursor();

        if (mMultiSelector != null) {
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(TAG));
            }

            if (mMultiSelector.isSelectable()) {
                if (mEditMode != null) {
                    mEditMode.setClearOnPrepare(false);
                    mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                    mActionMode.invalidate();
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Saving state");
        outState.putBundle(TAG, mMultiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    private void onViewUpdate(@Observes LocationUpdatedEvent event) {
        onViewUpdate(event.getLocation());
    }

    private void onViewUpdate(Location location) {
        if (!ObjectUtils.equals(location, mLocation)) {
            mLocation = location;
            mTaskListContext = TaskListContext.create(mLocation);

            mEnableTaskReordering = mTaskListContext.showMoveActions();
//            mSelectedTaskId = mLocation.getSelectedIndex();
            updateCursor();
        }
    }


    private List<Long> getSelectedIds() {
        return Lists.transform(mMultiSelector.getSelectedPositions(), new Function<Integer, Long>() {
            @Override
            public Long apply(Integer position) {
                mCursor.moveToPosition(position);
                return mTaskPersister.readLocalId(mCursor).getId();
            }
        });
    }

    private boolean doesSelectionContainIncompleteTasks() {
        List<Integer> positions = mMultiSelector.getSelectedPositions();
        Log.d(TAG, "Incomplete check for positions " + positions);
        return testMultiple(positions, false, new EntryMatcher() {
            @Override
            public boolean matches(Cursor c) {
                boolean complete = mTaskPersister.readComplete(c);
                Log.d(TAG, "Complete=" + complete);
                return complete;
            }
        });
    }

    private boolean doesSelectionContainUndeletedTasks() {
        List<Integer> positions = mMultiSelector.getSelectedPositions();
        return testMultiple(positions, false, new EntryMatcher() {
            @Override
            public boolean matches(Cursor c) {
                return mTaskPersister.readDeleted(c);
            }
        });
    }

    interface EntryMatcher {
        boolean matches(Cursor c);
    }

    /**
     * Test selected tasks for showing appropriate labels
     */
    private boolean testMultiple(List<Integer> positions, boolean defaultFlag, EntryMatcher matcher) {
        final Cursor c = mCursor;
        if (c == null || c.isClosed()) {
            return false;
        }
        for (Integer position : positions) {
            c.moveToPosition(position);
            if (matcher.matches(c) == defaultFlag) {
                return true;
            }
        }
        return false;
    }

    private void onViewLoaded(@Observes LocationUpdatedEvent event) {
        updateCursor();
    }

    private void onCursorUpdated(@Observes CursorUpdatedEvent event) {
        updateCursor();
    }

    private void updateCursor() {
        updateCursor(mCursorProvider.getTaskListCursor());
    }

    private void updateCursor(Cursor cursor) {
        if (cursor == null || mCursor == cursor) {
            return;
        }
        if (getActivity() == null) {
            Log.w(TAG, "Activity not set on " + this);
            return;
        }

        Log.d(TAG, "Swapping adapter cursor");
        mCursor = cursor;
        mListAdapter.changeCursor(cursor);
        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    protected RoboAppCompatActivity getRoboAppCompatActivity() {
        return (RoboAppCompatActivity) getActivity();
    }


    public class TaskHolder extends SelectableHolderImpl implements
            View.OnClickListener, View.OnLongClickListener {

        TaskListItem mTaskListItem;
        Task mTask;

        public TaskHolder(TaskListItem taskListItem) {
            super(taskListItem, mMultiSelector);

            mTaskListItem = taskListItem;
            mTaskListItem.setOnClickListener(this);
            mTaskListItem.setLongClickable(true);
            mTaskListItem.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (!mMultiSelector.tapSelection(this)) {
                if (mTask != null) {
                    Location location = Location.viewTask(mTaskListContext, getAdapterPosition());
                    mEventManager.fire(new NavigationRequestEvent(location));
                }
            } else if (mActionMode != null) {
                if (mMultiSelector.getSelectedPositions().isEmpty()) {
                    mActionMode.finish();
                } else {
                    mActionMode.invalidate();
                }
            }
        }

        public void bindTask(Task task) {
            mTask = task;
            boolean projectVisible = mTaskListContext == null || mTaskListContext.isProjectNameVisible();
            mTaskListItem.setTask(task, projectVisible);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mActionMode == null) {
                mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                mMultiSelector.setSelected(this, true);
                mActionMode.invalidate();
            } else {
                onClick(v);
            }
            return true;
        }
    }

    public class TaskListAdapter extends AbstractCursorAdapter<TaskHolder> {

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskListItem listItem = mTaskListItemProvider.get(getActivity());
            return new TaskHolder(listItem);
        }

        @Override
        public void onBindViewHolder(TaskHolder holder, int position) {
            Log.d(TAG, "Binding holder at " + position);
            Task task = readTask(position);
            if (task != null) {
                holder.bindTask(task);
            }
        }

        public Task readTask(int position) {
            if (mCursor.isClosed()) return null;
            mCursor.moveToPosition(position);
            return mTaskPersister.read(mCursor);
        }

    }

    public class TaskCallback extends AbstractSwipeItemTouchHelperCallback {

        public TaskCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

            setNegativeColor(getResources().getColor(R.color.complete_background));
            setNegativeIcon(sCompleteIcon);
            setPositiveColor(getResources().getColor(R.color.deferred));
            setPositiveIcon(sDeferIcon);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // callback for drag-n-drop, false to skip this feature
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            TaskHolder holder = (TaskHolder) viewHolder;
            Task task = holder.mTask;
            long id = viewHolder.getItemId();
            if (direction == ItemTouchHelper.LEFT) {
                Log.d(TAG, "Deferring task id " + id + " position=" + viewHolder.getAdapterPosition());
                // TODO
            } else {
                Log.d(TAG, "Toggling complete on task id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateTasksCompletedEvent(id, !task.isComplete()));
            }

        }


    }

}