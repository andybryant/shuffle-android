package org.dodgybits.shuffle.android.core.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.util.PackageUtils;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.list.event.ViewPreferencesEvent;
import org.dodgybits.shuffle.android.list.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.list.listener.NavigationListener;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
import org.dodgybits.shuffle.android.list.view.task.MultiTaskListContext;
import org.dodgybits.shuffle.android.list.view.task.MultiTaskListFragment;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;
import org.dodgybits.shuffle.android.server.gcm.GcmRegister;
import org.dodgybits.shuffle.android.server.gcm.event.RegisterGcmEvent;
import org.dodgybits.shuffle.android.server.sync.AuthTokenRetriever;
import org.dodgybits.shuffle.android.server.sync.SyncAlarmService;
import roboguice.event.EventManager;
import roboguice.inject.ContextScopedProvider;

import java.util.List;
import java.util.Map;

public class MainActivity extends RoboActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "MainActivity";

    public static final String QUERY_NAME = "queryName";
    private static final int WHATS_NEW_DIALOG = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private List<Fragment> mFragments;
    private Map<ListQuery,Integer> mQueryIndex;

    @Inject
    private ContextScopedProvider<TaskListFragment> mTaskListFragmentProvider;

    @Inject
    private ContextScopedProvider<MultiTaskListFragment> mMultiTaskListFragmentProvider;

    @Inject
    private ContextScopedProvider<ContextListFragment> mContextListFragmentProvider;

    @Inject
    private ContextScopedProvider<ProjectListFragment> mProjectListFragmentProvider;

    @Inject
    private GcmRegister gcmRegister;

    @Inject
    private EventManager mEventManager;

    @Inject
    private NavigationListener mNavigationListener;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    @Inject
    private AuthTokenRetriever authTokenRetriever;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // don't show soft keyboard unless user clicks on quick add box
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        initFragments();
        checkLastVersion();
        setupNavigationDrawer();
        setupSync();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(TAG, "Switching to item " + position);
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, mFragments.get(position))
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preferences:
                Log.d(TAG, "Bringing up preferences");
                mEventManager.fire(new ViewPreferencesEvent());
                return true;
            case R.id.action_search:
                Log.d(TAG, "Bringing up search");
                onSearchRequested();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public int getRequestedPosition() {
        if (mQueryIndex == null) {
            initFragments();
        }
        int position = 0;
        String queryName = getIntent().getStringExtra(QUERY_NAME);
        if (queryName != null) {
            ListQuery query = ListQuery.valueOf(queryName);
            position = mQueryIndex.get(query);
            if (position == -1) {
                Log.e(TAG, "Couldn't find page of list " + queryName);
                position = 0;
            }
        }

        return position;
    }


    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        super.setTitle(title);
    }

    private void checkLastVersion() {
        final int lastVersion = Preferences.getLastVersion(this);
        final int currentVersion = PackageUtils.getAppVersion(this);
        if (Math.abs(lastVersion) < Math.abs(currentVersion)) {
            // This is a new install or an upgrade.

            // show what's new message
            SharedPreferences.Editor editor = Preferences.getEditor(this);
            editor.putInt(Preferences.LAST_VERSION, currentVersion);
            // clear out GCM Registration ID after an upgrade
            editor.putString(Preferences.GCM_REGISTRATION_ID, "");
            editor.commit();

            showDialog(WHATS_NEW_DIALOG);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        if (id == WHATS_NEW_DIALOG) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.whats_new_dialog_title)
                    .setPositiveButton(R.string.ok_button_title, null)
                    .setMessage(R.string.whats_new_dialog_message)
                    .create();
        } else {
            dialog = super.onCreateDialog(id);
        }
        return dialog;
    }

    private void initFragments() {
        mFragments = Lists.newArrayList();
        mQueryIndex = Maps.newHashMap();

        addTaskList(ListQuery.inbox);
        addTaskList(ListQuery.dueNextMonth);
        addTaskList(ListQuery.nextTasks);
        addFragment(ListQuery.project, mProjectListFragmentProvider.get(this));
        addFragment(ListQuery.context, mContextListFragmentProvider.get(this));
        addTaskList(ListQuery.custom);
        addTaskList(ListQuery.tickler);
    }

    private void setupNavigationDrawer() {
        setContentView(R.layout.main);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    private void addMultiTaskList(List<ListQuery> queries) {
        MultiTaskListContext listContext = MultiTaskListContext.create(queries);
        addFragment(queries, createMultiTaskFragment(listContext));
    }

    private MultiTaskListFragment createMultiTaskFragment(MultiTaskListContext listContext) {
        MultiTaskListFragment fragment = mMultiTaskListFragmentProvider.get(this);
        Bundle args = new Bundle();
        args.putParcelable(TaskListFragment.ARG_LIST_CONTEXT, listContext);
        fragment.setArguments(args);
        return fragment;
    }

    private void addTaskList(ListQuery query) {
        TaskListContext listContext = TaskListContext.create(query);
        addFragment(query, createTaskFragment(listContext));
    }

    private TaskListFragment createTaskFragment(TaskListContext listContext) {
        TaskListFragment fragment = mTaskListFragmentProvider.get(this);
        Bundle args = new Bundle();
        args.putParcelable(TaskListFragment.ARG_LIST_CONTEXT, listContext);
        fragment.setArguments(args);
        return fragment;
    }

    private void addFragment(ListQuery query, Fragment fragment) {
        addFragment(Lists.newArrayList(query), fragment);
    }

    private void addFragment(List<ListQuery> queries, Fragment fragment) {
        mFragments.add(fragment);
        int index = mFragments.size() - 1;
        for (ListQuery query : queries) {
            mQueryIndex.put(query, index);
        }
    }

    private void setupSync() {
        mEventManager.fire(new RegisterGcmEvent(this));
        startService(new Intent(this, SyncAlarmService.class));
        authTokenRetriever.retrieveToken();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        if (mTitle != null) {
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.

            // TODO define menu when drawer open
//            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

}
