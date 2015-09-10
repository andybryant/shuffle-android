package org.dodgybits.shuffle.android.list.view.context;

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
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SingleSelector;
import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.ContextTaskCountLoadedEvent;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LoadCountCursorEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.ContextPersister;
import org.dodgybits.shuffle.android.core.view.AbstractSwipeItemTouchHelperCallback;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateContextActiveEvent;
import org.dodgybits.shuffle.android.list.event.UpdateContextDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.list.view.EntityListItem;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;

import java.util.List;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

public class ContextListFragment extends RoboFragment {
    private static final String TAG = "ContextListFragment";

    private static Bitmap sInactiveIcon;
    private static Bitmap sActiveIcon;
    private static Bitmap sDeleteIcon;

    @Inject
    private ContextPersister mContextPersister;

    @Inject
    private ContextScopedProvider<EntityListItem> mEntityListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private ContextListAdapter mListAdapter;
    private MultiSelector mMultiSelector = new SingleSelector();
    private ActionMode mActionMode = null;
    private ModalMultiSelectorCallback mEditMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onCreateActionMode(android.support.v7.view.ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.context_list_context_menu, menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            String entityName = getString(R.string.context_name);
            List<Integer> positions = mMultiSelector.getSelectedPositions();
            boolean isDeleted = false;
            boolean isActive = true;
            if (positions != null && !positions.isEmpty() && mCursor != null) {
                int position = positions.get(0);
                Context context = mListAdapter.readContext(position);
                isDeleted = context.isDeleted();
                isActive = context.isActive();
            }

            MenuItem deleteMenu = menu.findItem(R.id.action_delete);
            deleteMenu.setVisible(!isDeleted);
            deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));

            MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
            undeleteMenu.setVisible(isDeleted);
            undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));

            MenuItem activeMenu = menu.findItem(R.id.action_active);
            activeMenu.setVisible(!isActive);

            MenuItem inactiveMenu = menu.findItem(R.id.action_inactive);
            inactiveMenu.setVisible(isActive);

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

                case R.id.action_active:
                    mEventManager.fire(new UpdateContextActiveEvent(contextId, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_inactive:
                    mEventManager.fire(new UpdateContextActiveEvent(contextId, false));
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

        Resources r = getActivity().getResources();
        sActiveIcon = BitmapFactory.decodeResource(r, R.drawable.ic_visibility_white_24dp);
        sInactiveIcon = BitmapFactory.decodeResource(r, R.drawable.ic_visibility_off_white_24dp);
        sDeleteIcon = BitmapFactory.decodeResource(r, R.drawable.ic_delete_white_24dp);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mListAdapter = new ContextListAdapter();
        mListAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mListAdapter);

        // init swipe to dismiss logic
        ItemTouchHelper helper = new ItemTouchHelper(new ContextCallback());
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

        Log.d(TAG, "Swapping adapter cursor");
        mCursor = cursor;
        mListAdapter.changeCursor(cursor);
        refreshChildCount();
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
            View.OnClickListener, View.OnLongClickListener,
            EntityListItem.OnClickListener {

        EntityListItem mContextListItem;
        Context mContext;

        public ContextHolder(EntityListItem contextListItem) {
            super(contextListItem, mMultiSelector);

            mContextListItem = contextListItem;
            mContextListItem.setOnClickListener(this);
            mContextListItem.setLongClickable(true);
            mContextListItem.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            clickPanel();
        }

        private void clickPanel() {
            if (mContext != null) {
                Location location = Location.viewTaskList(ListQuery.context, Id.NONE, mContext.getLocalId());
                mEventManager.fire(new NavigationRequestEvent(location));
            }
        }

        @Override
        public void clickSelector() {
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


        public void bindContext(Context context, SparseIntArray taskCountArray) {
            mContext = context;
            mContextListItem.updateView(context, taskCountArray);
        }

        @Override
        public boolean onLongClick(View v) {
            clickSelector();
            return true;
        }

    }

    public class ContextListAdapter extends AbstractCursorAdapter<ContextHolder> {

        private SparseIntArray mTaskCountArray;

        @Override
        public ContextHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            EntityListItem listItem = mEntityListItemProvider.get(getActivity());
            ContextHolder contextHolder = new ContextHolder(listItem);
            listItem.setClickListener(contextHolder);
            return contextHolder;
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

    public class ContextCallback extends AbstractSwipeItemTouchHelperCallback {

        public ContextCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);

            setNegativeColor(getResources().getColor(R.color.delete_background));
            setNegativeIcon(sDeleteIcon);
            setPositiveColor(getResources().getColor(R.color.active_background));
            setPositiveIcon(sInactiveIcon);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // callback for drag-n-drop, false to skip this feature
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            ContextHolder holder = (ContextHolder) viewHolder;
            Context context = holder.mContext;
            Id id = Id.create(viewHolder.getItemId());
            if (direction == ItemTouchHelper.LEFT) {
                Log.d(TAG, "Toggling active for context id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateContextActiveEvent(id, !context.isActive()));
            } else {
                Log.d(TAG, "Toggling delete for context id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateContextDeletedEvent(id, !context.isDeleted()));
            }

        }


    }

}