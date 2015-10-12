package org.dodgybits.shuffle.android.list.view.task;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.*;
import android.widget.CompoundButton;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.DefaultEntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.AbstractSwipeItemTouchHelperCallback;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.EntityPickerDialogHelper;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.editor.activity.DateTimePickerActivity;
import org.dodgybits.shuffle.android.list.activity.TaskListActivity;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;
import org.dodgybits.shuffle.android.list.view.AbstractListAdapter;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.preference.model.ListFeatures;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

import java.util.*;
import static org.dodgybits.shuffle.android.core.util.ObjectUtils.compareInts;
import static org.dodgybits.shuffle.android.core.util.ObjectUtils.compareLongs;

public class TaskRecyclerFragment extends RoboFragment {
    private static final String TAG = "TaskRecyclerFragment";

    private static final String SELECTED_ITEMS = "TaskRecyclerFragment.selected";
    private static final String MOVE_TOGGLE = "TaskRecyclerFragment.moveToggle";

    private static final int DEFERRED_CODE = 102;

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

    @Inject
    private DefaultEntityCache<Project> mProjectCache;

    private Location mLocation;

    private boolean mMoveEnabled = false;

    private ItemTouchHelper mItemTouchHelper;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private RecyclerViewDragDropManager mRecyclerViewDragDropManager;
    private TaskListAdapter mListAdapter;
    private RecyclerView.Adapter mWrappedAdapter;

    private MultiSelector mMultiSelector = new MultiSelector();
    private ActionMode mActionMode = null;
    private int mDeferredPosition = -1;
    private TaskCallback mTaskCallback;
    private ModalMultiSelectorCallback mEditMode = new TaskModalMultiSelectorCallback(mMultiSelector);

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
        return inflater.inflate(R.layout.recycler_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());

        // drag & drop manager
        mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
        mRecyclerViewDragDropManager.setDraggingItemShadowDrawable(
                (NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z3_9));

