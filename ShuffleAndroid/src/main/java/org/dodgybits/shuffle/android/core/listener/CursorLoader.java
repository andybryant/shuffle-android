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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.event.MainViewUpdateEvent;
import org.dodgybits.shuffle.android.core.event.TaskListCursorLoadedEvent;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.event.ListSettingsUpdatedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListAdaptor;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorLoader {
    private static final String TAG = "CursorLoader";
    private static final int LOADER_ID_TASK_LIST_LOADER = 1;

    private FragmentActivity mActivity;

    private EventManager mEventManager;

    private MainView mMainView;

    private TaskListContext mTaskListContext;

    @Inject
    public CursorLoader(Activity activity, EventManager eventManager) {
        mActivity = (FragmentActivity) activity;
        mEventManager = eventManager;
    }

    public void onViewUpdated(@Observes MainViewUpdateEvent event) {
        Log.d(TAG, "Received view update event " + event);
        mMainView = event.getMainView();
        startLoading();
    }

    public void onListSettingsUpdated(@Observes ListSettingsUpdatedEvent event) {
        if (event.getListQuery().equals(mMainView.getListQuery())) {
            // our list settings changed - reload list (even if this list isn't currently visible)
            restartLoading();
        }
    }

    private void startLoading() {
        Log.d(TAG, "Creating relevant list cursor for " + mMainView);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case TASK:
            case TASK_LIST:
                updateTaskListContext();
                lm.initLoader(LOADER_ID_TASK_LIST_LOADER, null, LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void restartLoading() {
        Log.d(TAG, "Refreshing list cursor");
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case TASK:
            case TASK_LIST:
                updateTaskListContext();
                lm.restartLoader(LOADER_ID_TASK_LIST_LOADER, null, LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void updateTaskListContext() {
        mTaskListContext = null;
        ListQuery listQuery = mMainView.getListQuery();
        if (listQuery == ListQuery.context) {
            mTaskListContext = TaskListContext.createForContext(Id.create(mMainView.getEntityId()));
        } else if (listQuery == ListQuery.project) {
            mTaskListContext = TaskListContext.createForProject(Id.create(mMainView.getEntityId()));
        } else {
            mTaskListContext = TaskListContext.create(listQuery);
        }
    }

    /**
     * Loader callbacks for tasks list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    final TaskListContext listContext = mTaskListContext;
                    return TaskListAdaptor.createLoader(mActivity, listContext);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    Log.d(TAG, "IN AAC.TaskCursor.onLoadFinished");

                    mEventManager.fire(new TaskListCursorLoadedEvent(c, mTaskListContext));
//                    addTaskList();
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    // mTaskListCursor = null;
                }
            };

}
