/**
 * Copyright (C) 2014 Android Shuffle Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dodgybits.shuffle.android.core.listener;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.ContextListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.ProjectListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.TaskListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
import org.dodgybits.shuffle.android.list.view.task.TaskRecyclerFragment;
import org.dodgybits.shuffle.android.view.fragment.TaskPagerFragment;

import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class FragmentLoader {
    private static final String TAG = "FragmentLoader";

    /** Tags used when loading fragments. */
    public static final String TAG_TASK_LIST = "tag-task-list";
    public static final String TAG_TASK_ITEM = "tag-task-item";
    public static final String TAG_CONTEXT_LIST = "tag-context-list";
    public static final String TAG_PROJECT_LIST = "tag-project-list";

    private Location mPrevLocation;
    private Location mLocation;

    private FragmentActivity mActivity;

    @Inject
    public FragmentLoader(Activity activity) {
        mActivity = (FragmentActivity) activity;
    }

    private void onViewUpdated(@Observes LocationUpdatedEvent event) {
        mPrevLocation = mLocation;
        mLocation = event.getLocation();
    }

    private void onTaskListCursorLoaded(@Observes TaskListCursorLoadedEvent event) {
        Log.i(TAG, "Task list Cursor loaded - loading fragment now");
        if (!isNewLocation()) {
            Log.d(TAG, "Not updating fragment since already loaded");
            return;
        }

        switch (mLocation.getViewMode()) {
            case TASK_LIST:
            case SEARCH_RESULTS_LIST:
                addTaskList();
                break;
            case TASK:
            case SEARCH_RESULTS_TASK:
                if (UiUtilities.showListOnViewTask(mActivity.getResources())) {
                    addTaskList();
                }
                addTaskView();
                break;
            default:
                Log.w(TAG, "Unexpected view mode " + mLocation.getViewMode());
                break;
        }
    }

    private boolean isNewLocation() {
        return (mPrevLocation == null ||
                mLocation.getViewMode() != mPrevLocation.getViewMode());
    }

    private void onContextListCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        if (!isNewLocation()) {
            Log.d(TAG, "Not updating context list fragment since already loaded");
            return;
        }
        if (mLocation.getViewMode() == ViewMode.CONTEXT_LIST) {
            addContextList();
        }
    }

    private void onProjectListCursorLoaded(@Observes ProjectListCursorLoadedEvent event) {
        if (!isNewLocation()) {
            Log.d(TAG, "Not updating project list fragment since already loaded");
            return;
        }
        if (mLocation.getViewMode() == ViewMode.PROJECT_LIST) {
            addProjectList();
        }
    }

    private void addTaskList() {
        TaskRecyclerFragment fragment = getTaskRecyclerFragment();
        if (fragment == null) {
            fragment = new TaskRecyclerFragment();
            Log.d(TAG, "Creating task list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
//            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.replace(R.id.entity_list_pane,
                    fragment, TAG_TASK_LIST);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    private void addTaskView() {
        TaskPagerFragment fragment = getTaskPagerFragment() ;
        if (fragment == null) {
            fragment = new TaskPagerFragment();
            Log.d(TAG, "Creating task pager " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
//            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.replace(R.id.task_pane, fragment, TAG_TASK_ITEM);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    private void addContextList() {
        ContextListFragment fragment = getContextListFragment();
        if (fragment == null) {
            fragment = new ContextListFragment();
            Log.d(TAG, "Creating context list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
//            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.replace(R.id.entity_list_pane, fragment, TAG_CONTEXT_LIST);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    private void addProjectList() {
        ProjectListFragment fragment = getProjectListFragment();
        if (fragment == null) {
            fragment = new ProjectListFragment();
            Log.d(TAG, "Creating project list fragment " + fragment);

            FragmentTransaction fragmentTransaction =
                    mActivity.getSupportFragmentManager().beginTransaction();
            // Use cross fading animation.
//            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.replace(R.id.entity_list_pane, fragment, TAG_PROJECT_LIST);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    protected TaskRecyclerFragment getTaskRecyclerFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_TASK_LIST);
        if (isValidFragment(fragment)) {
            return (TaskRecyclerFragment) fragment;
        }
        return null;
    }

    protected TaskPagerFragment getTaskPagerFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_TASK_ITEM);
        if (isValidFragment(fragment)) {
            return (TaskPagerFragment) fragment;
        }
        return null;
    }

    protected ContextListFragment getContextListFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_CONTEXT_LIST);
        if (isValidFragment(fragment)) {
            return (ContextListFragment) fragment;
        }
        return null;
    }

    protected ProjectListFragment getProjectListFragment() {
        final Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(TAG_PROJECT_LIST);
        if (isValidFragment(fragment)) {
            return (ProjectListFragment) fragment;
        }
        return null;
    }

    /**
     * Check if the fragment is attached to an activity and has a root view.
     * @param in fragment to be checked
     * @return true if the fragment is valid, false otherwise
     */
    private static boolean isValidFragment(Fragment in) {
        return !(in == null || in.getActivity() == null || in.getView() == null);
    }

}
