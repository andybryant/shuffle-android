package org.dodgybits.shuffle.android.list.view.project;

import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.*;
import android.widget.AdapterView;
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SingleSelector;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

import java.util.List;

public class ProjectListFragment extends RoboFragment {
    private static final String TAG = "ProjectListFragment";

    private static Bitmap sCompleteIcon;
    private static Bitmap sDeferIcon;

    @Inject
    private TaskPersister mTaskPersister;

    @Inject
    private ProjectPersister mProjectPersister;

    @Inject
    private ContextScopedProvider<ProjectListItem> mProjectListItemProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    private Cursor mCursor;
    private RecyclerView mRecyclerView;
    private ProjectListAdapter mListAdapter;
    private MultiSelector mMultiSelector = new SingleSelector();
    private ActionMode mActionMode = null;
    private ModalMultiSelectorCallback mEditMode = new ModalMultiSelectorCallback(mMultiSelector) {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);
            getActivity().getMenuInflater().inflate(R.menu.project_list_context_menu, menu);

            String entityName = getString(R.string.project_name);
            List<Integer> positions = mMultiSelector.getSelectedPositions();
            boolean isDeleted = false;
            if (positions != null && !positions.isEmpty() && mCursor != null) {
                int position = positions.get(0);
                Project project = mListAdapter.readProject(position);
                isDeleted = project.isDeleted();
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
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int position = mMultiSelector.getSelectedPositions().get(0);
            Id projectId = mListAdapter.readProject(position).getLocalId();

            switch (menuItem.getItemId()) {
                case R.id.action_edit:
                    mEventManager.fire(new NavigationRequestEvent(
                            Location.editProject(projectId)));
                    mActionMode.finish();
                    return true;

                case R.id.action_delete:
                    mEventManager.fire(new UpdateProjectDeletedEvent(projectId, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_undelete:
                    mEventManager.fire(new UpdateProjectDeletedEvent(projectId, false));
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(layoutManager);
        mListAdapter = new ProjectListAdapter();
        mRecyclerView.setAdapter(mListAdapter);

        // init swipe to dismiss logic
        ItemTouchHelper swipeToDismissTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // callback for drag-n-drop, false to skip this feature
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // callback for swipe to dismiss, removing item from data and adapter

//                items.remove(viewHolder.getAdapterPosition());
                mListAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
            }

            @Override
            public void onChildDraw(
                    Canvas c, RecyclerView recyclerView,
                    RecyclerView.ViewHolder viewHolder,
                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // Get RecyclerView item from the ViewHolder
                    View itemView = viewHolder.itemView;

                    Paint p = new Paint();
                    if (dX > 0) {
                        Bitmap icon = sCompleteIcon;
                        int y = itemView.getTop() + (itemView.getHeight() - icon.getHeight()) / 2;

                        /* Set your color for positive displacement */
                        p.setColor(getResources().getColor(R.color.complete_background));

                        // Draw Rect with varying right side, equal to displacement dX
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                                (float) itemView.getBottom(), p);

                        int hOffset = icon.getWidth();
                        int x = itemView.getLeft() + hOffset;
                        if (dX > x) {

                            Rect destRect = new Rect(x, y,
                                    (int)Math.min(x + icon.getScaledWidth(c), dX),
                                    y + icon.getScaledHeight(c));
                            Rect srcRect = new Rect(0, 0, destRect.width(), destRect.height());
                            c.drawBitmap(icon, srcRect, destRect, null);
                        }
                    } else {
                        Bitmap icon = sDeferIcon;
                        int y = itemView.getTop() + (itemView.getHeight() - icon.getHeight()) / 2;

                        /* Set your color for negative displacement */
                        p.setColor(getResources().getColor(R.color.deferred));

                        // Draw Rect with varying left side, equal to the item's right side plus negative displacement dX
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), p);

                        int hOffset = 2 * icon.getWidth();
                        int x = itemView.getRight() - hOffset;
                        if (itemView.getRight() + dX < x + icon.getScaledWidth(c)) {
                            Rect destRect = new Rect(
                                    Math.max(x, (int) (itemView.getRight() + dX)), y,
                                    x + icon.getScaledWidth(c),
                                    y + icon.getScaledHeight(c));
                            Rect srcRect = new Rect(icon.getScaledWidth(c) - destRect.width(), 0,
                                    icon.getScaledWidth(c), destRect.height());
                            c.drawBitmap(icon, srcRect, destRect, null);
                        }
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        swipeToDismissTouchHelper.attachToRecyclerView(mRecyclerView);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG, "+onActivityCreated " + savedInstanceState + " selector=" + mMultiSelector);

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
                Location location = Location.editProject(Id.create(info.id));
                mEventManager.fire(new NavigationRequestEvent(location));
                return true;
            case R.id.action_delete:
                mEventManager.fire(new UpdateProjectDeletedEvent(Id.create(info.id), true));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.PROJECT_LIST));
                return true;
            case R.id.action_undelete:
                mEventManager.fire(new UpdateProjectDeletedEvent(Id.create(info.id), false));
                mEventManager.fire(new LoadListCursorEvent(ViewMode.PROJECT_LIST));
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
        updateCursor(mCursorProvider.getProjectListCursor());
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
    }

    private void onTaskCountCursorLoaded(@Observes ProjectTaskCountLoadedEvent event) {
        Log.d(TAG, "Project task count loaded " + event.getTaskCountArray());
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
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.PROJECT_LIST));
    }


    public class ProjectHolder extends SelectableHolderImpl implements
            View.OnClickListener, View.OnLongClickListener {

        ProjectListItem mProjectListItem;
        Project mProject;

        public ProjectHolder(ProjectListItem projectListItem) {
            super(projectListItem, mMultiSelector);

            mProjectListItem = projectListItem;
            mProjectListItem.setOnClickListener(this);
            mProjectListItem.setLongClickable(true);
            mProjectListItem.setOnLongClickListener(this);

        }

        @Override
        public void onClick(View v) {
            if (!mMultiSelector.tapSelection(this)) {
                if (mProject != null) {
                    Location location = Location.viewTaskList(ListQuery.project, mProject.getLocalId(), Id.NONE);
                    mEventManager.fire(new NavigationRequestEvent(location));
                }
            } else {
                if (mMultiSelector.getSelectedPositions().isEmpty() && mActionMode != null) {
                    mActionMode.finish();
                }
            }
        }

        public void bindProject(Project project, SparseIntArray taskCountArray) {
            mProject = project;
            mProjectListItem.setTaskCountArray(taskCountArray);
            mProjectListItem.updateView(project);
        }

        @Override
        public boolean onLongClick(View v) {
            mActionMode = getRoboAppCompatActivity().startSupportActionMode(mEditMode);
            mMultiSelector.setSelected(this, true);
            return true;
        }

    }

    public class ProjectListAdapter extends AbstractCursorAdapter<ProjectHolder> {

        private SparseIntArray mTaskCountArray;

        @Override
        public ProjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ProjectListItem listItem = mProjectListItemProvider.get(getActivity());
            return new ProjectHolder(listItem);
        }

        @Override
        public void onBindViewHolder(ProjectHolder holder, int position) {
            Log.d(TAG, "Binding holder at " + position);
            Project project = readProject(position);
            holder.bindProject(project, mTaskCountArray);
        }

        public Project readProject(int position) {
            mCursor.moveToPosition(position);
            return mProjectPersister.read(mCursor);
        }

        public void setTaskCountArray(SparseIntArray taskCountArray) {
            mTaskCountArray = taskCountArray;
        }
    }

}