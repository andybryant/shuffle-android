package org.dodgybits.shuffle.android.core.activity;

import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.controller.ActivityController;
import org.dodgybits.shuffle.android.core.controller.OnePaneController;
import org.dodgybits.shuffle.android.core.controller.TwoPaneController;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.core.view.TaskListCallbacks;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;

public class MainActivity extends RoboActionBarActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private static final String TAG = "MainActivity";

    private ActivityController mController;

    @Inject
    ViewMode mViewMode;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        final boolean tabletUi = UiUtilities.useTabletUI(this.getResources());
        Log.d(TAG, "Using tablet controller? " + tabletUi);
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
    public int getRequestedPosition() {
        return mController.getRequestedPosition();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mController.onNavigationDrawerItemSelected(position);
    }

    public TaskListCallbacks getListHandler() {
        return mController;
    }

}

