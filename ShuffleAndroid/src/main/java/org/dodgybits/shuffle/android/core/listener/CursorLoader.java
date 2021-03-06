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
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseIntArray;
import com.google.inject.Inject;
import org.dodgybits.shuffle.android.core.content.TaskCursorLoader;
import org.dodgybits.shuffle.android.core.event.*;
import org.dodgybits.shuffle.android.core.model.persistence.TaskPersister;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ContextSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.ProjectSelector;
import org.dodgybits.shuffle.android.core.model.persistence.selector.TaskSelector;
import org.dodgybits.shuffle.android.core.view.Location;
import org.dodgybits.shuffle.android.core.view.ViewMode;
import org.dodgybits.shuffle.android.list.content.ContextCursorLoader;
import org.dodgybits.shuffle.android.list.content.ProjectCursorLoader;
import org.dodgybits.shuffle.android.list.event.ListSettingsUpdatedEvent;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.model.ListSettingsCache;
import org.dodgybits.shuffle.android.persistence.provider.ContextProvider;
import org.dodgybits.shuffle.android.persistence.provider.ProjectProvider;
import roboguice.event.EventManager;
import roboguice.event.Observes;
import roboguice.inject.ContextSingleton;

@ContextSingleton
public class CursorLoader {
    private static final String TAG = "CursorLoader";

    private FragmentActivity mActivity;

    private EventManager mEventManager;

    private Location mLocation;

    private TaskSelector mTaskSelector;

    private TaskPersister mTaskPersister;

    @Inject
    public CursorLoader(Activity activity, EventManager eventManager, TaskPersister taskPersister) {
        mActivity = (FragmentActivity) activity;
        mEventManager = eventManager;
        mTaskPersister = taskPersister;
    }

