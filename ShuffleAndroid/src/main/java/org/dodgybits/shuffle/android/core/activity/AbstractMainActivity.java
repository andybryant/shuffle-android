package org.dodgybits.shuffle.android.core.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.OnCreatedEvent;
import org.dodgybits.shuffle.android.core.listener.FragmentLoader;
import org.dodgybits.shuffle.android.core.listener.MainListeners;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.core.view.MenuHandler;
import org.dodgybits.shuffle.android.core.view.NavigationDrawerFragment;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.event.EventManager;
import roboguice.event.Observes;

public abstract class AbstractMainActivity extends RoboAppCompatActivity
        implements ShuffleAppCompatActivity {
    private static final String TAG = "AbstractMainActivity";

    private static final int WHATS_NEW_DIALOG = 5000;

    private static final String LOCATION_KEY = "AbstractMainActivity.location";


    private Location mLocation;

    @Inject
    private MainListeners mListeners;

    @Inject
    private EventManager mEventManager;

    @Inject
    private FragmentLoader mFragmentLoader;

    @Inject
    private MenuHandler mMenuHandler;

    @Inject
    private LocationParser mLocationParser;

    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

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

        // don't show soft keyboard unless user clicks on quick add box
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mLocationParser.setLocationActivity(getLocationActivity());
        Location location = parseLocation(savedState);
        mEventManager.fire(new LocationUpdatedEvent(location));

        final boolean tabletUi = UiUtilities.useTabletUI(this.getResources());
        Log.d(TAG, "Using tablet layout? " + tabletUi);
        setContentView(contentView(tabletUi));

        mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        if (mActionBarToolbar != null) {
            setSupportActionBar(mActionBarToolbar);
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                drawerLayout);

        mEventManager.fire(new OnCreatedEvent());
    }

    protected int contentView(boolean isTablet) {
        return isTablet ? R.layout.two_pane_activity : R.layout.one_pane_activity;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        boolean drawerOpen = mNavigationDrawerFragment.isDrawerOpen();
        return mMenuHandler.onCreateOptionsMenu(menu, getMenuInflater(), drawerOpen);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mMenuHandler.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMenuHandler.onOptionsItemSelected(item);
    }

    private void onViewChanged(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(LOCATION_KEY, mLocation);

        super.onSaveInstanceState(outState);
    }

}
