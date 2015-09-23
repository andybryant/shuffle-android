package org.dodgybits.shuffle.android.core.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.OnCreatedEvent;
import org.dodgybits.shuffle.android.core.listener.FragmentLoader;
import org.dodgybits.shuffle.android.core.listener.MainListeners;
import org.dodgybits.shuffle.android.core.model.Context;
import org.dodgybits.shuffle.android.core.model.Project;
import org.dodgybits.shuffle.android.core.model.persistence.CursorEntityCache;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.util.FontUtils;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.core.view.MenuHandler;
import org.dodgybits.shuffle.android.preference.model.ListFeatures;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;

public abstract class AbstractMainActivity extends RoboAppCompatActivity
        implements ShuffleAppCompatActivity {
    private static final String TAG = "AbstractMainActivity";

    private static final int WHATS_NEW_DIALOG = 5000;

    private static final String LOCATION_KEY = "AbstractMainActivity.location";


    protected Location mLocation;

    @Inject
    private MainListeners mListeners;

    @Inject
    protected EventManager mEventManager;

    @Inject
    private FragmentLoader mFragmentLoader;

    @Inject
    private MenuHandler mMenuHandler;

    @Inject
    private LocationParser mLocationParser;

    @Inject
    private CursorEntityCache<Context> mContextCache;

    @Inject
    private CursorEntityCache<Project> mProjectCache;

    // Primary toolbar and drawer toggle
    protected Toolbar mActionBarToolbar;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;

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
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        validateActivity();

        // don't show soft keyboard unless user clicks on quick add box
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mLocationParser.setLocationActivity(getLocationActivity());
        Location location = parseLocation(savedState);
        validateLocation(location);
        mEventManager.fire(new LocationUpdatedEvent(location));

        final boolean tabletUi = UiUtilities.useTabletUI(this.getResources());
        Log.d(TAG, getClass().getName() + " using tablet layout? " + tabletUi);
        setContentView(contentView(tabletUi));

        mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (mActionBarToolbar != null) {
            setSupportActionBar(mActionBarToolbar);
            FontUtils.setCustomFont(mActionBarToolbar, getAssets());
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setupNavDrawer();
        updateFab();

        mEventManager.fire(new OnCreatedEvent());
    }

    /**
     * Insure this is the right activity for the device.
     * Warning - this is called before mLocation is set.
     * If you need location, use validateLocation instead.
     */
    protected void validateActivity() {
    }

    /**
     * Insure this is the right activity for location.
     */
    protected void validateLocation(Location location) {
    }

    protected void redirect(Class newActivityClazz) {
        redirect(newActivityClazz, null);
    }

    protected void redirect(Class newActivityClazz, Location location) {
        Intent newIntent = location == null ? new Intent(getIntent()) :
                LocationParser.createIntent(this, location);
        newIntent.setClass(this, newActivityClazz);
        startActivity(newIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateHomeIcon();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AnalyticsUtils.activityStop(this);
    }

    private void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.navigation_drawer);
        mNavigationView.setNavigationItemSelectedListener(mMenuHandler);
        switch (mLocation.getListQuery()) {
            case inbox:
                checkNavMenu(R.id.nav_inbox);
                break;
            case nextTasks:
                checkNavMenu(R.id.nav_next_tasks);
                break;
            case dueTasks:
                checkNavMenu(R.id.nav_due_tasks);
                break;
            case project:
                checkNavMenu(R.id.nav_projects);
                break;
            case context:
                checkNavMenu(R.id.nav_contexts);
                break;
            case deferred:
                checkNavMenu(R.id.nav_deferred);
                break;
            case deleted:
                checkNavMenu(R.id.nav_deleted);
                break;
        }
    }

    private void checkNavMenu(int id) {
        mNavigationView.getMenu().findItem(id).setChecked(true);
    }

    protected int contentView(boolean isTablet) {
        return R.layout.entity_list_activity;
    }

    private Location parseLocation(Bundle savedState) {
        Location location;
        if (savedState != null) {
            location = savedState.getParcelable(LOCATION_KEY);
        } else {
            location = mLocationParser.parseIntent(getIntent());
        }
        return location;
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        Dialog dialog = null;
        if (id == WHATS_NEW_DIALOG) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.whats_new_dialog_title)
                    .setPositiveButton(R.string.ok_button_title, null)
                    .setMessage(R.string.whats_new_dialog_message)
                    .create();
        }

        return dialog;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if (!handleBackPress()) {
            super.onBackPressed();
        }
    }

    protected boolean handleBackPress() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mMenuHandler.onCreateOptionsMenu(menu, getMenuInflater());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mMenuHandler.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && !UiUtilities.showHomeAsUp(getResources(), mLocation)) {
            mDrawerLayout.openDrawer(mNavigationView);
            return true;
        }

        return mMenuHandler.onOptionsItemSelected(item);
    }

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
        updateHomeIcon();
        updateFab();
    }

    private void updateHomeIcon() {
        Toolbar actionBarToolbar = (Toolbar)findViewById(R.id.toolbar_actionbar);
        if (actionBarToolbar != null) {
            if (UiUtilities.showHomeAsUp(getResources(), mLocation)) {
                actionBarToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            } else {
                actionBarToolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
            }
        }
    }

    private void updateFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(ListFeatures.showAddFab(mLocation) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION_KEY, mLocation);

        super.onSaveInstanceState(outState);
    }

}
