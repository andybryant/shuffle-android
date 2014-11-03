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
package org.dodgybits.shuffle.android.core.controller;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.util.PackageUtils;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.TaskSelectionSet;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.event.ListSettingsUpdatedEvent;
import org.dodgybits.shuffle.android.list.event.ViewPreferencesEvent;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.NavigationListener;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.context.ContextListFragment;
import org.dodgybits.shuffle.android.list.view.project.ProjectListFragment;
import org.dodgybits.shuffle.android.list.view.task.TaskListAdaptor;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.gcm.GcmRegister;
import org.dodgybits.shuffle.android.server.gcm.event.RegisterGcmEvent;
import org.dodgybits.shuffle.android.server.sync.AuthTokenRetriever;
import org.dodgybits.shuffle.android.server.sync.SyncAlarmService;
import roboguice.RoboGuice;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextScopedProvider;

import java.util.Map;

/**
 * This is an abstract implementation of the Activity Controller. This class
 * knows how to respond to menu items, state changes, layout changes, etc. It
 * weaves together the views and listeners, dispatching actions to the
 * respective underlying classes.
 * <p>
 * Even though this class is abstract, it should provide default implementations
 * for most, if not all the methods in the ActivityController interface. This
 * makes the task of the subclasses easier: OnePaneController and
 * TwoPaneController can be concise when the common functionality is in
 * AbstractActivityController.
 * </p>
 */
public abstract class AbstractActivityController {
//
//    /** Key to store {@link #mTaskListScrollPositions} */
//    private static final String SAVED_TASK_LIST_SCROLL_POSITIONS =
//            "saved-task-list-scroll-positions";
//
//
//
//    protected MainActivity mActivity;
//    protected ViewMode mViewMode;
//
//    private Cursor mTaskListCursor;
//    private TaskListContext mListContext;
//
//
//    /** A map of {@link ListQuery} to scroll position in the task list. */
//    private final Bundle mTaskListScrollPositions = new Bundle();
//
//
//
//    private Map<ListQuery,Integer> mQueryIndex;
//
//    /**
//     * Selected conversations, if any.
//     */
//    private final TaskSelectionSet mSelectedSet = new TaskSelectionSet();
//
//    final private FragmentManager mFragmentManager;
//
//    @Inject
//    protected AbstractActivityController(MainActivity activity, ViewMode viewMode) {
//        mActivity = activity;
//        mViewMode = viewMode;
//        mFragmentManager = activity.getSupportFragmentManager();
//        // Allow the fragment to observe changes to its own selection set. No other object is
//        // aware of the selected set.
//
////        mSelectedSet.addObserver(this);
//
//    }
//
//    /**
//     * Check if the fragment is attached to an activity and has a root view.
//     * @param in fragment to be checked
//     * @return true if the fragment is valid, false otherwise
//     */
//    private static boolean isValidFragment(Fragment in) {
//        return !(in == null || in.getActivity() == null || in.getView() == null);
//    }
//
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//    }
//
//    @Override
//    public boolean onBackPressed() {
//        if (isDrawerEnabled() && mNavigationDrawerFragment.isDrawerVisible()) {
//            mNavigationDrawerFragment.closeDrawers();
//            return true;
//        }
//
//        return handleBackPress();
//    }
//
//    @Override
//    public boolean onUpPressed() {
//        return handleUpPress();
//    }
//
//    @Override
//    public void setTaskListScrollPosition(ListQuery listQuery, Parcelable savedPosition) {
//        mTaskListScrollPositions.putParcelable(listQuery.name(), savedPosition);
//    }
//
//    @Override
//    public Parcelable getTaskListScrollPosition(ListQuery listQuery) {
//        return mTaskListScrollPositions.getParcelable(listQuery.name());
//    }
//
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedState) {
//        mTaskListScrollPositions.clear();
//        mTaskListScrollPositions.putAll(
//                savedState.getBundle(SAVED_TASK_LIST_SCROLL_POSITIONS));
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        mViewMode.handleSaveInstanceState(outState);
//
//        outState.putBundle(SAVED_TASK_LIST_SCROLL_POSITIONS,
//                mTaskListScrollPositions);
//    }
//
//    @Override
//    public void startSearch() {
////        mActionBarView.expandSearch();
//    }
//
//    @Override
//    public void exitSearchMode() {
//        if (mViewMode.getMode() == ViewMode.SEARCH_RESULTS_LIST) {
//            mActivity.finish();
//        }
//    }
//
//    public void disablePagerUpdates() {
////        mPagerController.stopListening();
//    }
//

}

