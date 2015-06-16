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
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.ListSettingsListener;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.encoding.TaskEncoder;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.Location;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;


public class TaskPagerFragment extends RoboFragment implements ViewPager.OnPageChangeListener {
    private static final String TAG = "TaskPagerFragment";

    @Inject
    private ListSettingsListener mListSettingsListener;

    @Inject
    private EventManager mEventManager;

    @Inject
    private EntityUpdateListener mEntityUpdateListener;

    @Inject
    private CursorProvider mCursorProvider;

    @Inject
    private LocationProvider mLocationProvider;

    Cursor mCursor;

    MyAdapter mAdapter;

    ViewPager mPager;

    Location mLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPager = (ViewPager)getActivity().findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);

        mLocation = mLocationProvider.getLocation();
        updateCursor();
    }

    private void onViewUpdated(@Observes LocationUpdatedEvent event) {
        mLocation = event.getLocation();
        updateCursor();
    }

    private void onCursorUpdated(@Observes CursorUpdatedEvent event) {
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

        Log.d(TAG, "Swapping cursor " + this + " location=" + mLocation);
        mCursor = cursor;

        if (mLocation != null) {
            mAdapter = new MyAdapter(getActivity().getSupportFragmentManager(), cursor);
            mPager.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            mPager.setCurrentItem(mLocation.getSelectedIndex());
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int newIndex) {
        if (mLocation.getSelectedIndex() != newIndex) {
            Location newView = mLocation.builderFrom().setSelectedIndex(newIndex).build();
            mEventManager.fire(new NavigationRequestEvent(newView));
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
            Log.d(TAG, "Created new adapter " + this);
        }

        @Override
        public int getCount() {
            return mCursor.getCount();
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            long taskId = mCursor.getLong(0);
            return taskId;
        }

        @Override
        public Fragment getItem(int position) {
            TaskViewFragment fragment = mFragments[position];
            if (fragment == null) {
                Bundle args = new Bundle();
                args.putLong(TaskViewFragment.ID, getItemId(position));
                fragment = TaskViewFragment.newInstance(args);
                mFragments[position] = fragment;
            }
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            int result = POSITION_NONE;
            if (object instanceof TaskViewFragment) {
                for (int i = 0; i < mFragments.length; i++) {
                    if (object == mFragments[i]) {
                        result = i;
                        break;
                    }
                }
            }
            return result;
        }
    }


}
