/*
 * Copyright (C) 2009 Android Shuffle Open Source Project
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

package org.dodgybits.shuffle.android.core.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.fragment.HelpListFragment;
import org.dodgybits.shuffle.android.core.util.AnalyticsUtils;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.roboguice.RoboAppCompatActivity;
import roboguice.inject.ContextScopedProvider;

import java.util.List;
import java.util.Map;

public class HelpActivity extends RoboAppCompatActivity {
    private static final String TAG = "HelpActivity";
    
    public static final String QUERY_NAME = "queryName";

    private MyAdapter mAdapter;
    private ViewPager mPager;
    private List<Fragment> mFragments;
    private Map<ListQuery,Integer> mQueryIndex;

    @Inject
    private ContextScopedProvider<HelpListFragment> mHelpListFragmentProvider;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pager);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);

        initFragments();

        mAdapter = new MyAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        int position = getRequestedPosition(getIntent());
        mPager.setCurrentItem(position);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = LocationParser.createIntent(this, Location.home());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
        }

        return false;
    }

    private void initFragments() {
        mFragments = Lists.newArrayList();
        
        String[] helpTitles = getResources().getStringArray(R.array.help_screens);
        int[] helpKeys = getResources().getIntArray(R.array.help_keys);
        int length = helpTitles.length;
        if (helpKeys.length != length) {
            Log.e(TAG, "Mismatch between keys length " + helpKeys.length + " and titles " + length);
            length = Math.min(length, helpKeys.length);
        }

        for (int i = 0; i < length; i++) {
            HelpListFragment fragment = mHelpListFragmentProvider.get(this);
            Bundle args = new Bundle();
            int index = helpKeys[i];
            String idKey = "help" + index;
            int contentId = getResources().getIdentifier(idKey, "string", getPackageName());
            CharSequence content = getText(contentId);
            args.putCharSequence(HelpListFragment.CONTENT, content);
            args.putString(HelpListFragment.TITLE, helpTitles[index]);

            fragment.setArguments(args);
            mFragments.add(fragment);
        }
        
        // few magic numbers for good luck...
        mQueryIndex = Maps.newHashMap();
        mQueryIndex.put(ListQuery.inbox, 1);
        mQueryIndex.put(ListQuery.dueTasks, 2);
        mQueryIndex.put(ListQuery.nextTasks, 3);
        mQueryIndex.put(ListQuery.project, 4);
        mQueryIndex.put(ListQuery.context, 5);
        mQueryIndex.put(ListQuery.custom, 6);
        mQueryIndex.put(ListQuery.tickler, 7);
    }

    private int getRequestedPosition(Intent intent) {
        int position = 0;
        String queryName = intent.getStringExtra(QUERY_NAME);
        if (queryName != null) {
            ListQuery query = ListQuery.valueOf(queryName);
            position = mQueryIndex.get(query);
            if (position == -1) {
                Log.e(TAG, "Couldn't find page of list " + queryName);
                position = 0;
            }
        }

        return position;
    }

    public class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }
    }
    
}
