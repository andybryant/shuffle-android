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

import com.google.inject.Inject;

import org.dodgybits.shuffle.android.core.event.ContextListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.CursorUpdatedEvent;
import org.dodgybits.shuffle.android.core.event.ProjectListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.event.TaskListCursorLoadedEvent;

import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorProvider {
    private static final String TAG = "CursorProvider";

    @Inject
    private EventManager mEventManager;

    private Cursor mTaskListCursor;
    private Cursor mContextListCursor;
    private Cursor mProjectListCursor;


    private void onCursorLoaded(@Observes TaskListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for context " + event.getTaskListContext());
        mTaskListCursor = event.getCursor();
        mEventManager.fire(new CursorUpdatedEvent());
    }

    private void onCursorLoaded(@Observes ContextListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for context " + event.getCursor());
        mContextListCursor = event.getCursor();
        mEventManager.fire(new CursorUpdatedEvent());
    }

    private void onCursorLoaded(@Observes ProjectListCursorLoadedEvent event) {
        Log.d(TAG, "Updating cursor for project " + event.getCursor());
        mProjectListCursor = event.getCursor();
        mEventManager.fire(new CursorUpdatedEvent());
    }

    public Cursor getTaskListCursor() {
        return mTaskListCursor;
    }

    public Cursor getContextListCursor() {
        return mContextListCursor;
    }

    public Cursor getProjectListCursor() {
        return mProjectListCursor;
    }

}
