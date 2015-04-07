package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.AndroidException;
import android.util.Log;
import android.view.*;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ContextSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.EntitySelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ProjectSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.util.UiUtilities;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.preference.model.Preferences;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends RoboFragment {
    private static final String TAG = "NavDrawerFragment";
    private static final String[] PROJECTION = new String[]{"_id"};

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    @Inject
    private EventManager mEventManager;

    @Inject
    private LocationProvider mLocationProvider;

    private DrawerLayout mDrawerLayout;
    private View mFragmentContainerView;
    private ViewGroup mDrawerItemsListContainer;
    private List<View> mNavDrawerItemViews;

    private boolean mUserLearnedDrawer;

    private Cursor mContextCursor;
    private Cursor mContextCountCursor;
    private Cursor mProjectCursor;
    private Cursor mProjectCountCursor;

    private Map<Location,NavDrawerEntry> mDrawerEntryMap;


    private AsyncTask<?, ?, ?> mTask;

    private Location mLocation;

    private void onViewChange(@Observes NavigationRequestEvent event) {
        mLocation = event.getLocation();
        updateSelection();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocation = mLocationProvider.getLocation();

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        mDrawerLayout.setStatusBarBackgroundColor(
                getResources().getColor(R.color.theme_primary_dark));

        ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById(R.id.navdrawer);

        if (navDrawer != null) {
            // TODO setup account view
        }

        Toolbar actionBarToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar_actionbar);
        if (actionBarToolbar != null) {
            actionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
            actionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDrawerLayout.openDrawer(Gravity.START);
                }
            });
        }

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                if (!isAdded()) {
                    return;
                }

                onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (!isAdded()) {
                    return;
                }

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager
                            .getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                onNavDrawerSlide(slideOffset);
            }

        });

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        // populate the nav drawer with the correct items
        fetchItems();

    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        // TODO - auto hide of action bar
    }

    protected void onNavDrawerSlide(float offset) {}

    public boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    public void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
    }

    private void fetchItems() {
        mEventManager.fire(new LoadListCursorEvent(ViewMode.CONTEXT_LIST));
        mEventManager.fire(new LoadListCursorEvent(ViewMode.PROJECT_LIST));
    }

    private void onContextsLoaded(@Observes ContextListCursorLoadedEvent event) {
        mContextCursor = event.getCursor();
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.CONTEXT_LIST));
        populateNavDrawer();
    }

    private void onProjectsLoaded(@Observes ProjectListCursorLoadedEvent event) {
        mProjectCursor = event.getCursor();
        mEventManager.fire(new LoadCountCursorEvent(ViewMode.PROJECT_LIST));
        populateNavDrawer();
    }

    /** Populates the navigation drawer with the appropriate items. */
    private void populateNavDrawer() {
        if (mContextCursor == null || mProjectCursor == null) {
            return;
        }

        Log.d(TAG, "Populating nav drawer");

        mDrawerItemsListContainer = (ViewGroup) getView().findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        getView().post(new Runnable() {
            @Override
            public void run() {
                mDrawerItemsListContainer.removeAllViews();
                mDrawerEntryMap = new HashMap<>();
                mNavDrawerItemViews = new ArrayList<>();

                int[] cachedCounts = Preferences.getTopLevelCounts(getActivity());

                addTaskItem(getInitialCount(cachedCounts, 0), ListIcons.INBOX, ListQuery.inbox);
                addTaskItem(getInitialCount(cachedCounts, 2), ListIcons.NEXT_TASKS, ListQuery.nextTasks);
                addTaskItem(getInitialCount(cachedCounts, 1), ListIcons.DUE_TASKS, ListQuery.dueTasks);
                addTaskItem(getInitialCount(cachedCounts, 5), ListIcons.CUSTOM, ListQuery.custom);
                addTaskItem(getInitialCount(cachedCounts, 6), ListIcons.TICKLER, ListQuery.tickler);
                addSeparator(mDrawerItemsListContainer);
                addProjectListItem(getInitialCount(cachedCounts, 3), ListIcons.PROJECTS);
                addSeparator(mDrawerItemsListContainer);
                addContextListItem(getInitialCount(cachedCounts, 4), ListIcons.CONTEXTS);
                addSeparator(mDrawerItemsListContainer);
                addSettings();
                addHelp();

                for (View view : mNavDrawerItemViews) {
                    mDrawerItemsListContainer.addView(view);
                }

                mTask = new CalculateCountTask().execute(mDrawerEntryMap.values().toArray(new NavDrawerEntry[0]));

                // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
                // per the navigation drawer design guidelines.
                if (!mUserLearnedDrawer) {
                    mDrawerLayout.openDrawer(mFragmentContainerView);
                }

                updateSelection();
            }
        });

    }

    private void addTaskItem(Integer count, int iconResId, ListQuery listQuery) {
        String name = UiUtilities.getTitle(getResources(), listQuery);
        final Location location = Location.newBuilder().setListQuery(listQuery).build();
        final TaskSelector selector = TaskSelector.newBuilder().setListQuery(listQuery).build();
        addEntityItem(count, iconResId, name, location, selector);
    }

    private void addProjectListItem(Integer count, int iconResId) {
        String name = UiUtilities.getTitle(getResources(), ListQuery.project);
        final Location location = Location.newBuilder().setListQuery(ListQuery.project).build();
        addEntityItem(count, iconResId, name, location, ProjectSelector.newBuilder().build());
    }

    private void addContextListItem(Integer count, int iconResId) {
        String name = UiUtilities.getTitle(getResources(), ListQuery.context);
        final Location location = Location.newBuilder().setListQuery(ListQuery.context).build();
        addEntityItem(count, iconResId, name, location, ContextSelector.newBuilder().build());
    }

    private void addSettings() {
        NavDrawerEntityView view = new NavDrawerEntityView(getActivity());
        view.init(R.drawable.ic_drawer_settings, getString(R.string.menu_preferences), null);
        mNavDrawerItemViews.add(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDrawerLayout != null) {
                    mDrawerLayout.closeDrawer(mFragmentContainerView);
                }
                mEventManager.fire(NavigationRequestEvent.viewSettings());
            }
        });
    }

    private void addHelp() {
        NavDrawerEntityView view = new NavDrawerEntityView(getActivity());
        view.init(R.drawable.ic_drawer_help, getString(R.string.menu_help), null);
        mNavDrawerItemViews.add(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDrawerLayout != null) {
                    mDrawerLayout.closeDrawer(mFragmentContainerView);
                }
                // TODO - pick listquery for current location
                mEventManager.fire(NavigationRequestEvent.viewHelp(ListQuery.context));
            }
        });
    }


    private void addEntityItem(Integer count, int iconResId, String name, final Location location,
                         EntitySelector selector) {
        NavDrawerEntityView view = new NavDrawerEntityView(getActivity());
        view.init(iconResId, name, count);
        NavDrawerEntry entry = new NavDrawerEntry(count, location, selector, view);
        mDrawerEntryMap.put(entry.getLocation(), entry);
        mNavDrawerItemViews.add(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDrawerLayout != null) {
                    mDrawerLayout.closeDrawer(mFragmentContainerView);
                }
                mEventManager.fire(new NavigationRequestEvent(location));
            }
        });
    }

    private void addSeparator(ViewGroup container) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.navdrawer_separator, container, false);
        mNavDrawerItemViews.add(view);
    }

    private Integer getInitialCount(int[] cachedCounts, int index) {
        Integer result = null;
        if (cachedCounts != null && cachedCounts.length > index) {
            result = cachedCounts[index];
        }
        return result;
    }

    private void onContextCountLoaded(@Observes ContextTaskCountCursorLoadedEvent event) {
        final Location location = Location.newBuilder().setListQuery(ListQuery.context).build();
        // TODO iterate through cursor - construct location, find entry and update
    }

    private void onProjectCountLoaded(@Observes ProjectTaskCountCursorLoadedEvent event) {
        // TODO iterate through cursor - construct mainView, find entry and update

    }

    private void updateSelection() {
        if (mLocation == null || mDrawerEntryMap == null) {
            return;
        }

        for (Map.Entry<Location, NavDrawerEntry> entry : mDrawerEntryMap.entrySet()) {
            entry.getValue().mListener.setViewSelected(entry.getKey().equals(mLocation));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {

            // TODO add global menu items
//            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // TODO add global options
//            case R.id.action_example:
//                Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT).show();
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(getVersionedTitle());
    }

    private String getVersionedTitle() {
        String title = getString(R.string.app_name);
        try {
            PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            title += " " + info.versionName;
        } catch (AndroidException e) {
            Log.e(TAG, "Failed to add version to title: " + e.getMessage());
        }
        return title;
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    private class CalculateCountTask extends AsyncTask<NavDrawerEntry, Void, Void> {

        @Override
        protected Void doInBackground(NavDrawerEntry... entries) {
            int length = entries.length;
            StringBuilder cachedCountStr = new StringBuilder();
            int i = 0;
            for (NavDrawerEntry entry : entries) {
                int count = entry.updateCount(getActivity());
                entry.setCount(count);

                cachedCountStr.append(count);
                if (i < length - 1) {
                    cachedCountStr.append(",");
                }
                i++;
            }

            // updated cached counts
            SharedPreferences.Editor editor = Preferences.getEditor(getActivity());
            editor.putString(Preferences.TOP_LEVEL_COUNTS_KEY, cachedCountStr.toString());
            editor.commit();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mTask = null;
        }

    }

    private static class NavDrawerEntry {
        private Integer mCount;
        final Location mLocation;
        final EntitySelector mSelector;
        final NavDrawerEntityListener mListener;

        private NavDrawerEntry(Integer count, Location location, EntitySelector selector, NavDrawerEntityListener listener) {
            mCount = count;
            mLocation = location;
            mSelector = selector;
            mListener = listener;
        }

        public int updateCount(Activity activity) {
            EntitySelector selector = mSelector.builderFrom().applyListPreferences(activity,
                    ListSettingsCache.findSettings(mLocation.getListQuery())).build();
            Cursor cursor = activity.getContentResolver().query(
                    selector.getContentUri(),
                    PROJECTION,
                    selector.getSelection(activity),
                    selector.getSelectionArgs(),
                    selector.getSortOrder());
            int count = cursor.getCount();
            cursor.close();
            return count;
        }

        public Location getLocation() {
            return mLocation;
        }

        public Integer getCount() {
            return mCount;
        }

        public void setCount(Integer count) {
            mCount = count;
            mListener.onUpdateCount(count);
        }

    };


}
