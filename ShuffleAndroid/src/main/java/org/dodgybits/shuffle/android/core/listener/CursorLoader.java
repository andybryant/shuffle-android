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
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.MainView;
import org.dodgybits.shuffle.android.list.content.ContextCursorLoader;
import org.dodgybits.shuffle.android.list.event.ListSettingsUpdatedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.list.view.task.TaskListAdaptor;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorLoader {
    private static final String TAG = "CursorLoader";
    private static final int LOADER_ID_TASK_LIST_LOADER = 1;
    private static final int LOADER_ID_CONTEXT_LIST_LOADER = 2;
    private static final int LOADER_ID_CONTEXT_TASK_COUNT_LOADER = 3;

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
        mTaskListContext = TaskListContext.create(mMainView);
        restartListLoading();
        restartCountLoading();
    }

    public void onListSettingsUpdated(@Observes ListSettingsUpdatedEvent event) {
        if (event.getListQuery().equals(mMainView.getListQuery())) {
            // our list settings changed - reload list (even if this list isn't currently visible)
            restartListLoading();
            restartCountLoading();
        }
    }

    public void onReloadListCursor(@Observes ReloadListCursorEvent event) {
        Log.d(TAG, "Refreshing list cursor");
        restartListLoading();
    }

    public void onReloadCountCursor(@Observes ReloadCountCursorEvent event) {
        Log.d(TAG, "Refreshing count cursor");
        restartCountLoading();
    }

    private void startListLoading() {
        Log.d(TAG, "Creating relevant list cursor for " + mMainView);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case TASK:
            case TASK_LIST:
                lm.initLoader(LOADER_ID_TASK_LIST_LOADER, null, TASK_LIST_LOADER_CALLBACKS);
                break;
            case CONTEXT_LIST:
                lm.initLoader(LOADER_ID_CONTEXT_LIST_LOADER, null, CONTEXT_LIST_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void startCountLoading() {
        Log.d(TAG, "Creating relevant count cursor for " + mMainView);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case CONTEXT_LIST:
                lm.initLoader(LOADER_ID_CONTEXT_TASK_COUNT_LOADER, null, CONTEXT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void restartListLoading() {
        Log.d(TAG, "Refreshing list cursor");
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case TASK:
            case TASK_LIST:
                lm.restartLoader(LOADER_ID_TASK_LIST_LOADER, null, TASK_LIST_LOADER_CALLBACKS);
                break;
            case CONTEXT_LIST:
                lm.restartLoader(LOADER_ID_CONTEXT_LIST_LOADER, null, CONTEXT_LIST_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void restartCountLoading() {
        Log.d(TAG, "Refreshing count cursor");
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (mMainView.getViewMode()) {
            case CONTEXT_LIST:
                lm.restartLoader(LOADER_ID_CONTEXT_TASK_COUNT_LOADER, null, CONTEXT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    /**
     * Loader callbacks for tasks list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> TASK_LIST_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    final TaskListContext listContext = mTaskListContext;
                    return TaskListAdaptor.createLoader(mActivity, listContext);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    Log.d(TAG, "In TASK_LIST_LOADER_CALLBACKS.onLoadFinished");

                    mEventManager.fire(new TaskListCursorLoadedEvent(c, mTaskListContext));
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    /**
     * Loader callbacks for message list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> CONTEXT_LIST_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new ContextCursorLoader(mActivity);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
                    Log.d(TAG, "In TASK_LIST_LOADER_CALLBACKS.onLoadFinished");

                    mEventManager.fire(new ContextListCursorLoadedEvent(c));
                }


                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    /**
     * Loader callbacks for task counts.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> CONTEXT_TASK_COUNT_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new ContextTaskCountCursorLoader(mActivity);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
                    Log.d(TAG, "In CONTEXT_TASK_COUNT_LOADER_CALLBACKS.onLoadFinished");

                    mEventManager.fire(new ContextTaskCountCursorLoadedEvent(cursor));
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private static class ContextTaskCountCursorLoader extends android.support.v4.content.CursorLoader {
        protected final Context mContext;

        private TaskSelector mSelector;

        public ContextTaskCountCursorLoader(Context context) {
            // Initialize with no where clause.  We'll set it later.
            super(context, ContextProvider.Contexts.CONTEXT_TASKS_CONTENT_URI,
                    ContextProvider.Contexts.FULL_TASK_PROJECTION, null, null,
                    null);
            mSelector = TaskSelector.newBuilder().applyListPreferences(context,
                    ListSettingsCache.findSettings(ListQuery.context)).build();
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

}
