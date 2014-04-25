package org.dodgybits.shuffle.android.core.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.controller.ActivityController;
import org.dodgybits.shuffle.android.core.controller.OnePaneController;
import org.dodgybits.shuffle.android.core.controller.TwoPaneController;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.ViewPreferencesEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;
import roboguice.inject.ContextScopedProvider;

import java.util.List;
import java.util.Map;

public class MainActivity extends RoboActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "MainActivity";

    private MyAdapter mAdapter;

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mController.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mController.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mController.onRestart();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mController.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        final Dialog dialog = mController.onCreateDialog(id, bundle);
        return dialog == null ? super.onCreateDialog(id, bundle) : dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mController.onCreateOptionsMenu(menu) || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mController.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        mController.onPause();
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        super.onPrepareDialog(id, dialog, bundle);
        mController.onPrepareDialog(id, dialog, bundle);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mController.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mController.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mController.onStart();
    }

    @Override
    public boolean onSearchRequested() {
        mController.startSearch();
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mController.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mController.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mController.onWindowFocusChanged(hasFocus);
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
