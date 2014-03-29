package org.dodgybits.shuffle.android.core.view;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.AndroidException;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ContextSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.EntitySelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ProjectSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.preference.model.Preferences;

;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {
    private static final String TAG = "HomeListFragment";
    private static final String[] PROJECTION = new String[]{"_id"};

    private static final String POSITION = "position";

    private static IconNameCountListAdaptor.ListItem<HomeEntry>[] sListItems = null;

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerListView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
    private boolean mUserLearnedDrawer;

    private AsyncTask<?, ?, ?> mTask;

    private IconNameCountListAdaptor mAdaptor;

    private void createListItems() {
        if (sListItems == null) {
            String[] perspectives = getResources().getStringArray(R.array.perspectives).clone();
            int[] cachedCounts = Preferences.getTopLevelCounts(getActivity());
            int i = 0;
            sListItems = new IconNameCountListAdaptor.ListItem[] {
                    createTaskListItem(ListIcons.INBOX, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.inbox),
                    createTaskListItem(ListIcons.DUE_NEXT_MONTH, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.dueNextMonth),
                    createTaskListItem(ListIcons.NEXT_TASKS, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.nextTasks),
                    createListItem(ListIcons.PROJECTS, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.project, ProjectSelector.newBuilder().build()),
                    createListItem(ListIcons.CONTEXTS, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.context, ContextSelector.newBuilder().build()),
                    createTaskListItem(ListIcons.CUSTOM, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.custom),
                    createTaskListItem(ListIcons.TICKLER, perspectives[i], getInitialCount(cachedCounts, i++), ListQuery.tickler)
            };
        };
    }

    private String getInitialCount(int[] cachedCounts, int index) {
        String result = "";
        if (cachedCounts != null && cachedCounts.length > index) {
            result = String.valueOf(cachedCounts[index]);
        }
        return result;
    }

    private IconNameCountListAdaptor.ListItem<HomeEntry> createTaskListItem(int iconResId, String name,
                                                                            String initialCount, ListQuery query) {
        final TaskSelector selector = TaskSelector.newBuilder().setListQuery(query).build();
        return createListItem(iconResId, name, initialCount, query, selector);
    }

    private IconNameCountListAdaptor.ListItem<HomeEntry> createListItem(int iconResId, String name, String initialCount,
                                                                        ListQuery query, EntitySelector selector) {
        HomeEntry entry = new HomeEntry(query, selector);
        IconNameCountListAdaptor.ListItem<HomeEntry> listItem = new IconNameCountListAdaptor.ListItem<>(iconResId, name, entry);
        listItem.setCount(initialCount);
        return listItem;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        // Select either the default item (0) or the last selected item.
        mCurrentSelectedPosition = fetchSelectedPosition(savedInstanceState);
        selectItem(mCurrentSelectedPosition);

        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
        createListItems();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
        return mDrawerListView;
    }

    @Override
    public void onResume() {
        super.onResume();

        setupAdaptor();
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);

        mTask = new CalculateCountTask().execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(POSITION, mCurrentSelectedPosition);
    }

    private void setupAdaptor() {
        mAdaptor = new IconNameCountListAdaptor(
                getActivity(), R.layout.list_item_view, sListItems);
        mDrawerListView.setAdapter(mAdaptor);
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

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
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

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private int fetchSelectedPosition(Bundle savedInstanceState) {
        int position = -1;
        if (savedInstanceState != null) {
            position = savedInstanceState.getInt(POSITION, -1);
        }
        if (position < 0) {
            if (mCallbacks != null) {
                position = mCallbacks.getRequestedPosition();
            } else {
                position = 0;
            }
        }
        return position;
    }

    private void selectItem(int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
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
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);

        int getRequestedPosition();
    }

    private class CalculateCountTask extends AsyncTask<Void, Void, Void> {


        @Override
        protected Void doInBackground(Void... params) {
            int length = sListItems.length;
            StringBuilder cachedCountStr = new StringBuilder();
            for (int i = 0; i < length; i++) {
                IconNameCountListAdaptor.ListItem<HomeEntry> item = sListItems[i];
                String count = item.getPayload().getCount(getActivity());
                item.setCount(count);
                publishProgress();

                cachedCountStr.append(count);
                if (i < length - 1) {
                    cachedCountStr.append(",");
                }
            }

            // updated cached counts
            SharedPreferences.Editor editor = Preferences.getEditor(getActivity());
            editor.putString(Preferences.TOP_LEVEL_COUNTS_KEY, cachedCountStr.toString());
            editor.commit();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mAdaptor.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mTask = null;
        }

    }

    private static class HomeEntry {
        final ListQuery mListQuery;
        final EntitySelector mSelector;

        private HomeEntry(ListQuery listQuery, EntitySelector selector) {
            mListQuery = listQuery;
            mSelector = selector;
        }

        public String getCount(Activity activity) {
            EntitySelector selector = mSelector.builderFrom().applyListPreferences(activity,
                    ListSettingsCache.findSettings(mListQuery)).build();
            Cursor cursor = activity.getContentResolver().query(
                    selector.getContentUri(),
                    PROJECTION,
                    selector.getSelection(activity),
                    selector.getSelectionArgs(),
                    selector.getSortOrder());
            int count = cursor.getCount();
            cursor.close();
            return String.valueOf(count);
        }

    };

}
