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
package org.dodgybits.shuffle.android.core.listener;

import android.database.Cursor;
import android.util.Log;
import org.dodgybits.shuffle.android.core.event.ContextListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.ProjectListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.TaskListCursorLoadedEvent;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorProvider {
    private static final String TAG = "CursorProvider";


    private Cursor mCursor;


    public void onCursorLoaded(@Observes TaskListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for context " + event.getTaskListContext());
        mCursor = event.getCursor();
    }

    public void onCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for context " + event.getCursor());
        mCursor = event.getCursor();
    }

    public void onCursorLoaded(@Observes ProjectListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for project " + event.getCursor());
        mCursor = event.getCursor();
    }


    public Cursor getCursor() {
        return mCursor;
    }
}
