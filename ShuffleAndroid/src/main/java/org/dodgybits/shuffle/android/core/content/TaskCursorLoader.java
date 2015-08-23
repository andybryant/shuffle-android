/*
 * Copyright (C) 2012 Android Shuffle Open Source Project
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
package org.dodgybits.shuffle.android.core.content;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.list.view.task.TaskRecyclerFragment;
import org.dodgybits.shuffle.android.persistence.provider.TaskProvider;

public class TaskCursorLoader extends CursorLoader {
    protected final Context mContext;

    private TaskSelector mSelector;

    /**
     * Creates the loader for {@link TaskRecyclerFragment}.
     *
     * @return always of {@link Cursor}.
     */
    public static Loader<Cursor> createLoader(Context context, TaskListContext listContext) {
        return new TaskCursorLoader(context, listContext);
    }

    public TaskCursorLoader(Context context, TaskListContext listContext) {
        this(context, listContext.createSelectorWithPreferences(context));
    }

    private TaskCursorLoader(Context context, TaskSelector selector) {
        // Initialize with no where clause.  We'll set it later.
        super(context, selector.getContentUri(),
                TaskProvider.Tasks.FULL_PROJECTION, null, null,
                null);
        mSelector = selector;
        mContext = context;
    }

    @Override
    public Cursor loadInBackground() {
        // Build the where cause (which can't be done on the UI thread.)
        setSelection(mSelector.getSelection(mContext));
        setSelectionArgs(mSelector.getSelectionArgs());
        setSortOrder(mSelector.getSortOrder());
        // Then do a query to get the cursor
        return super.loadInBackground();
    }

}
