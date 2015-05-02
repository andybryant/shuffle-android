package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
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
public class MenuHandler {
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

    public boolean onCreateOptionsMenu(Menu menu, MenuInflater inflater, boolean drawerOpen) {
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

                MenuItem deleteMenu = menu.findItem(R.id.action_delete);
                MenuItem restoreMenu = menu.findItem(R.id.action_undelete);
                if (listContext.showEditActions()) {
                    String entityName = listContext.getEditEntityName(mActivity);
                    boolean entityDeleted = listContext.isEditEntityDeleted(mActivity, mContextCache, mProjectCache);
                    editMenu.setVisible(true);
                    editMenu.setTitle(getString(R.string.menu_edit, entityName));
                    deleteMenu.setVisible(!entityDeleted);
                    deleteMenu.setTitle(getString(R.string.menu_delete_entity, entityName));
                    restoreMenu.setVisible(entityDeleted);
                    restoreMenu.setTitle(getString(R.string.menu_undelete_entity, entityName));
                } else {
                    editMenu.setVisible(false);
                    deleteMenu.setVisible(false);
                    restoreMenu.setVisible(false);
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
            case R.id.action_view_settings:
                Log.d(TAG, "Bringing up view settings");
                mEventManager.fire(
                        new EditListSettingsEvent(mLocation.getListQuery(), mActivity, FILTER_CONFIG));
                return true;
            case R.id.action_edit:
                switch (mLocation.getViewMode()) {
                    case CONTEXT_LIST:
                    case PROJECT_LIST:
                    case TASK_LIST:
                        if (mLocation.getListQuery() == ListQuery.project) {
                            mEventManager.fire(Location.editProject(mLocation.getProjectId()));
                        } else if (mLocation.getListQuery() == ListQuery.context) {
                            mEventManager.fire(Location.editContext(mLocation.getContextId()));
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
}
