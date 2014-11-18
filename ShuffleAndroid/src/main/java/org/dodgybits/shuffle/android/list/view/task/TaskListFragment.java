package org.dodgybits.shuffle.android.list.view.task;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.LoadTaskFragmentEvent;
import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.event.*;
import org.dodgybits.shuffle.android.list.view.QuickAddController;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import roboguice.activity.RoboActionBarActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboListFragment;

import java.util.Set;

public class TaskListFragment extends RoboListFragment
        implements AdapterView.OnItemLongClickListener, TaskListAdaptor.Callback {
    private static final String TAG = "TaskListFragment"; 
    
    /** Argument name(s) */
    public static final String TASK_LIST_CONTEXT = "listContext";

    private static final String BUNDLE_LIST_STATE = "taskListFragment.state.listState";
    private static final String BUNDLE_KEY_SELECTED_TASK_ID
            = "taskListFragment.state.listState.selected_task_id";

    private static final String SELECTED_ITEM = "SELECTED_ITEM";

    // result codes
    private static final int FILTER_CONFIG = 600;
    
    private boolean mShowMoveActions = false;

    /** ID of the message to highlight. */
    private long mSelectedTaskId = -1;

    @Inject
    private TaskListAdaptor mListAdapter;

    @Inject
    private EventManager mEventManager;

    private boolean mIsFirstLoad;

    /**
     * {@link ActionMode} shown when 1 or more message is selected.
     */
    private ActionMode mSelectionMode;
    private SelectionModeCallback mLastSelectionModeCallback;

    // UI Support
    private boolean mIsViewCreated;

    /** true between {@link #onResume} and {@link #onPause}. */
    private boolean mResumed;

    @Inject
    TaskPersister mPersister;

    @Inject
    EntityCache<org.dodgybits.shuffle.android.core.model.Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    private QuickAddController mQuickAddController;

    @Inject
    private CursorProvider mCursorProvider;

    private TaskListContext mListContext;

    /**
     * If <code>true</code>, we have restored (or attempted to restore) the list's scroll position
     * from when we were last on this conversation list.
     */
    private boolean mScrollPositionRestored = false;

    public TaskListFragment() {
        Log.d(TAG, "Created " + this);
    }


    protected RoboActionBarActivity getRoboActionBarActivity() {
        return (RoboActionBarActivity)getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mIsViewCreated = true;
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "+onActivityCreated");

        loadConfiguration(savedInstanceState);

        setHasOptionsMenu(true);

        mListAdapter.setProjectNameVisible(mListContext.isProjectNameVisible());
        mListAdapter.setCallback(this);
        mIsFirstLoad = true;

        final ListView lv = getListView();
        lv.setOnItemLongClickListener(this);
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        setEmptyText(getString(R.string.no_tasks));

        updateCursor();
    }

    private void loadConfiguration(Bundle savedInstanceState) {
        Bundle bundle = savedInstanceState == null ? getArguments() : savedInstanceState;
        mListContext = bundle.getParcelable(TASK_LIST_CONTEXT);
        mShowMoveActions = mListContext.showMoveActions();
        mListAdapter.loadState(bundle);
        mSelectedTaskId = bundle.getLong(BUNDLE_KEY_SELECTED_TASK_ID);
    }

    @Override
    public void onResume() {
        super.onResume();

        mResumed = true;
        onVisibilityChange();

        restoreLastScrolledPosition();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mIsViewCreated = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        onVisibilityChange();
    }

    @Override
    public void onPause() {
        super.onPause();
        mResumed = false;

        Log.d(TAG, "onPause with context " + getListContext());
        saveLastScrolledPosition();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // Always toggle the item.
        TaskListItem listItem = (TaskListItem) view;
        boolean toggled = false;
        if (!mListAdapter.isSelected(listItem)) {
            toggleSelection(listItem);
            toggled = true;
        }

        return toggled;
    }

    /**
     * Called when a message is clicked.
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        Uri url = ContentUris.withAppendedId(TaskProvider.Tasks.CONTENT_URI, id);

        String action = getActivity().getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)
                || Intent.ACTION_GET_CONTENT.equals(action)) {
            // The caller is waiting for us to return a task selected by
            // the user. They have clicked on one, so return it now.
            Bundle bundle = new Bundle();
            bundle.putString(SELECTED_ITEM, url.toString());
            Intent intent = new Intent();
            intent.putExtras(bundle);
            getActivity().setResult(Activity.RESULT_OK, intent);
        } else {
            MainView mainView = MainView.createTaskView(mListContext, position);
            mEventManager.fire(new MainViewUpdateEvent(mainView));
        }
    }

    @Override
    public void onAdapterSelectedChanged(TaskListItem itemView, boolean newSelected, int mSelectedCount) {
        updateSelectionMode();
    }

    @Override
    public void onAdaptorSelectedRemoved(Set<Long> removedIds) {
        updateSelectionMode();
    }

    public void onViewUpdate(@Observes MainViewUpdateEvent event) {
        TaskListContext newListContext = TaskListContext.create(event.getMainView());
        if (!ObjectUtils.equals(mListContext, newListContext)) {
            mListContext = newListContext;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mListAdapter.onSaveInstanceState(outState);
        if (isViewCreated()) {
            outState.putParcelable(BUNDLE_LIST_STATE, getListView().onSaveInstanceState());
        }
        outState.putLong(BUNDLE_KEY_SELECTED_TASK_ID, mSelectedTaskId);
        outState.putParcelable(TASK_LIST_CONTEXT, mListContext);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        String taskName = getString(R.string.task_name);
        String addTitle = getString(R.string.menu_insert, taskName);
        menu.findItem(R.id.action_add).setTitle(addTitle);

        MenuItem editMenu = menu.findItem(R.id.action_edit);
        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
        MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
        if (getListContext().showEditActions()) {
            String entityName = getListContext().getEditEntityName(getActivity());
            boolean entityDeleted = getListContext().isEditEntityDeleted(getActivity(), mContextCache, mProjectCache);
            editMenu.setVisible(true);
            editMenu.setTitle(getString(R.string.menu_edit, entityName));
            deleteMenu.setVisible(!entityDeleted);
            deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));
            undeleteMenu.setVisible(entityDeleted);
            undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
        } else {
            editMenu.setVisible(false);
            deleteMenu.setVisible(false);
            undeleteMenu.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                Log.d(TAG, "adding task");
                mEventManager.fire(mListContext.createEditNewTaskEvent());
                return true;
            case R.id.action_help:
                Log.d(TAG, "Bringing up help");
                mEventManager.fire(new ViewHelpEvent(mListContext.getListQuery()));
                return true;
            case R.id.action_view_settings:
                Log.d(TAG, "Bringing up view settings");
                mEventManager.fire(new EditListSettingsEvent(mListContext.getListQuery(), this, FILTER_CONFIG));
                return true;
            case R.id.action_edit:
                mEventManager.fire(mListContext.createEditEvent());
                return true;
            case R.id.action_delete:
                mEventManager.fire(mListContext.createDeleteEvent(true));
                getActivity().finish();
                return true;
            case R.id.action_undelete:
                mEventManager.fire(mListContext.createDeleteEvent(false));
                getActivity().finish();
                return true;

        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case FILTER_CONFIG:
                mEventManager.fire(new ListSettingsUpdatedEvent(getListContext().getListQuery()));
                break;

            default:
                Log.e(TAG, "Unknown requestCode: " + requestCode);
        }
    }

    protected void onVisibilityChange() {
        if (getUserVisibleHint()) {
            flushCaches();
            updateTitle();
            updateQuickAdd();
        }
        updateSelectionMode();
    }

    public void onCursorLoaded(@Observes LoadTaskFragmentEvent event) {
        updateCursor();
    }

    private void updateCursor() {
        Log.d(TAG, "Swapping cursor " + this);
        // Update the list
        mListAdapter.swapCursor(mCursorProvider.getCursor());
        setListAdapter(mListAdapter);
        updateSelectionMode();

        // We want to make visible the selection only for the first load.
        // Re-load caused by content changed events shouldn't scroll the list.
        highlightSelectedMessage(mIsFirstLoad);

        mIsFirstLoad = false;
    }

    private void flushCaches() {
        mContextCache.flush();
        mProjectCache.flush();
    }

    private void updateTitle() {
        getListContext().updateTitle(((ActionBarActivity)getActivity()), mContextCache, mProjectCache);
    }

    public void onQuickAddEvent(@Observes QuickAddEvent event) {
        if (getUserVisibleHint() && mResumed) {
            mEventManager.fire(getListContext().createNewTaskEventWithDescription(event.getValue()));
        }
    }

    private void updateQuickAdd() {
        mQuickAddController.init(getActivity());
        mQuickAddController.setEnabled(getListContext().isQuickAddEnabled(getActivity()));
        mQuickAddController.setEntityName(getString(R.string.task_name));
    }


    public TaskListContext getListContext() {
        return mListContext;
    }

    private boolean showMoveActions() {
        return mShowMoveActions;
    }

    private boolean doesSelectionContainIncompleteMessage() {
        Set<Long> selectedSet = mListAdapter.getSelectedSet();
        return testMultiple(selectedSet, false, new EntryMatcher() {
            @Override
            public boolean matches(Cursor c) {
                return mPersister.readComplete(c);
            }
        });
    }

    private boolean doesSelectionContainUndeletedMessage() {
        Set<Long> selectedSet = mListAdapter.getSelectedSet();
        return testMultiple(selectedSet, false, new EntryMatcher() {
            @Override
            public boolean matches(Cursor c) {
                return mPersister.readDeleted(c);
            }
        });
    }

    interface EntryMatcher {
        boolean matches(Cursor c);
    }
    
    /**
     * Test selected messages for showing appropriate labels
     * @param selectedSet
     * @param matcher
     * @param defaultFlag
     * @return true when the specified flagged message is selected
     */
    private boolean testMultiple(Set<Long> selectedSet, boolean defaultFlag, EntryMatcher matcher) {
        final Cursor c = mListAdapter.getCursor();
        if (c == null || c.isClosed()) {
            return false;
        }
        c.moveToPosition(-1);
        while (c.moveToNext()) {
            long id = mPersister.readLocalId(c).getId();
            if (selectedSet.contains(Long.valueOf(id))) {
                if (matcher.matches(c) == defaultFlag) {
                    return true;
                }
            }
        }
        return false;
    }



    /**
     * @return true if the content view is created and not destroyed yet. (i.e. between
     * {@link #onCreateView} and {@link #onDestroyView}.
     */
    private boolean isViewCreated() {
        // Note that we don't use "getView() != null".  This method is used in updateSelectionMode()
        // to determine if CAB shold be shown.  But because it's called from onDestroyView(), at
        // this point the fragment still has views but we want to hide CAB, we can't use
        // getView() here.
        return mIsViewCreated;
    }


    private void toggleSelection(TaskListItem itemView) {
        itemView.invalidate();
        mListAdapter.toggleSelected(itemView);
    }

    private void onDeselectAll() {
        mListAdapter.clearSelection();
        if (isInSelectionMode()) {
            finishSelectionMode();
        }
    }
    /**
     * Show/hide the "selection" action mode, according to the number of selected messages and
     * the visibility of the fragment.
     * Also update the content (title and menus) if necessary.
     */
    public void updateSelectionMode() {
        final int numSelected = getSelectedCount();
        if ((numSelected == 0) || !isViewCreated() || !getUserVisibleHint()) {
            finishSelectionMode();
            return;
        }
        if (isInSelectionMode()) {
            updateSelectionModeView();
        } else {
            mLastSelectionModeCallback = new SelectionModeCallback();
            getRoboActionBarActivity().startSupportActionMode(mLastSelectionModeCallback);
        }
    }


    /**
     * Finish the "selection" action mode.
     *
     * Note this method finishes the contextual mode, but does *not* clear the selection.
     * If you want to do so use {@link #onDeselectAll()} instead.
     */
    private void finishSelectionMode() {
        if (isInSelectionMode()) {
            mLastSelectionModeCallback.mClosedByUser = false;
            mSelectionMode.finish();
        }
    }

    /** Update the "selection" action mode bar */
    private void updateSelectionModeView() {
        mSelectionMode.invalidate();
    }

    /**
     * @return the number of messages that are currently selected.
     */
    private int getSelectedCount() {
        return mListAdapter.getSelectedSet().size();
    }

    /**
     * @return true if the list is in the "selection" mode.
     */
    public boolean isInSelectionMode() {
        return mSelectionMode != null;
    }

    private class SelectionModeCallback implements ActionMode.Callback {
        private MenuItem mMarkComplete;
        private MenuItem mMarkIncomplete;
        private MenuItem mMarkDelete;
        private MenuItem mMarkUndelete;
        private MenuItem mMoveUp;
        private MenuItem mMoveDown;

        /* package */ boolean mClosedByUser = true;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mSelectionMode = mode;

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.task_cab_menu, menu);
            mMoveUp = menu.findItem(R.id.action_move_up);
            mMoveDown = menu.findItem(R.id.action_move_down);
            mMarkComplete = menu.findItem(R.id.action_mark_complete);
            mMarkIncomplete = menu.findItem(R.id.action_mark_incomplete);
            mMarkDelete = menu.findItem(R.id.action_delete);
            mMarkUndelete = menu.findItem(R.id.action_undelete);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int num = getSelectedCount();
            // Set title -- "# selected"
            mSelectionMode.setTitle(getActivity().getResources().getQuantityString(
                    R.plurals.task_view_selected_message_count, num, num));

            // Show appropriate menu items.
            boolean incompleteExists = doesSelectionContainIncompleteMessage();
            boolean undeletedExists = doesSelectionContainUndeletedMessage();
            mMoveUp.setVisible(showMoveActions());
            mMoveUp.setEnabled(showMoveActions() && moveUpEnabled());
            mMoveDown.setVisible(showMoveActions());
            mMoveDown.setEnabled(showMoveActions() && moveDownEnabled());
            mMarkComplete.setVisible(incompleteExists);
            mMarkIncomplete.setVisible(!incompleteExists);
            mMarkDelete.setVisible(undeletedExists);
            mMarkUndelete.setVisible(!undeletedExists);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Set<Long> selectedTasks = mListAdapter.getSelectedSet();
            if (selectedTasks.isEmpty()) return true;
            switch (item.getItemId()) {
                case R.id.action_move_up:
                    if (moveUpEnabled()) {
                        mEventManager.fire(new MoveTasksEvent(selectedTasks, true, mListAdapter.getCursor()));
                    }
                    break;
                case R.id.action_move_down:
                    if (moveDownEnabled()) {
                        mEventManager.fire(new MoveTasksEvent(selectedTasks, false, mListAdapter.getCursor()));
                    }
                    break;
                case R.id.action_mark_complete:
                    mEventManager.fire(new UpdateTasksCompletedEvent(selectedTasks, true));
                    break;
                case R.id.action_mark_incomplete:
                    mEventManager.fire(new UpdateTasksCompletedEvent(selectedTasks, false));
                    break;
                case R.id.action_delete:
                    mEventManager.fire(new UpdateTasksDeletedEvent(selectedTasks, true));
                    break;
                case R.id.action_undelete:
                    mEventManager.fire(new UpdateTasksDeletedEvent(selectedTasks, false));
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Clear this before onDeselectAll() to prevent onDeselectAll() from trying to close the
            // contextual mode again.
            mSelectionMode = null;
            if (mClosedByUser) {
                // Clear selection, only when the contextual mode is explicitly closed by the user.
                //
                // We close the contextual mode when the fragment becomes temporary invisible
                // (i.e. mIsVisible == false) too, in which case we want to keep the selection.
                onDeselectAll();
            }
        }

        private boolean moveUpEnabled() {
            return !mListAdapter.isFirstTaskSelected();
        }

        private boolean moveDownEnabled() {
            return !mListAdapter.isLastTaskSelected();
        }
}

    /**
     * Highlight the selected task.
     */
    private void highlightSelectedMessage(boolean ensureSelectionVisible) {
        if (!isViewCreated()) {
            return;
        }

        final ListView lv = getListView();
        if (mSelectedTaskId == -1) {
            // No task selected
            lv.clearChoices();
            return;
        }

        final int count = lv.getCount();
        for (int i = 0; i < count; i++) {
            if (lv.getItemIdAtPosition(i) != mSelectedTaskId) {
                continue;
            }
            lv.setItemChecked(i, true);
            if (ensureSelectionVisible) {
                UiUtilities.listViewSmoothScrollToPosition(getActivity(), lv, i);
            }
            break;
        }
    }

    private void saveLastScrolledPosition() {
        if (mListAdapter.getCursor() == null) {
            // If you save your scroll position in an empty list, you're gonna have a bad time
            return;
        }

        final Parcelable savedState = getListView().onSaveInstanceState();

       // TODO add back scroll position persistence

//        mActivity.getListHandler().setTaskListScrollPosition(
//                mListContext.getListQuery(), savedState);
    }

    private void restoreLastScrolledPosition() {
        // Scroll to our previous position, if necessary
//        if (!mScrollPositionRestored && mListContext != null) {
//            final ListQuery key = mListContext.getListQuery();
//            final Parcelable savedState = mActivity.getListHandler()
//                    .getTaskListScrollPosition(key);
//            if (savedState != null) {
//                getListView().onRestoreInstanceState(savedState);
//            }
//            mScrollPositionRestored = true;
//        }
    }


}
