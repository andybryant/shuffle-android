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
package org.dodgybits.shuffle.android.list.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.google.inject.Inject;

import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;

import roboguice.event.Observes;
import roboguice.inject.ContextScopedProvider;

public class TaskSearchResultsActivity extends AbstractMainActivity {
    public static final String TAG = "TaskSearchResultsAct";

    @Inject
    ContextScopedProvider<TaskListFragment> mTaskListFragmentProvider;

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskSearch;
    }

    @Override
    protected int contentView(boolean isTablet) {
        return R.layout.fragment_entity_list;
    }

    private void onLocationUpdated(@Observes LocationUpdatedEvent event) {
        showResults(event.getLocation().getSearchQuery());
    }

    private void showResults(String query) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment == null) {
            fragment = mTaskListFragmentProvider.get(this);

            // TODO populate fragment with search results

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, fragment);
            ft.commit();
        }
    }
}
