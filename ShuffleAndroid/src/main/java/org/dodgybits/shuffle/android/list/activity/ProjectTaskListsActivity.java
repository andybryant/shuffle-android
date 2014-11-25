package org.dodgybits.shuffle.android.list.activity;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.NavigationListener;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.persistence.ProjectPersister;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.content.ProjectCursorLoader;
import org.dodgybits.shuffle.android.list.event.ViewPreferencesEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import roboguice.activity.RoboActionBarActivity;
import roboguice.event.EventManager;
import roboguice.inject.ContextScopedProvider;

public class ProjectTaskListsActivity extends RoboActionBarActivity {
    public static final String TAG = "ProjectTaskListsActivity";
    public static final String INITIAL_POSITION = "initialPosition";
    public static final String INITIAL_ID = "initialId";

    private static final int LOADER_ID_CONTEXT_LIST_LOADER = 1;

    private MyAdapter mAdapter;

    private ViewPager mPager;

    @Inject
    private ProjectPersister mPersister;

    @Inject
    ContextScopedProvider<TaskListFragment> mTaskListFragmentProvider;

    @Inject
    private EventManager mEventManager;

    @Inject
    private NavigationListener mNavigationListener;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(TAG, "onCreate+");
        super.onCreate(icicle);
        setContentView(R.layout.fragment_entity_list_pager);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);

        mPager = (ViewPager)findViewById(R.id.pager);

        startLoading();

        // don't show soft keyboard unless user clicks on quick add box
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(MainView.QUERY_NAME, ListQuery.project.name());
                startActivity(intent);
                finish();
                return true;
            case R.id.action_preferences:
                Log.d(TAG, "Bringing up preferences");
                mEventManager.fire(new ViewPreferencesEvent());
                return true;
            case R.id.action_search:
                Log.d(TAG, "Bringing up search");
                onSearchRequested();
                return true;
        }

        return false;
    }

    private void startLoading() {
        Log.d(TAG, "Creating project list cursor");
        final LoaderManager lm = getSupportLoaderManager();
        final Bundle args = createLoaderArgs(getIntent());
        lm.initLoader(LOADER_ID_CONTEXT_LIST_LOADER, args, LOADER_CALLBACKS);
    }

    private Bundle createLoaderArgs(Intent intent) {
        Uri data = intent.getData();
        long contextId = ContentUris.parseId(data);

        Bundle args = new Bundle();
        args.putLong(INITIAL_ID, contextId);
        args.putInt(INITIAL_POSITION, intent.getIntExtra(INITIAL_POSITION, -1));

        return args;
    }

    /**
     * Loader callbacks for message list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                int mInitialPosition;
                long mInitialId;
                
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    mInitialPosition = args.getInt(INITIAL_POSITION, -1);
                    mInitialId = args.getLong(INITIAL_ID, -1L);
                    return new ProjectCursorLoader(ProjectTaskListsActivity.this);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    mAdapter = new MyAdapter(getSupportFragmentManager(), c);
                    mPager.setAdapter(mAdapter);
                    if (mInitialPosition == -1) {
                        mInitialPosition = mPersister.getPositionOfItemWithId(c, mInitialId);
                        if (mInitialPosition == -1) {
                            mInitialPosition = 0;
                        }
                    }
                    
                    mPager.setCurrentItem(mInitialPosition);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    public class MyAdapter extends FragmentPagerAdapter {
        Cursor mCursor;

        public MyAdapter(FragmentManager fm, Cursor c) {
            super(fm);
            mCursor = c;
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            Id projectId = mPersister.read(mCursor).getLocalId();
            TaskListContext listContext = TaskListContext.createForProject(projectId);
            TaskListFragment fragment = mTaskListFragmentProvider.get(ProjectTaskListsActivity.this);
            return fragment;
        }
        
    }
}
