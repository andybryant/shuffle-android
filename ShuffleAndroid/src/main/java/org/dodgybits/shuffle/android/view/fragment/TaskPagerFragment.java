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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.MainViewUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.MainViewUpdatingEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.MainViewProvider;
import org.dodgybits.shuffle.android.core.listener.NavigationListener;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.MainView;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;



public class TaskPagerFragment extends RoboFragment implements ViewPager.OnPageChangeListener {
    private static final String TAG = "TaskPagerFragment";

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

    @Inject
    private MainViewProvider mMainViewProvider;

    Cursor mCursor;

    MyAdapter mAdapter;

    ViewPager mPager;

    MainView mMainView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPager = (ViewPager)getActivity().findViewById(R.id.pager);
        mPager.setOnPageChangeListener(this);

        mMainView = mMainViewProvider.getMainView();
        updateCursor();
    }

    private void onViewUpdated(@Observes MainViewUpdatedEvent event) {
        mMainView = event.getMainView();
        updateCursor();
    }

    private void updateCursor() {
        updateCursor(mCursorProvider.getTaskListCursor());
    }

    private void updateCursor(Cursor cursor) {
        if (cursor == null || mCursor == cursor) {
            return;
        }
        if (getActivity() == null) {
            Log.w(TAG, "Activity not set on " + this);
            return;
        }

        Log.d(TAG, "Swapping cursor " + this);
        mCursor = cursor;

        if (mMainView != null) {
            mAdapter = new MyAdapter(getActivity().getSupportFragmentManager(), cursor);
            mPager.setAdapter(mAdapter);
            mPager.setCurrentItem(mMainView.getSelectedIndex());
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int newIndex) {
        if (mMainView.getSelectedIndex() != newIndex) {
            Log.d(TAG, "View changed from " + mMainView.getSelectedIndex() + " to " + newIndex);
            MainView newView = mMainView.builderFrom().setSelectedIndex(newIndex).build();
            mEventManager.fire(new MainViewUpdatingEvent(newView));
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

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
