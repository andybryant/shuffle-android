package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.app.SearchManager;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.ActiveToggleEvent;
import org.dodgybits.shuffle.android.core.event.CompletedToggleEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.MoveEnabledChangeEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.EntityCache;
import org.dodgybits.shuffle.android.core.model.persistence.selector.Flag;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.preference.model.ListFeatures;
import org.dodgybits.shuffle.android.preference.model.ListSettings;
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
            case SEARCH_RESULTS_LIST:
                inflater.inflate(R.menu.task_list_menu, menu);
                break;
            case TASK:
            case SEARCH_RESULTS_TASK:
//                inflater.inflate(R.menu.task_view_menu, menu);
                break;
        }

        prepareSearch(menu);

        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mLocation == null) {
            return false;
        }
        MenuItem editMenu = menu.findItem(R.id.action_edit);
        MenuItem moveMenu = menu.findItem(R.id.move_toggle);
        switch (mLocation.getViewMode()) {
            case CONTEXT_LIST:
                break;
            case PROJECT_LIST:
                break;
            case TASK_LIST:
            case SEARCH_RESULTS_LIST:
                if (ListFeatures.showEditActions(mLocation)) {
                    String entityName = ListFeatures.getEditEntityName(mActivity, mLocation);
                    editMenu.setVisible(true);
                    editMenu.setTitle(getString(R.string.menu_edit, entityName));
                } else {
                    editMenu.setVisible(false);
                }
                moveMenu.setVisible(ListFeatures.showMoveActions(mLocation));
                break;
            case TASK:
            case SEARCH_RESULTS_TASK:
                break;
        }

        addCompleteToggleListener(menu);
        addActiveToggleListener(menu);
        addMoveToggleListener(menu);

        return true;
    }

    private void addCompleteToggleListener(Menu menu) {
        MenuItem toggleMenu = menu.findItem(R.id.complete_toggle);
        if (toggleMenu != null) {
            final CompoundButton completeSwitch = (CompoundButton) toggleMenu.getActionView();
            if (completeSwitch != null) {
                ListSettings listSettings = ListSettingsCache.findSettings(
                        mLocation.getListQuery());
                if (listSettings.isCompletedEnabled()) {
                    toggleMenu.setVisible(true);
                    Flag completed = listSettings
                            .getCompleted(mActivity);
                    completeSwitch.setChecked(completed == Flag.yes);
                    completeSwitch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "complete toggle hit");
                            mEventManager.fire(new CompletedToggleEvent(
                                    completeSwitch.isChecked(),
                                    mLocation.getListQuery(),
                                    mLocation.getViewMode()));
                        }
                    });
                } else {
                    toggleMenu.setVisible(false);
                }
            }
        }
    }

    private void addActiveToggleListener(Menu menu) {
        MenuItem toggleMenu = menu.findItem(R.id.active_toggle);
        if (toggleMenu != null) {
            final CompoundButton activeSwitch = (CompoundButton) toggleMenu.getActionView();
            if (activeSwitch != null) {
                Flag active = ListSettingsCache.findSettings(
                        mLocation.getListQuery())
                        .getActive(mActivity);
                activeSwitch.setChecked(active == Flag.no);
                activeSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "active toggle hit");
                        mEventManager.fire(new ActiveToggleEvent(
                                activeSwitch.isChecked(),
                                mLocation.getListQuery(),
                                mLocation.getViewMode()));
                    }
                });
            }
        }
    }

    private void addMoveToggleListener(Menu menu) {
        MenuItem toggleMenu = menu.findItem(R.id.move_toggle);
        if (toggleMenu != null) {
            final CompoundButton moveSwitch = (CompoundButton) toggleMenu.getActionView();
            if (moveSwitch != null) {
                moveSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "move toggle hit");
                        mEventManager.fire(new MoveEnabledChangeEvent(moveSwitch.isChecked()));
                    }
                });
            }
        }
    }

    private void prepareSearch(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) mActivity.getSystemService(android.content.Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));
        }

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            case R.id.nav_projects:
                go(Location.viewProjectList());
                return true;
            case R.id.nav_contexts:
                go(Location.viewContextList());
                return true;
            case R.id.nav_deferred:
                go(Location.viewTaskList(ListQuery.deferred));
                return true;
            case R.id.nav_deleted:
                go(Location.viewTaskList(ListQuery.deleted));
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
