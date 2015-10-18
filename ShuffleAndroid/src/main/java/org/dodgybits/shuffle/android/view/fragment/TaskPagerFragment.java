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

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ImageButton;

import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.NavigationRequestEvent;
import org.dodgybits.shuffle.android.core.listener.CursorProvider;
import org.dodgybits.shuffle.android.core.listener.EntityUpdateListener;
import org.dodgybits.shuffle.android.core.listener.ListSettingsListener;
import org.dodgybits.shuffle.android.core.listener.LocationProvider;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.model.Task;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.editor.activity.DateTimePickerActivity;
import org.dodgybits.shuffle.android.list.event.UpdateTasksCompletedEvent;
import org.dodgybits.shuffle.android.list.event.UpdateTasksDeletedEvent;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.fragment.RoboFragment;


public class TaskPagerFragment extends RoboFragment
        implements ViewPager.OnPageChangeListener, View.OnClickListener {
    private static final String TAG = "TaskPagerFragment";

    private static final int DEFERRED_CODE = 102;

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

    @Inject
    private TaskPersister mTaskPersister;

    private ImageButton mEditButton;
    private ImageButton mCompleteButton;
    private ImageButton mDeferButton;
    private ImageButton mDeleteButton;


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

        mEditButton = (ImageButton) getActivity().findViewById(R.id.edit_button);
        mCompleteButton = (ImageButton) getActivity().findViewById(R.id.complete_button);
        mDeferButton = (ImageButton) getActivity().findViewById(R.id.defer_button);
        mDeleteButton = (ImageButton) getActivity().findViewById(R.id.delete_button);

        mEditButton.setOnClickListener(this);
        mCompleteButton.setOnClickListener(this);
        mDeferButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);

        updateCursor();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Got resultCode " + resultCode + " with data " + data);
        switch (requestCode) {
            case DEFERRED_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        long deferred = data.getLongExtra(DateTimePickerActivity.DATETIME_VALUE, 0L);
                        Task.Builder builder = Task.newBuilder().mergeFrom(getSelectedTask())
                                .setStartDate(deferred);
                        builder.getChangeSet().showFromChanged();
                        mTaskPersister.update(builder.build());
                    }
                }
                break;

            default:
                Log.e(TAG, "Unknown requestCode: " + requestCode);
        }
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
        if (cursor == null) {
            return;
        }
        if (cursor == mCursor) {
            if (mLocation != null && mPager != null) {
                mPager.setCurrentItem(mLocation.getSelectedIndex());
                updateToolbar();
            }
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
            updateToolbar();
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
            updateToolbar();
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
            return mCursor.getLong(0);
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

        public Task getTask(int position) {
            mCursor.moveToPosition(position);
            return mTaskPersister.read(mCursor, false);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_button: {
                editTask();
                break;
            }
            case R.id.complete_button: {
                toggleComplete();
                break;
            }
            case R.id.defer_button: {
                deferTask();
                break;
            }
            case R.id.delete_button: {
                toggleDeleted();
                break;
            }
        }
    }

    private void editTask() {
        Log.d(TAG, "Editing the action");
        Task task = getSelectedTask();
        Location location = Location.editTask(task.getLocalId());
        mEventManager.fire(new NavigationRequestEvent(location));
    }

    private void deferTask() {
        Task task = getSelectedTask();
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(DateTimePickerActivity.TYPE);
        intent.putExtra(DateTimePickerActivity.DATETIME_VALUE, task.getStartDate());
        intent.putExtra(DateTimePickerActivity.TITLE, getString(R.string.title_deferred_picker));
        startActivityForResult(intent, DEFERRED_CODE);
    }

    private void toggleComplete() {
        Task task = getSelectedTask();
        Id taskId = task.getLocalId();
        Log.d(TAG, "Toggling complete on task " + task.getDescription() +
                " id=" + taskId + " tag=" + getTag());
        mEventManager.fire(new UpdateTasksCompletedEvent(taskId, !task.isComplete()));
    }

    private void toggleDeleted() {
        Task task = getSelectedTask();
        Id taskId = task.getLocalId();
        Log.d(TAG, "Toggling deleted on task " + task.getDescription() +
                " id=" + taskId + " tag=" + getTag());
        mEventManager.fire(new UpdateTasksDeletedEvent(taskId, !task.isDeleted()));
    }

    private void updateToolbar() {
        Task task = getSelectedTask();
        if (task != null) {
            mDeleteButton.setImageResource(task.isDeleted() ? R.drawable.ic_restore_black_24dp :
                    R.drawable.ic_delete_black_24dp);
            mCompleteButton.setImageResource(task.isComplete() ? R.drawable.ic_done_green_24dp :
                    R.drawable.ic_done_black_24dp);
        }
    }

    private Task getSelectedTask() {
        return mAdapter == null ? null : mAdapter.getTask(mPager.getCurrentItem());
    }

}
