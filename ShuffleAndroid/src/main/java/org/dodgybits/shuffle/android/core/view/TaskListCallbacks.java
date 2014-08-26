/*
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
package org.dodgybits.shuffle.android.core.view;

import android.database.Cursor;
import android.os.Parcelable;
import org.dodgybits.shuffle.android.list.model.ListQuery;

/**
 * A controller interface that is to receive user initiated events and handle them.
 */
public interface TaskListCallbacks {

    Cursor getTaskListCursor();

    String TASK_LIST_SCROLL_POSITION_INDEX = "index";
    String TASK_LIST_SCROLL_POSITION_OFFSET = "offset";

    /**
     * Gets the last save scroll position of the task list for the specified Folder.
     *
     * @return A {@link Parcelable} containing two ints,
     *         {@link #TASK_LIST_SCROLL_POSITION_INDEX} and
     *         {@link #TASK_LIST_SCROLL_POSITION_OFFSET}, or <code>null</code>
     */
    Parcelable getTaskListScrollPosition(ListQuery listQuery);

    /**
     * Sets the last save scroll position of the task list for the specified Folder for
     * restoration on returning to this list.
     *
     * @param savedPosition A {@link Parcelable} containing two ints,
     *            {@link #TASK_LIST_SCROLL_POSITION_INDEX} and
     *            {@link #TASK_LIST_SCROLL_POSITION_OFFSET}
     */
    void setTaskListScrollPosition(ListQuery listQuery, Parcelable savedPosition);
}
