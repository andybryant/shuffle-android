package org.dodgybits.shuffle.android.list.view.context;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.view.*;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.*;
import android.view.ActionMode;
import android.widget.AdapterView;
import android.widget.ListView;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SingleSelector;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateContextDeletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.list.view.project.ProjectListItem;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.fragment.RoboListFragment;
import roboguice.inject.ContextScopedProvider;

import java.util.List;

public class ContextListFragment extends RoboFragment {
    private static final String TAG = "ContextListFragment";


    @Inject
    private TaskPersister mTaskPersister;
    
    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private ContextScopedProvider<ContextListItem> mContextListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private ContextListAdapter mListAdapter;
    private MultiSelector mMultiSelector = new SingleSelector();
    private android.support.v7.view.ActionMode mActionMode = null;
    private ModalMultiSelectorCallback mEditMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onCreateActionMode(android.support.v7.view.ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.context_list_context_menu, menu);

            String entityName = getString(R.string.context_name);
            List<Integer> positions = mMultiSelector.getSelectedPositions();
            boolean isDeleted = false;
            if (positions != null && !positions.isEmpty() && mCursor != null) {
                int position = positions.get(0);
                Context context = mListAdapter.readContext(position);
                isDeleted = context.isDeleted();
            }

            MenuItem deleteMenu = menu.findItem(R.id.action_delete);
            deleteMenu.setVisible(!isDeleted);
            deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));

            MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
            undeleteMenu.setVisible(isDeleted);
            undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));

            return true;
        }

        @Override
        public boolean onActionItemClicked(android.support.v7.view.ActionMode actionMode, MenuItem menuItem) {
            int position = mMultiSelector.getSelectedPositions().get(0);
            Id contextId = mListAdapter.readContext(position).getLocalId();

            switch (menuItem.getItemId()) {
                case R.id.action_edit:
                    mEventManager.fire(new NavigationRequestEvent(
                            Location.editContext(contextId)));
                    mActionMode.finish();
                    return true;

                case R.id.action_delete:
                    mEventManager.fire(new UpdateContextDeletedEvent(contextId, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_undelete:
                    mEventManager.fire(new UpdateContextDeletedEvent(contextId, false));
                    mActionMode.finish();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(android.support.v7.view.ActionMode actionMode) {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mListAdapter = new ContextListAdapter();
        mRecyclerView.setAdapter(mListAdapter);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateCursor();
        getView().findViewById(R.id.fab).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Location location = Location.newContext();
                        mEventManager.fire(new NavigationRequestEvent(location));
                    }
                }
        );

        if (mMultiSelector != null) {
            if (savedInstanceState != null) {
                mMultiSelector.restoreSelectionStates(savedInstanceState.getBundle(TAG));
            }

            if (mMultiSelector.isSelectable()) {
                if (mEditMode != null) {
                    mEditMode.setClearOnPrepare(false);
                    mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
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

    @Override
    public void onResume() {
        super.onResume();

        refreshChildCount();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) return super.onContextItemSelected(item);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_edit:
                Location location = Location.editContext(Id.create(info.id));
                mEventManager.fire(new NavigationRequestEvent(location));
                return true;
            case R.id.action_delete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), true));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.CONTEXT_LIST));
                return true;
            case R.id.action_undelete:
                mEventManager.fire(new UpdateContextDeletedEvent(Id.create(info.id), false));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.CONTEXT_LIST));
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void onViewLoaded(@Observes LocationUpdatedEvent event) {
        updateCursor();
    }

    private void onCursorUpdated(@Observes CursorUpdatedEvent event) {
        updateCursor();
    }

    private void updateCursor() {
        updateCursor(mCursorProvider.getContextListCursor());
    }

    private void updateCursor(Cursor cursor) {
        if (cursor == null || cursor == mCursor) {
            return;
        }
        if (getActivity() == null) {
            Log.w(TAG, "Activity not set on " + this);
            return;
        }

        Log.d(TAG, "Swapping cursor and setting adapter");
        mCursor = cursor;
        mListAdapter.changeCursor(cursor);
    }

    private void onTaskCountCursorLoaded(@Observes ContextTaskCountLoadedEvent event) {
        mListAdapter.setTaskCountArray(event.getTaskCountArray());
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    protected RoboAppCompatActivity getRoboAppCompatActivity() {
        return (RoboAppCompatActivity) getActivity();
    }

    private void refreshChildCount() {
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.CONTEXT_LIST));
    }

    public class ContextHolder extends SelectableHolderImpl implements
            View.OnClickListener, View.OnLongClickListener {

        ContextListItem mContextListItem;
        Context mContext;

        public ContextHolder(ContextListItem contextListItem) {
            super(contextListItem, mMultiSelector);

            mContextListItem = contextListItem;
            mContextListItem.setOnClickListener(this);
            mContextListItem.setLongClickable(true);
            mContextListItem.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (!mMultiSelector.tapSelection(this)) {
                if (mContext != null) {
                    Location location = Location.viewTaskList(ListQuery.context, Id.NONE, mContext.getLocalId());
                    mEventManager.fire(new NavigationRequestEvent(location));
                }
            } else {
                if (mMultiSelector.getSelectedPositions().isEmpty() && mActionMode != null) {
                    mActionMode.finish();
                }
            }
        }

        public void bindContext(Context context, SparseIntArray taskCountArray) {
            mContext = context;
            mContextListItem.setTaskCountArray(taskCountArray);
            mContextListItem.updateView(context);
        }

        @Override
        public boolean onLongClick(View v) {
            mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
            mMultiSelector.setSelected(this, true);
            return true;
        }

    }

    public class ContextListAdapter extends AbstractCursorAdapter<ContextHolder> {

        private SparseIntArray mTaskCountArray;

        @Override
        public ContextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ContextListItem listItem = mContextListItemProvider.get(getActivity());
            return new ContextHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ContextHolder holder, int position) {
            Log.d(TAG, "Binding holder at " + position);
            Context context = readContext(position);
            holder.bindContext(context, mTaskCountArray);
        }

        public Context readContext(int position) {
            mCursor.moveToPosition(position);
            return mContextPersister.read(mCursor);
        }

        public void setTaskCountArray(SparseIntArray taskCountArray) {
            mTaskCountArray = taskCountArray;
        }
    }
}