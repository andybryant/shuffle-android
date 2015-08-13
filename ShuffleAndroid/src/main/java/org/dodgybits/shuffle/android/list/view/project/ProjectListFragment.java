package org.dodgybits.shuffle.android.list.view.project;

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
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LoadCountCursorEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.event.ProjectTaskCountLoadedEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.view.AbstractSwipeItemTouchHelperCallback;
import org.dodgybits.shuffle.android.core.view.DividerItemDecoration;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateProjectActiveEvent;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.list.view.SelectableHolderImpl;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;

import java.util.List;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

public class ProjectListFragment extends RoboFragment {
    private static final String TAG = "ProjectListFragment";

    private static Bitmap sInactiveIcon;
    private static Bitmap sActiveIcon;
    private static Bitmap sDeleteIcon;

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

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            String entityName = getString(R.string.project_name);
            List<Integer> positions = mMultiSelector.getSelectedPositions();
            boolean isDeleted = false;
            boolean isActive = true;
            if (positions != null && !positions.isEmpty() && mCursor != null) {
                int position = positions.get(0);
                Project project = mListAdapter.readProject(position);
                isDeleted = project.isDeleted();
                isActive = project.isActive();
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
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            int position = mMultiSelector.getSelectedPositions().get(0);
            Id projectId = mListAdapter.readProject(position).getLocalId();

            switch (menuItem.getItemId()) {
                case R.id.action_edit:
                    mEventManager.fire(new NavigationRequestEvent(Location.editProject(projectId)));
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

                case R.id.action_active:
                    mEventManager.fire(new UpdateProjectActiveEvent(projectId, true));
                    mActionMode.finish();
                    return true;

                case R.id.action_inactive:
                    mEventManager.fire(new UpdateProjectActiveEvent(projectId, false));
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
        mListAdapter = new ProjectListAdapter();
        mListAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mListAdapter);

        // init swipe to dismiss logic
        ItemTouchHelper helper = new ItemTouchHelper(new ProjectCallback());
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
        refreshChildCount();
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
                } else {
                    mActionMode.invalidate();
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
            mActionMode.invalidate();
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

    public class ProjectCallback extends AbstractSwipeItemTouchHelperCallback {

        public ProjectCallback() {
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
            ProjectHolder holder = (ProjectHolder) viewHolder;
            Project project = holder.mProject;
            Id id = Id.create(viewHolder.getItemId());

            if (direction == ItemTouchHelper.LEFT) {
                Log.d(TAG, "Toggling active for project id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateProjectActiveEvent(id, !project.isActive()));
            } else {
                Log.d(TAG, "Toggling delete for project id " + id + " position=" + viewHolder.getAdapterPosition());
                mEventManager.fire(new UpdateProjectDeletedEvent(id, !project.isDeleted()));
            }

        }


    }

}