package org.dodgybits.shuffle.android.list.view.project;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SingleSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;
import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LoadCountCursorEvent;
import org.dodgybits.shuffle.android.core.event.LoadListCursorEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.event.ProjectTaskCountCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.AbstractCursorAdapter;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;
import roboguice.inject.ContextScopedProvider;

public class ProjectListFragment extends RoboFragment {
    private static final String TAG = "ProjectListFragment";

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
        mListAdapter = new ProjectListAdapter();
        mRecyclerView.setAdapter(mListAdapter);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateCursor();
        Log.d(TAG, "-onActivityCreated");

        getView().findViewById(R.id.fab).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Location location = Location.newProject();
                        mEventManager.fire(new NavigationRequestEvent(location));
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshChildCount();
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        MenuInflater inflater = getActivity().getMenuInflater();
//        inflater.inflate(R.menu.project_list_context_menu, menu);
//
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
//        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
//        Project project = mProjectPersister.read(cursor);
//
//        String entityName = getString(R.string.project_name);
//
//        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
//        deleteMenu.setVisible(!project.isDeleted());
//        deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));
//
//        MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
//        undeleteMenu.setVisible(project.isDeleted());
//        undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
//    }

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

    private void onTaskCountCursorLoaded(@Observes ProjectTaskCountCursorLoadedEvent event) {
        Cursor cursor = event.getCursor();
        SparseIntArray taskCountArray = mTaskPersister.readCountArray(cursor);
        mListAdapter.setTaskCountArray(taskCountArray);
        Log.d(TAG, "Project task count loaded " + taskCountArray);
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mListAdapter.notifyDataSetChanged();
                }
            });
        }
        cursor.close();
    }

    protected RoboAppCompatActivity getRoboAppCompatActivity() {
        return (RoboAppCompatActivity) getActivity();
    }

    private void refreshChildCount() {
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.PROJECT_LIST));
    }

    public class ProjectHolder extends SwappingHolder implements
            View.OnClickListener, View.OnLongClickListener {

        ProjectListItem mProjectListItem;
        Project mProject;

        public ProjectHolder(ProjectListItem projectListItem) {
            super(projectListItem, mMultiSelector);

            mProjectListItem = projectListItem;
            mProjectListItem.setOnClickListener(this);
            mProjectListItem.setOnLongClickListener(this);
            mProjectListItem.setLongClickable(true);

        }

        @Override
        public void onClick(View v) {
            if (mProject != null) {
                Location location = Location.viewTaskList(ListQuery.project, mProject.getLocalId(), Id.NONE);
                mEventManager.fire(new NavigationRequestEvent(location));
            }
        }

        public void onUpdate(Project project, SparseIntArray taskCountArray) {
            mProject = project;
            mProjectListItem.setTaskCountArray(taskCountArray);
            mProjectListItem.updateView(project);
        }



        @Override
        public boolean onLongClick(View v) {
//            AppCompatActivity activity = (AppCompatActivity)getActivity();
//            activity.startSupportActionMode(mEditMode);
//            mMultiSelector.setSelected(this, true);
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
            mCursor.moveToPosition(position);
            Project project = mProjectPersister.read(mCursor);
            holder.onUpdate(project, mTaskCountArray);
        }

        public void setTaskCountArray(SparseIntArray taskCountArray) {
            mTaskCountArray = taskCountArray;
        }
    }

}