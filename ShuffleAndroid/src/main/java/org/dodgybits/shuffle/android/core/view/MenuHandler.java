package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.list.event.EditListSettingsEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class MenuHandler implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MenuHandler";

    // result codes
    private static final int FILTER_CONFIG = 600;

    private AppCompatActivity mActivity;

    private Location mLocation;

    @Inject
    private CursorProvider mCursorProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    EntityCache<Context> mContextCache;

    @Inject
    EntityCache<Project> mProjectCache;

    @Inject
    public MenuHandler(Activity activity) {
        mActivity = (AppCompatActivity) activity;
    }

    public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mLocation == null) {
            return false;
        }

        switch (mLocation.getViewMode()) {
            case CONTEXT_LIST:
                inflater.inflate(R.menu.list_menu, menu);
                break;
            case PROJECT_LIST:
                inflater.inflate(R.menu.list_menu, menu);
                break;
            case TASK_LIST:
                inflater.inflate(R.menu.list_menu, menu);
                break;
            case TASK:
                inflater.inflate(R.menu.task_view_menu, menu);
                break;
            case SEARCH_RESULTS_TASK:
            case SEARCH_RESULTS_LIST:
                break;
        }

        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mLocation == null) {
            return false;
        }
        MenuItem editMenu = menu.findItem(R.id.action_edit);

        switch (mLocation.getViewMode()) {
            case CONTEXT_LIST:
                editMenu.setTitle(getString(R.string.menu_edit, getString(R.string.context_name)));
                break;
            case PROJECT_LIST:
                editMenu.setTitle(getString(R.string.menu_edit, getString(R.string.project_name)));
                break;
            case TASK_LIST:
                TaskListContext listContext = TaskListContext.create(mLocation);
                if (listContext == null) {
                    return false;
                }

                if (listContext.showEditActions()) {
                    String entityName = listContext.getEditEntityName(mActivity);
                    editMenu.setVisible(true);
                    editMenu.setTitle(getString(R.string.menu_edit, entityName));
                } else {
                    editMenu.setVisible(false);
                }
                break;
            case TASK:
                break;
            case SEARCH_RESULTS_TASK:
            case SEARCH_RESULTS_LIST:
                break;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.complete_toggle:
                Log.d(TAG, "complete toggle hit");
//                mEventManager.fire(
//                        new EditListSettingsEvent(mLocation.getListQuery(), mActivity, FILTER_CONFIG));
                return true;
            case R.id.action_edit:
                switch (mLocation.getViewMode()) {
                    case CONTEXT_LIST:
                    case PROJECT_LIST:
                    case TASK_LIST:
                        if (mLocation.getListQuery() == ListQuery.project) {
                            go(Location.editProject(mLocation.getProjectId()));
                        } else if (mLocation.getListQuery() == ListQuery.context) {
                            go(Location.editContext(mLocation.getContextId()));
                        }
                        break;
                    case TASK:
                        // TODO

                }
                break;
            case android.R.id.home:
                mEventManager.fire(new NavigationRequestEvent(mLocation.parent()));
                return true;
        }
        return false;
    }

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
    }

    private String getString(int id) {
        return mActivity.getString(id);
    }

    private String getString(int id, String arg) {
        return mActivity.getString(id, arg);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        Log.d(TAG, "Nav item selected" + menuItem);
        switch (menuItem.getItemId()) {
            case R.id.nav_inbox:
                go(Location.viewTaskList(ListQuery.inbox));
                return true;
            case R.id.nav_next_tasks:
                go(Location.viewTaskList(ListQuery.nextTasks));
                return true;
            case R.id.nav_due_tasks:
                go(Location.viewTaskList(ListQuery.dueTasks));
                return true;
            case R.id.nav_custom:
                go(Location.viewTaskList(ListQuery.custom));
                return true;
            case R.id.nav_tickler:
                go(Location.viewTaskList(ListQuery.tickler));
                return true;
            case R.id.nav_projects:
                go(Location.viewProjectList());
                return true;
            case R.id.nav_contexts:
                go(Location.viewContextList());
                return true;
            case R.id.nav_settings:
                go(Location.viewSettings());
                return true;
            case R.id.nav_help:
                go(Location.viewHelp());
                return true;
        }
        return false;
    }

    private void go(Location location) {
        mEventManager.fire(new NavigationRequestEvent(location));

    }
}
