/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.dodgybits.shuffle.android.widget;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.persistence.loader.ThrottlingCursorLoader;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;

/**
 * Loader for {@link TaskWidget}.
 *
 * This loader not only loads the messages, but also:
 * - The number of accounts.
 * - The message count shown in the widget header.
 *   It's currently just the same as the message count, but this will be updated to the unread
 *   counts for inboxes.
 */
class TaskWidgetLoader extends ThrottlingCursorLoader {
    private final Context mContext;

    private TaskSelector mSelector;

    /**
     * Cursor data specifically for use by the Task widget. Contains a cursor of tasks in
     * addition to a task count.
     */
    static class WidgetCursor extends CursorWrapper {
        private final int mTaskCount;

        public WidgetCursor(Cursor cursor, int taskCount) {
            super(cursor);
            mTaskCount = taskCount;
        }

        public int getTaskCount() {
            return mTaskCount;
        }
    }

    public TaskWidgetLoader(Context context) {
        // Initialize with no where clause.  We'll set it later.
        super(context, TaskProvider.Tasks.CONTENT_URI,
                TaskProvider.Tasks.FULL_PROJECTION, null, null,
                null);
        mContext = context;
    }

    @Override
    public Cursor loadInBackground() {
        final Cursor taskCursor = super.loadInBackground();
        
        final int messageCount = taskCursor.getCount();
        return new WidgetCursor(taskCursor, messageCount);
    }

    /**
     * Stop any pending load, reset selection parameters, and start loading.
     *
     * Must be called from the UI thread
     */
    void load(Location location) {
        reset();
        mSelector = TaskSelector.fromLocation(mContext, location);
        setSelectionAndArgs();
        startLoading();
    }

    /** Sets the loader's selection and arguments depending upon the account and mailbox */
    private void setSelectionAndArgs() {
        // Build the where cause (which can't be done on the UI thread.)
        setSelection(mSelector.getSelection(mContext));
        setSelectionArgs(mSelector.getSelectionArgs());
        setSortOrder(mSelector.getSortOrder());
    }
}