        mRecyclerView.setHasFixedSize(true);
        mListAdapter = new TaskListAdapter();
        mListAdapter.setHasStableIds(true);
        mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mListAdapter);      // wrap for dragging

        final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();

        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mWrappedAdapter);
        mRecyclerView.setItemAnimator(animator);

        if (!UiUtilities.supportsViewElevation()) {
            mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1_9)));
        }
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);

        // init swipe
        mTaskCallback = new TaskCallback();
        mItemTouchHelper = new ItemTouchHelper(mTaskCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateCursor();

        if (mMultiSelector != null) {
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(SELECTED_ITEMS));
            }

            if (mMultiSelector.isSelectable()) {
                if (mEditMode != null) {
                    mEditMode.setClearOnPrepare(false);
                    mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                    mActionMode.invalidate();
                }
            }
        }

        if (savedInstanceState != null) {
            updateMoveEnabled(savedInstanceState.getBoolean(MOVE_TOGGLE, false));
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem moveMenu = menu.findItem(R.id.move_toggle);
        if (moveMenu != null) {
            final CompoundButton moveSwitch = (CompoundButton) moveMenu.getActionView();
            moveSwitch.setChecked(mMoveEnabled);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Saving state");
        outState.putBundle(SELECTED_ITEMS, mMultiSelector.saveSelectionStates());
        outState.putBoolean(MOVE_TOGGLE, mMoveEnabled);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case DEFERRED_CODE:
                TaskHolder holder = (TaskHolder) mRecyclerView.findViewHolderForAdapterPosition(mDeferredPosition);
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long deferred = data.getLongExtra(DateTimePickerActivity.DATETIME_VALUE, 0L);
                        Task.Builder builder = Task.newBuilder().mergeFrom(holder.mTask)
                                .setStartDate(deferred);
                        builder.getChangeSet().showFromChanged();
                        mTaskPersister.update(builder.build());
                    }
                } else {
                    mItemTouchHelper.startSwipe(holder);
                }
                break;

            default:
                Log.e(TAG, "Unknown requestCode: " + requestCode);
        }
    }

    @Override
    public void onPause() {
        mRecyclerViewDragDropManager.cancelDrag();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mRecyclerViewDragDropManager != null) {
            mRecyclerViewDragDropManager.release();
            mRecyclerViewDragDropManager = null;
        }

        if (mRecyclerView != null) {
            mRecyclerView.setItemAnimator(null);
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }

        if (mWrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(mWrappedAdapter);
            mWrappedAdapter = null;
        }
        mListAdapter = null;

        super.onDestroyView();
    }

    private TaskListActivity getTaskListActivity() {
        return (TaskListActivity) getActivity();
    }

    private void onViewUpdate(@Observes LocationUpdatedEvent event) {
        onViewUpdate(event.getLocation());
    }

    private void onCacheUpdated(@Observes CacheUpdatedEvent event) {
        mListAdapter.notifyDataSetChanged();
    }

    private void onViewUpdate(Location location) {
        if (!ObjectUtils.equals(location, mLocation)) {
            mLocation = location;

            updateCursor();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
        updateSwipeSupport();
    }

    private void onMoveEnabledChange(@Observes MoveEnabledChangeEvent event) {
        boolean enabled = event.isEnabled();
        updateMoveEnabled(enabled);
    }

    private void updateMoveEnabled(boolean enabled) {
        if (enabled != mMoveEnabled) {
            mMoveEnabled = enabled;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            updateSwipeSupport();
        }
    }


    private void updateSwipeSupport() {
        if (mTaskCallback != null && mLocation != null) {
            mTaskCallback.setDefaultSwipeDirs(
                    ListFeatures.isSwipeSupported(mLocation) && !mMoveEnabled ?
                            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT : 0);

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
            View.OnClickListener, View.OnLongClickListener,
            DraggableItemViewHolder {

        private int mDragStateFlags;
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
            if (mMoveEnabled) return;
            clickPanel();
        }

        private void clickPanel() {
            if (mTask != null) {
                Location location;
                if (UiUtilities.showListOnViewTask(getResources())) {
                    location = Location.viewTask(mLocation, getAdapterPosition());
                } else {
                    location = Location.editTask(mTask.getLocalId());
                }
                mEventManager.fire(new NavigationRequestEvent(location));
            }
        }

        public void clickTag() {
            if (mMoveEnabled) return;
            if (mActionMode == null) {
                mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
                mMultiSelector.setSelected(this, true);
                mActionMode.invalidate();
            } else {
                if (mMultiSelector.tapSelection(this)) {
                    if (mMultiSelector.getSelectedPositions().isEmpty()) {
                        mActionMode.finish();
                    } else {
                        mActionMode.invalidate();
                    }
                } else {
                    clickPanel();
                }
            }
        }

        public void bindTask(Task task, boolean moveEnabled) {
            mTask = task;
            boolean projectVisible = ListFeatures.isProjectNameVisible(mLocation);
            boolean isSelected = mLocation.getSelectedIndex() == getAdapterPosition();
            boolean isDraggable = ((getDragStateFlags() & RecyclerViewDragDropManager.STATE_FLAG_IS_IN_RANGE) != 0);
            boolean isDragging = ((getDragStateFlags() & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0);
            mTaskListItem.updateItem(task, projectVisible, moveEnabled, isDraggable, isDragging, isSelected);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mMoveEnabled) return false;
            clickTag();
            return true;
        }

        @Override
        public void setDragStateFlags(int flags) {
            mDragStateFlags = flags;
        }

        @Override
        public int getDragStateFlags() {
            return mDragStateFlags;
        }

    }

    public class TaskListAdapter extends AbstractListAdapter<TaskHolder, Task>
            implements DraggableItemAdapter<TaskHolder> {

        public TaskListAdapter() {
            this.mPersister = mTaskPersister;
        }

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskListItem listItem = mTaskListItemProvider.get(getActivity());
            TaskHolder taskHolder = new TaskHolder(listItem);
            listItem.setHolder(taskHolder);
            return taskHolder;
        }

        @Override
        public void onBindViewHolder(TaskHolder holder, int position) {
            Task task = readTask(position);
            if (task != null && mLocation != null) {
                holder.bindTask(task, canMoveItem(position));
            }
        }

        private boolean canMoveItem(int position) {
            if (!mMoveEnabled) return false;

            // can only move when there's more than one item with the same project (or lack of project)
            Id projectId = readTask(position).getProjectId();

            // check above first (assumes sorted by project)
            if (position > 0 && projectId.equals(readTask(position - 1).getProjectId())) return true;

            // check below
            return (!isLast(position) && projectId.equals(readTask(position + 1).getProjectId()));
        }

        @Override
        protected void sortItems() {
            Collections.sort(mItems, projectOrderDueCreatedComparator);
        }

        public Task readTask(int position) {
            return mItems.get(position);
        }

        private boolean isLast(int position) {
            return position == mItems.size() - 1;
        }

        @Override
        public boolean onCheckCanStartDrag(TaskHolder holder, int position, int x, int y) {
            // x, y --- relative from the itemView's top-left
            if (!canMoveItem(position)) return false;

            final int dragRight = holder.mTaskListItem.getDragRight();
            return x <= dragRight;
        }

        @Override
        public ItemDraggableRange onGetItemDraggableRange(TaskHolder holder, int position) {
            final int start = findFirstSectionItem(position);
            final int end = findLastSectionItem(position);

            return new ItemDraggableRange(start, end);
        }

        private int findFirstSectionItem(int position) {
            Id projectId = readTask(position).getProjectId();

            while (position > 0) {
                Id prevProjectId = readTask(position - 1).getProjectId();

                if (!projectId.equals(prevProjectId)) {
                    break;
                }

                position -= 1;
            }

            return position;
        }

        private int findLastSectionItem(int position) {
            Id projectId = readTask(position).getProjectId();

            final int lastIndex = getItemCount() - 1;

            while (position < lastIndex) {
                Id prevProjectId = readTask(position + 1).getProjectId();

                if (!projectId.equals(prevProjectId)) {
                    break;
                }

                position += 1;
            }

            return position;
        }

        @Override
        public void onMoveItem(int fromPosition, int toPosition) {
            Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");
            if (fromPosition == toPosition) return;

            Task tmp = mItems.remove(fromPosition);
            mItems.add(toPosition, tmp);

            mTaskPersister.moveTaskWithinProject(fromPosition, toPosition, mCursor);

            notifyItemMoved(fromPosition, toPosition);
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
                mDeferredPosition = viewHolder.getAdapterPosition();
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(DateTimePickerActivity.TYPE);
                intent.putExtra(DateTimePickerActivity.DATETIME_VALUE, task.getStartDate());
                intent.putExtra(DateTimePickerActivity.TITLE, getString(R.string.title_deferred_picker));
                startActivityForResult(intent, DEFERRED_CODE);
            } else {
                Log.d(TAG, "Toggling complete on task id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateTasksCompletedEvent(id, !task.isComplete()));
            }

        }


    }

    private class TaskModalMultiSelectorCallback extends
            ModalMultiSelectorCallback implements
            EntityPickerDialogHelper.OnEntitiesSelected {
        private MenuItem mMarkComplete;
        private MenuItem mMarkIncomplete;
        private MenuItem mMarkDelete;
        private MenuItem mMarkUndelete;

        public TaskModalMultiSelectorCallback(MultiSelector multiSelector) {
            super(multiSelector);
        }

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

                case R.id.action_update_contexts:
                    getTaskListActivity().setSelectionHandler(this);
                    getTaskListActivity().showContextPicker();
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

        @Override
        public List<Id> getInitialSelection() {
            Set<Id> contextIds = Sets.newHashSet();
            List<Integer> selectedPositions = getMultiSelector().getSelectedPositions();
            for (Integer position : selectedPositions) {
                mCursor.moveToPosition(position);
                contextIds.addAll(mTaskPersister.readContextIds(mCursor));
            }
            return Lists.newArrayList(contextIds);
        }

        @Override
        public void onSelected(List<Id> selectedIds, Set<Id> modifiedIds) {
            List<Integer> selectedPositions = getMultiSelector().getSelectedPositions();
            for (Integer position : selectedPositions) {
                mCursor.moveToPosition(position);
                Task task = mTaskPersister.read(mCursor);
                Set<Id> updatedContexts = Sets.newHashSet(task.getContextIds());
                for (Id id : modifiedIds) {
                    if (selectedIds.contains(id)) {
                        updatedContexts.add(id);
                    } else {
                        updatedContexts.remove(id);
                    }
                }
                mTaskPersister.saveContextIds(task.getLocalId().getId(), Lists.newArrayList(updatedContexts));
                mListAdapter.notifyItemChanged(position);
            }

            mActionMode.finish();
        }

        @Override
        public void onCancel() {
            // nothing to do
        }


    };

    /**
     * Sort by following criteria: project order (no project last) asc,
     * display order asc, due date asc, created desc
     */
    private Comparator<Task> projectOrderDueCreatedComparator = new Comparator<Task>() {
        @Override
        public int compare(Task lhs, Task rhs) {
            int result;
            Project lhsProject = mProjectCache.findById(lhs.getProjectId());
            Project rhsProject = mProjectCache.findById(rhs.getProjectId());
            if (lhsProject != null) {
                if (rhsProject != null) {
                    result = compareInts(lhsProject.getOrder(), rhsProject.getOrder());
                } else {
                    return -1;
                }
            } else {
                if (rhsProject != null) {
                    return 1;
                } else {
                    result = 0;
                }
            }

            if (result == 0) {
                result = compareInts(lhs.getOrder(), rhs.getOrder());
                if (result == 0) {
                    result = compareLongs(lhs.getDueDate(), rhs.getDueDate());
                    if (result == 0) {
                        result = compareLongs(rhs.getCreatedDate(), lhs.getCreatedDate());
                    }
                }
            }
            return result;
        }
    };


}