package org.dodgybits.shuffle.android.view.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.NavigationListener;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.event.ViewContextEvent;
import org.dodgybits.shuffle.android.list.event.ViewProjectEvent;
import org.dodgybits.shuffle.android.list.event.ViewTaskSearchResultsEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;
import org.dodgybits.shuffle.android.view.fragment.TaskViewFragment;
import roboguice.event.EventManager;

/**
 * View a task in the context of a given task list.
 * Each task is a separate page on a PageViewer.
 */
public class TaskPagerActivity extends RoboActionBarActivity {
    private static final String TAG = "TaskPagerActivity";

    public static final String INITIAL_POSITION = "selectedIndex";
    public static final String TASK_LIST_CONTEXT = "taskListContext";

    private static final int LOADER_ID_TASK_LIST_LOADER = 1;

    @Inject
    TaskPersister mPersister;

    @Inject
    TaskEncoder mEncoder;

    @Inject
    private NavigationListener mNavigationListener;

    @Inject
    private EventManager mEventManager;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    MyAdapter mAdapter;

    ViewPager mPager;

    TaskListContext mListContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pager);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);

        mPager = (ViewPager)findViewById(R.id.pager);

        startLoading();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                ListQuery listQuery = getListContext().getListQuery();
                switch (listQuery) {
                    case project:
                        mEventManager.fire(new ViewProjectEvent(getListContext().getEntityId()));
                        break;
                    case context:
                        mEventManager.fire(new ViewContextEvent(getListContext().getEntityId()));
                        break;
                    case search:
                        mEventManager.fire(new ViewTaskSearchResultsEvent(getListContext().getSearchQuery()));
                        break;
                    default:
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(MainView.QUERY_NAME, listQuery.name());
                        startActivity(intent);
                        break;
                }
                finish();
                return true;
        }

        return false;
    }

    private void startLoading() {
        Log.d(TAG, "Creating list cursor");
        final LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_ID_TASK_LIST_LOADER, getIntent().getExtras(), LOADER_CALLBACKS);
    }

    private TaskListContext getListContext() {
        if (mListContext == null) {
            mListContext = getIntent().getParcelableExtra(TASK_LIST_CONTEXT);
        }
        return mListContext;
    }


    /**
     * Loader callbacks for task list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                int mInitialPosition;
                
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    mInitialPosition = args.getInt(INITIAL_POSITION, 0);
                    return new TaskCursorLoader(TaskPagerActivity.this, getListContext());
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    mAdapter = new MyAdapter(getSupportFragmentManager(), c);
                    mPager.setAdapter(mAdapter);
                    mPager.setCurrentItem(mInitialPosition);
                }


                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private static class TaskCursorLoader extends CursorLoader {
        protected final Context mContext;

        private TaskSelector mSelector;

        public TaskCursorLoader(Context context, TaskListContext listContext) {
            // Initialize with no where clause.  We'll set it later.
            super(context, TaskProvider.Tasks.CONTENT_URI,
                    TaskProvider.Tasks.FULL_PROJECTION, null, null,
                    null);
            mSelector = listContext.createSelectorWithPreferences(context);
            mContext = context;
        }

        @Override
        public Cursor loadInBackground() {
            // Build the where cause (which can't be done on the UI thread.)
            setSelection(mSelector.getSelection(mContext));
            setSelectionArgs(mSelector.getSelectionArgs());
            setSortOrder(mSelector.getSortOrder());
            // Then do a query to get the cursor
            return super.loadInBackground();
        }

    }

    public class MyAdapter extends FragmentPagerAdapter {
        Cursor mCursor;
        TaskViewFragment[] mFragments;
        
        public MyAdapter(FragmentManager fm, Cursor c) {
            super(fm);
            mCursor = c;
            mFragments = new TaskViewFragment[getCount()];
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public Fragment getItem(int position) {
            TaskViewFragment fragment = mFragments[position];
            if (fragment == null) {
                Log.d(TAG, "Creating fragment item " + position);
                mCursor.moveToPosition(position);
                Task task = mPersister.read(mCursor);
                Bundle args = new Bundle();
                args.putInt(TaskViewFragment.INDEX, position);
                args.putInt(TaskViewFragment.COUNT, mCursor.getCount());
                mEncoder.save(args, task);
                fragment = TaskViewFragment.newInstance(args);
                mFragments[position] = fragment;
            }
            return fragment;
        }
    }
    
}
