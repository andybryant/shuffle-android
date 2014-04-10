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

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskListFragment;
import org.dodgybits.shuffle.android.roboguice.RoboActionBarActivity;
import org.dodgybits.shuffle.android.view.activity.TaskViewActivity;
import org.dodgybits.shuffle.android.view.fragment.TaskViewFragment;
import roboguice.inject.ContextScopedProvider;

public class TaskSearchResultsActivity extends RoboActionBarActivity {

    @Inject
    private TaskListFragment mTaskListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_entity_list);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME |
                ActionBar.DISPLAY_SHOW_TITLE);

        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // handles a click on a search suggestion; launches activity to show task
            Intent viewIntent = new Intent(this, TaskViewActivity.class);
            viewIntent.setData(intent.getData());
            startActivity(viewIntent);
            finish();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);
            showResults(query);
        }
    }

    private void showResults(String query) {
        TaskListContext taskListContext = TaskListContext.createForSearch(query);
        Bundle args = new Bundle();
        args.putParcelable(TaskListFragment.ARG_LIST_CONTEXT, taskListContext);
        mTaskListFragment.setArguments(args);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, mTaskListFragment);
        ft.commit();
    }
}
