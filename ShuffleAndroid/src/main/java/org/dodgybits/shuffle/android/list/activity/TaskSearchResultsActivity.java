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

import android.content.Intent;
import com.google.inject.Inject;
import org.dodgybits.android.shuffle.R;
import org.dodgybits.shuffle.android.core.activity.AbstractMainActivity;
import org.dodgybits.shuffle.android.core.event.LocationUpdatedEvent;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.LocationParser;
import org.dodgybits.shuffle.android.list.view.task.TaskRecyclerFragment;
import roboguice.event.Observes;
import roboguice.inject.ContextScopedProvider;

public class TaskSearchResultsActivity extends AbstractMainActivity {
    public static final String TAG = "TaskSearchResultsAct";

    @Override
    public Location.LocationActivity getLocationActivity() {
        return Location.LocationActivity.TaskSearch;
    }

    @Override
    protected void validateLocation(Location location) {
        String action = getIntent().getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            redirect(TaskListActivity.class, location);
        } else {
            Intent intent = LocationParser.createIntent(this, location);
            startActivity(intent);
            finish();
        }
    }

}
