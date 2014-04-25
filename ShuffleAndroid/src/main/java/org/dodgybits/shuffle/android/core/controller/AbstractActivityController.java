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


import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.util.PackageUtils;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.list.listener.NavigationListener;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import org.dodgybits.shuffle.android.server.gcm.GcmRegister;
import org.dodgybits.shuffle.android.server.gcm.event.RegisterGcmEvent;
import org.dodgybits.shuffle.android.server.sync.AuthTokenRetriever;
import org.dodgybits.shuffle.android.server.sync.SyncAlarmService;
import roboguice.event.EventManager;

public abstract class AbstractActivityController implements ActivityController {

    public static final String QUERY_NAME = "queryName";
    private static final int WHATS_NEW_DIALOG = 0;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    protected CharSequence mTitle;
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

    protected AbstractActivityController(MainActivity activity, ViewMode viewMode) {
        mActivity = activity;
        mViewMode = viewMode;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public boolean onUpPressed() {
        return false;
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
        return null;
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
        if (mTitle != null) {
            actionBar.setTitle(mTitle);
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
}

