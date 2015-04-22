package org.dodgybits.shuffle.android.list.view.project;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.UpdateProjectDeletedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboListFragment;

public class ProjectListFragment extends RoboListFragment {
    private static final String TAG = "ProjectListFragment";
    
    /** Argument name(s) */
    private static final String BUNDLE_LIST_STATE = "ProjectListFragment.state.listState";

    @Inject
    private ProjectListAdaptor mListAdapter;

    @Inject
    private TaskPersister mTaskPersister;

    @Inject
    private ProjectPersister mProjectPersister;

    @Inject
    private EventManager mEventManager;

    @Inject
    private CursorProvider mCursorProvider;

    private Parcelable mSavedListState;

    private Cursor mCursor;

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
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        inflater.inflate(R.layout.fab, root, true);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final ListView lv = getListView();
        lv.setItemsCanFocus(false);
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(lv);

        setEmptyText(getString(R.string.no_projects));

        if (savedInstanceState != null) {
            // Fragment doesn't have this method.  Call it manually.
            restoreInstanceState(savedInstanceState);
            restoreListState();
        }

        updateCursor();
        Log.d(TAG, "-onActivityCreated");

        getActivity().findViewById(R.id.fab).setOnClickListener(
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
    public void onPause() {
        mSavedListState = getListView().onSaveInstanceState();
        super.onPause();

        Log.d(TAG, "-onPause");
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshChildCount();
    }

    /**
     * Called when a message is clicked.
     */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
        Location location = Location.viewTaskList(ListQuery.project, Id.create(id), Id.NONE);
        mEventManager.fire(new NavigationRequestEvent(location));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.project_list_context_menu, menu);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        Project project = mProjectPersister.read(cursor);

        String entityName = getString(R.string.project_name);

        MenuItem deleteMenu = menu.findItem(R.id.action_delete);
        deleteMenu.setVisible(!project.isDeleted());
        deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));
        
        MenuItem undeleteMenu = menu.findItem(R.id.action_undelete);
        undeleteMenu.setVisible(project.isDeleted());
        undeleteMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
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

        Log.d(TAG, "Swapping cursor and setting adapter");
        mCursor = cursor;
        mListAdapter.swapCursor(cursor);
        setListAdapter(mListAdapter);

        restoreListState();
    }

    private void restoreListState() {
        if (getActivity() != null && getListAdapter() != null) {
            // Restore the state -- this step has to be the last, because Some of the
            // "post processing" seems to reset the scroll position.
            if (mSavedListState != null) {
                getListView().onRestoreInstanceState(mSavedListState);
                mSavedListState = null;
            }
        }
    }

    private void onTaskCountCursorLoaded(@Observes ProjectTaskCountCursorLoadedEvent event) {
        Cursor cursor = event.getCursor();
        mListAdapter.setTaskCountArray(mTaskPersister.readCountArray(cursor));
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getListView().invalidateViews();
                }
            });
        }
        cursor.close();
    }

    protected RoboAppCompatActivity getRoboAppCompatActivity() {
        return (RoboAppCompatActivity) getActivity();
    }

    void restoreInstanceState(Bundle savedInstanceState) {
        mSavedListState = savedInstanceState.getParcelable(BUNDLE_LIST_STATE);
    }

    private void refreshChildCount() {
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.PROJECT_LIST));
    }

}