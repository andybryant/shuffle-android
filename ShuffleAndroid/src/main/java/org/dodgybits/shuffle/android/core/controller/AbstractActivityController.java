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


import android.app.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.util.PackageUtils;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.TaskSelectionSet;
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
import org.dodgybits.shuffle.android.server.gcm.GcmRegister;
import org.dodgybits.shuffle.android.server.gcm.event.RegisterGcmEvent;
import org.dodgybits.shuffle.android.server.sync.AuthTokenRetriever;
import org.dodgybits.shuffle.android.server.sync.SyncAlarmService;
import roboguice.event.EventManager;
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
public abstract class AbstractActivityController implements ActivityController {
    private static final String TAG = "AbsActController";

    public static final String QUERY_NAME = "queryName";
    private static final int WHATS_NEW_DIALOG = 0;

    protected MainActivity mActivity;
    protected ViewMode mViewMode;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Inject
    private GcmRegister mGcmRegister;

    @Inject
    protected EventManager mEventManager;

    @Inject
    private NavigationListener mNavigationListener;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    @Inject
    private AuthTokenRetriever mAuthTokenRetriever;

    private Map<ListQuery,Integer> mQueryIndex;

    @Inject
    private ContextScopedProvider<TaskListFragment> mTaskListFragmentProvider;
    @Inject
    private ContextScopedProvider<ContextListFragment> mContextListFragmentProvider;
    @Inject
    private ContextScopedProvider<ProjectListFragment> mProjectListFragmentProvider;

    /**
     * Selected conversations, if any.
     */
    private final TaskSelectionSet mSelectedSet = new TaskSelectionSet();

    final private FragmentManager mFragmentManager;

    @Inject
    protected AbstractActivityController(MainActivity activity, ViewMode viewMode) {
        mActivity = activity;
        mViewMode = viewMode;
        mFragmentManager = activity.getSupportFragmentManager();
        // Allow the fragment to observe changes to its own selection set. No other object is
        // aware of the selected set.
        mSelectedSet.addObserver(this);

    }

