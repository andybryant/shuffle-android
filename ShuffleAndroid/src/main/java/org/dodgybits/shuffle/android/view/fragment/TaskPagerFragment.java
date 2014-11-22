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
package org.dodgybits.shuffle.android.view.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.MainActivity;
import org.dodgybits.shuffle.android.core.event.LoadTaskFragmentEvent;
import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.NavigationListener;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.util.ObjectUtils;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.event.ViewContextEvent;
import org.dodgybits.shuffle.android.list.event.ViewProjectEvent;
import org.dodgybits.shuffle.android.list.event.ViewTaskSearchResultsEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;

public class TaskPagerFragment extends RoboFragment {
    private static final String TAG = "TaskPagerFragment";

    public static final String INITIAL_POSITION = "selectedIndex";
    public static final String TASK_LIST_CONTEXT = "taskListContext";

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

    @Inject
    private CursorProvider mCursorProvider;

    MyAdapter mAdapter;

    ViewPager mPager;

    TaskListContext mListContext;

    int mPosition = 0;

    public TaskPagerFragment() {
        Log.d(TAG, "Created " + this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadConfiguration(savedInstanceState);
    }

        @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPager = (ViewPager)getActivity().findViewById(R.id.pager);

        loadConfiguration(savedInstanceState);
        updateCursor();
    }

    private void loadConfiguration(Bundle savedInstanceState) {
        if (mListContext == null) {
            Bundle bundle = savedInstanceState == null ? getArguments() : savedInstanceState;
            mListContext = bundle.getParcelable(TASK_LIST_CONTEXT);
            mPosition = bundle.getInt(INITIAL_POSITION, 0);
        }
    }

    public void onViewUpdate(@Observes MainViewUpdateEvent event) {
        TaskListContext newListContext = TaskListContext.create(event.getMainView());
        if (!ObjectUtils.equals(mListContext, newListContext)) {
            mListContext = newListContext;
        }
        mPosition = event.getMainView().getSelectedIndex();
        if (mPager != null) {
            mPager.setCurrentItem(mPosition);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                ListQuery listQuery = mListContext.getListQuery();
                switch (listQuery) {
                    case project:
                        mEventManager.fire(new ViewProjectEvent(mListContext.getEntityId()));
                        break;
                    case context:
                        mEventManager.fire(new ViewContextEvent(mListContext.getEntityId()));
                        break;
                    case search:
                        mEventManager.fire(new ViewTaskSearchResultsEvent(mListContext.getSearchQuery()));
                        break;
                    default:
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(MainView.QUERY_NAME, listQuery.name());
                        startActivity(intent);
                        break;
                }
                getActivity().finish();
                return true;
        }

        return false;
    }

    public void onCursorLoaded(@Observes LoadTaskFragmentEvent event) {
        updateCursor();
    }
    
    private void updateCursor() {
        Log.d(TAG, "Swapping cursor " + this);
        // Update the list

        if (getActivity() == null) {
            Log.wtf(TAG, "Activity not set on " + this);
            return;
        }

        Cursor cursor = mCursorProvider.getCursor();
        if (cursor != null) {
            mAdapter = new MyAdapter(getActivity().getSupportFragmentManager(), cursor);
            mPager.setAdapter(mAdapter);
            mPager.setCurrentItem(mPosition);
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