    private void onViewUpdated(@Observes LocationUpdatedEvent event) {
        Log.d(TAG, "Received view update event " + event);
        mLocation = event.getLocation();
        mTaskSelector = TaskSelector.fromLocation(mActivity, mLocation);

        // delay call so that when list is reloaded straight away,
        // other location update handlers have all been called
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                startListLoading(mLocation);
                startCountLoading(mLocation);
            }
        });
    }

    private void onListSettingsUpdated(@Observes ListSettingsUpdatedEvent event) {
        if (event.getListQuery().equals(mLocation.getListQuery())) {
            mTaskSelector = TaskSelector.fromLocation(mActivity, mLocation);
            // our list settings changed - reload list
            startListLoading(mLocation);
            startCountLoading(mLocation);
        }
    }

    private void onReloadListCursor(@Observes LoadListCursorEvent event) {
        mTaskSelector = TaskSelector.fromLocation(mActivity, mLocation);
        restartListLoading(event.getLocation());
    }

    private void onReloadCountCursor(@Observes LoadCountCursorEvent event) {
        restartCountLoading(event.getLocation());
    }

    private void startListLoading(Location location) {
        ViewMode viewMode = location.getViewMode();
        int listId = listId();
        Log.d(TAG, "Creating relevant list cursor for " + viewMode + " listId " + listId);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (viewMode) {
            case TASK:
            case TASK_LIST:
            case SEARCH_RESULTS_LIST:
            case SEARCH_RESULTS_TASK:
                lm.initLoader(listId, null, TASK_LIST_LOADER_CALLBACKS);
                if (location.getListQuery() == ListQuery.deleted) {
                    lm.initLoader(listId, null, CONTEXT_LIST_LOADER_CALLBACKS);
                    lm.initLoader(listId, null, PROJECT_LIST_LOADER_CALLBACKS);
                }
                break;
            case CONTEXT_LIST:
                lm.initLoader(listId, null, CONTEXT_LIST_LOADER_CALLBACKS);
                break;
            case PROJECT_LIST:
                lm.initLoader(listId, null, PROJECT_LIST_LOADER_CALLBACKS);
                break;
            case DELETED_LIST:
                lm.initLoader(("task" + listId).hashCode(), null, TASK_LIST_LOADER_CALLBACKS);
                lm.initLoader(("context" + listId).hashCode(), null, CONTEXT_LIST_LOADER_CALLBACKS);
                lm.initLoader(("project" + listId).hashCode(), null, PROJECT_LIST_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private int listId() {
        return mTaskSelector.hashCode();
    }

    private int countId() {
        return 31 * mTaskSelector.hashCode();
    }

    private void startCountLoading(Location location) {
        ViewMode viewMode = location.getViewMode();
        Log.d(TAG, "Creating relevant count cursor for " + viewMode);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (viewMode) {
            case CONTEXT_LIST:
                lm.initLoader(countId(), null, CONTEXT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            case PROJECT_LIST:
                lm.initLoader(countId(), null, PROJECT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            default:
                // TODO
        }
    }

    private void restartListLoading(Location location) {
        ViewMode viewMode = location.getViewMode();
        int listId = listId();
        Log.d(TAG, "Refreshing list cursor " + viewMode + " listId " + listId);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (viewMode) {
            case TASK:
            case TASK_LIST:
            case SEARCH_RESULTS_LIST:
            case SEARCH_RESULTS_TASK:
                lm.restartLoader(listId, null, TASK_LIST_LOADER_CALLBACKS);
                break;
            case CONTEXT_LIST:
                lm.restartLoader(listId, null, CONTEXT_LIST_LOADER_CALLBACKS);
                break;
            case PROJECT_LIST:
                lm.restartLoader(listId, null, PROJECT_LIST_LOADER_CALLBACKS);
                break;
            case DELETED_LIST:
                lm.restartLoader(("task" + listId).hashCode(), null, TASK_LIST_LOADER_CALLBACKS);
                lm.restartLoader(("context" + listId).hashCode(), null, CONTEXT_LIST_LOADER_CALLBACKS);
                lm.restartLoader(("project" + listId).hashCode(), null, PROJECT_LIST_LOADER_CALLBACKS);
                break;
            default:
                // nothing to do
        }
    }

    private void restartCountLoading(Location location) {
        ViewMode viewMode = location.getViewMode();
        Log.d(TAG, "Refreshing count cursor " + viewMode);
        final LoaderManager lm = mActivity.getSupportLoaderManager();
        switch (viewMode) {
            case CONTEXT_LIST:
                lm.restartLoader(countId(), null, CONTEXT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            case PROJECT_LIST:
                lm.restartLoader(countId(), null, PROJECT_TASK_COUNT_LOADER_CALLBACKS);
                break;
            default:
                // not needed for other views
        }
    }

    /**
     * Loader callbacks for tasks list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> TASK_LIST_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return TaskCursorLoader.createLoader(mActivity, mTaskSelector);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, final Cursor c) {
                    Log.d(TAG, "In TASK_LIST_LOADER_CALLBACKS.onLoadFinished");

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mEventManager.fire(new TaskListCursorLoadedEvent(c, mLocation));
                        }
                    });
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
                    ContextSelector selector = ContextSelector.newBuilder().
                            applyListPreferences(mActivity,
                                    ListSettingsCache.findSettings(mLocation.getListQuery())).build();
                    return new ContextCursorLoader(mActivity, selector);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, final Cursor c) {
                    Log.d(TAG, "In CONTEXT_LIST_LOADER_CALLBACKS.onLoadFinished");

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mEventManager.fire(new ContextListCursorLoadedEvent(c));
                        }
                    });
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
                public void onLoadFinished(Loader<Cursor> loader, final Cursor cursor) {
                    Log.d(TAG, "In CONTEXT_TASK_COUNT_LOADER_CALLBACKS.onLoadFinished");

                    final SparseIntArray taskCountArray = mTaskPersister.readCountArray(cursor);
                    cursor.close();

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mEventManager.fire(new ContextTaskCountLoadedEvent(taskCountArray));
                        }
                    });
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

    /**
     * Loader callbacks for message list.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> PROJECT_LIST_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    ProjectSelector selector = ProjectSelector.newBuilder().
                            applyListPreferences(mActivity,
                                    ListSettingsCache.findSettings(mLocation.getListQuery())).build();
                    return new ProjectCursorLoader(mActivity, selector);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, final Cursor c) {
                    Log.d(TAG, "In PROJECT_LIST_LOADER_CALLBACKS.onLoadFinished");

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mEventManager.fire(new ProjectListCursorLoadedEvent(c));
                        }
                    });
                }


                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    /**
     * Loader callbacks for task counts.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> PROJECT_TASK_COUNT_LOADER_CALLBACKS =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new ProjectTaskCountCursorLoader(mActivity);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, final Cursor cursor) {
                    Log.d(TAG, "In PROJECT_TASK_COUNT_LOADER_CALLBACKS.onLoadFinished");

                    final SparseIntArray taskCountArray = mTaskPersister.readCountArray(cursor);
                    cursor.close();

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            mEventManager.fire(new ProjectTaskCountLoadedEvent(taskCountArray));
                        }
                    });
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                }
            };

    private static class ProjectTaskCountCursorLoader extends android.support.v4.content.CursorLoader {
        protected final Context mContext;

        private TaskSelector mSelector;

        public ProjectTaskCountCursorLoader(Context context) {
            // Initialize with no where clause.  We'll set it later.
            super(context, ProjectProvider.Projects.PROJECT_TASKS_CONTENT_URI,
                    ProjectProvider.Projects.FULL_TASK_PROJECTION, null, null,
                    null);
            mSelector = TaskSelector.newBuilder().applyListPreferences(context,
                    ListSettingsCache.findSettings(ListQuery.project)).build();
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