    /**
     * Check if the fragment is attached to an activity and has a root view.
     * @param in fragment to be checked
     * @return true if the fragment is valid, false otherwise
     */
    private static boolean isValidFragment(Fragment in) {
        return !(in == null || in.getActivity() == null || in.getView() == null);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public boolean onBackPressed() {
        if (isDrawerEnabled() && mNavigationDrawerFragment.isDrawerVisible()) {
            mNavigationDrawerFragment.closeDrawers();
            return true;
        }

        return handleBackPress();
    }

    protected abstract boolean handleBackPress();
    protected abstract boolean handleUpPress();

    @Override
    public boolean onUpPressed() {
        return handleUpPress();
    }

    /**
     * The application can be started from the following entry points:
     * <ul>
     *     <li>Launcher: you tap on Shuffle icon in the launcher. This is what most users think of
     *         as “Starting the app”.</li>
     *     <li>Shortcut: Users can make a shortcut to take them directly to view.</li>
     *     <li>Widget: Shows the contents of a list view, and allows:
     *     <ul>
     *         <li>Viewing the list (tapping on the title)</li>
     *         <li>Creating a new action (tapping on the new message icon in the title. This
     *         launches the {@link org.dodgybits.shuffle.android.editor.activity.EditTaskActivity}.
     *         </li>
     *         <li>Viewing a single action (tapping on a list element)</li>
     *     </ul>
     *
     *     </li>
     *     <li>...and most importantly, the activity life cycle can tear down the application and
     *     restart it:
     *     <ul>
     *         <li>Rotate the application: it is destroyed and recreated.</li>
     *         <li>Navigate away, and return from recent applications.</li>
     *     </ul>
     *     </li>
     * </ul>
     * {@inheritDoc}
     */
    @Override
    public boolean onCreate(Bundle savedState) {
        // don't show soft keyboard unless user clicks on quick add box
        mActivity.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        checkLastVersion();
        setupNavigationDrawer();
        setupSync();

        final Intent intent = mActivity.getIntent();
        if (intent != null) {
            handleIntent(intent);
        }

        return true;
    }

    private void checkLastVersion() {
        final int lastVersion = Preferences.getLastVersion(mActivity);
        final int currentVersion = PackageUtils.getAppVersion(mActivity);
        if (Math.abs(lastVersion) < Math.abs(currentVersion)) {
            // This is a new install or an upgrade.

            // show what's new message
            SharedPreferences.Editor editor = Preferences.getEditor(mActivity);
            editor.putInt(Preferences.LAST_VERSION, currentVersion);
            // clear out GCM Registration ID after an upgrade
            editor.putString(Preferences.GCM_REGISTRATION_ID, "");
            editor.commit();

            mActivity.showDialog(WHATS_NEW_DIALOG);
        }
    }

    private void setupNavigationDrawer() {
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                mActivity.getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) mActivity.findViewById(R.id.drawer_layout));
    }

    private void setupSync() {
        mEventManager.fire(new RegisterGcmEvent(mActivity));
        mActivity.startService(new Intent(mActivity, SyncAlarmService.class));
        mAuthTokenRetriever.retrieveToken();
    }

    /**
     * Handle an intent to open the app. This method is called only when there is no saved state,
     * so we need to set state that wasn't set before. It is correct to change the viewmode here
     * since it has not been previously set.
     *
     * This method is called for a subset of the reasons mentioned in
     * {@link #onCreate(android.os.Bundle)}. Notably, this is called when launching the app from
     * notifications, widgets, and shortcuts.
     * @param intent intent passed to the activity.
     */
    private void handleIntent(Intent intent) {
        Log.d(TAG, "IN AAC.handleIntent. action=" + intent.getAction());
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            final boolean isConversationMode = intent.hasExtra(Utils.EXTRA_CONVERSATION);

            if (isConversationMode && mViewMode.getMode() == ViewMode.UNKNOWN) {
                mViewMode.enterConversationMode();
            } else {
                mViewMode.enterConversationListMode();
            }
            // Put the folder and conversation, and ask the loader to create this folder.
            final Bundle args = new Bundle();

            final Uri folderUri;
            if (intent.hasExtra(Utils.EXTRA_FOLDER_URI)) {
                folderUri = (Uri) intent.getParcelableExtra(Utils.EXTRA_FOLDER_URI);
            } else if (intent.hasExtra(Utils.EXTRA_FOLDER)) {
                final Folder folder =
                        Folder.fromString(intent.getStringExtra(Utils.EXTRA_FOLDER));
                folderUri = folder.folderUri.fullUri;
            } else {
                final Bundle extras = intent.getExtras();
                LogUtils.d(LOG_TAG, "Couldn't find a folder URI in the extras: %s",
                        extras == null ? "null" : extras.toString());
                folderUri = mAccount.settings.defaultInbox;
            }

            args.putParcelable(Utils.EXTRA_FOLDER_URI, folderUri);
            args.putParcelable(Utils.EXTRA_CONVERSATION,
                    intent.getParcelableExtra(Utils.EXTRA_CONVERSATION));
            restartOptionalLoader(LOADER_FIRST_FOLDER, mFolderCallbacks, args);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            if (intent.hasExtra(Utils.EXTRA_ACCOUNT)) {
                mHaveSearchResults = false;
                // Save this search query for future suggestions.
                final String query = intent.getStringExtra(SearchManager.QUERY);
                final String authority = mContext.getString(R.string.suggestions_authority);
                final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(
                        mContext, authority, SuggestionsProvider.MODE);
                suggestions.saveRecentQuery(query, null);
                setAccount((Account) intent.getParcelableExtra(Utils.EXTRA_ACCOUNT));
                fetchSearchFolder(intent);
                if (shouldEnterSearchConvMode()) {
                    mViewMode.enterSearchResultsConversationMode();
                } else {
                    mViewMode.enterSearchResultsListMode();
                }
            } else {
                LogUtils.e(LOG_TAG, "Missing account extra from search intent.  Finishing");
                mActivity.finish();
            }
        }
        if (mAccount != null) {
            restartOptionalLoader(LOADER_ACCOUNT_UPDATE_CURSOR, mAccountCallbacks, Bundle.EMPTY);
        }
    }

    @Override
    public void onPostCreate(Bundle savedState) {
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onRestart() {

    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        Dialog dialog = null;
        if (id == WHATS_NEW_DIALOG) {
            dialog = new AlertDialog.Builder(mActivity)
                    .setTitle(R.string.whats_new_dialog_title)
                    .setPositiveButton(R.string.ok_button_title, null)
                    .setMessage(R.string.whats_new_dialog_message)
                    .create();
        }

        return dialog;
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

        return false;
    }

    public void restoreActionBar() {
        ActionBar actionBar = mActivity.getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
//        if (mTitle != null) {
//            actionBar.setTitle(mTitle);
//        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
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
                startSearch();
                return true;
        }

        return false;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public void onResume() {
        mActivity.invalidateOptionsMenu();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

    }

    @Override
    public void startSearch() {
        mActionBarView.expandSearch();
    }

    @Override
    public void exitSearchMode() {
        if (mViewMode.getMode() == ViewMode.SEARCH_RESULTS_LIST) {
            mActivity.finish();
        }
    }

    public void disablePagerUpdates() {
        mPagerController.stopListening();
    }


    ////////
    /// From activity
    ////////

    public int getRequestedPosition() {
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

}

