package org.dodgybits.shuffle.android.core.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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
import org.dodgybits.shuffle.android.core.controller.ActivityController;
import org.dodgybits.shuffle.android.core.controller.OnePaneController;
import org.dodgybits.shuffle.android.core.controller.TwoPaneController;
import org.dodgybits.shuffle.android.core.util.PackageUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.ViewPreferencesEvent;
import org.dodgybits.shuffle.android.list.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.list.listener.NavigationListener;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
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

    private MyAdapter mAdapter;

    private ViewPager mPager;

    private List<Fragment> mFragments;
    private Map<ListQuery,Integer> mQueryIndex;

    @Inject
    private ContextScopedProvider<TaskListFragment> mTaskListFragmentProvider;

    @Inject
    private ContextScopedProvider<ContextListFragment> mContextListFragmentProvider;

    @Inject
    private ContextScopedProvider<ProjectListFragment> mProjectListFragmentProvider;


    private ActivityController mController;

    @Inject
    ViewMode mViewMode;



    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final boolean tabletUi = UiUtilities.useTabletUI(this.getResources());
        mController = tabletUi ? new TwoPaneController(this, mViewMode) : new OnePaneController(this, mViewMode);
        mController.onCreate(savedState);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(TAG, "Switching to item " + position);
        if (mPager != null) {
            mPager.setCurrentItem(position);
        }
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
        Integer position = 0;
        String queryName = getIntent().getStringExtra(QUERY_NAME);
        if (queryName != null) {
            ListQuery query = ListQuery.valueOf(queryName);
            position = mQueryIndex.get(query);
            if (position == null) {
                Log.e(TAG, "Couldn't find page " + queryName);
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

    private void setupPager() {
        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        int position = getRequestedPosition();
        mPager.setCurrentItem(position);
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

    public class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }
    }
}
