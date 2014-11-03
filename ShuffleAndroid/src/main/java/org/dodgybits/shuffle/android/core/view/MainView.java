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
package org.dodgybits.shuffle.android.core.view;

import android.os.Bundle;
import org.dodgybits.shuffle.android.list.model.ListQuery;

/**
 * Immutable description of the current view of the main activity.
 */
public class MainView {
    public static final String QUERY_NAME = "queryName";

    private static final String VIEW_MODE_KEY = "view-mode";
    private static final String LIST_QUERY_KEY = "list-query";
    private static final String ENTITIY_ID_KEY = "entityId";
    private static final String SEARCH_QUERY_KEY = "search-query";

    private final ViewMode mViewMode;
    private final ListQuery mListQuery;
    private final Long mEntityId;
    private final String mSearchQuery;


    public static MainView createView(ViewMode viewMode) {
        return new MainView(viewMode, null, null, null);
    }

    public static MainView createSearchList(String searchQuery) {
        return new MainView(ViewMode.SEARCH_RESULTS_LIST, ListQuery.search, null, searchQuery);
    }

    public static MainView createSearchListItem(String searchQuery, Long taskId) {
        return new MainView(ViewMode.SEARCH_RESULTS_TASK, ListQuery.search, taskId, searchQuery);
    }

    public static MainView createContextTaskList(Long contextId) {
        return new MainView(ViewMode.CONTEXT_LIST, ListQuery.context, contextId, null);
    }

    public static MainView createProjectTaskList(Long projectId) {
        return new MainView(ViewMode.PROJECT_LIST, ListQuery.project, projectId, null);
    }

    public static MainView createView(ListQuery listQuery) {
        ViewMode mode;
        switch (listQuery) {
            case all:
            case custom:
            case dueNextMonth:
            case dueNextWeek:
            case dueToday:
            case inbox:
            case nextTasks:
            case tickler:
                mode = ViewMode.TASK_LIST;
                break;
            case project:
                mode = ViewMode.PROJECT_LIST;
                break;
            case context:
                mode = ViewMode.CONTEXT_LIST;
                break;
            default:
                mode = ViewMode.UNKNOWN;
        }
        return new MainView(mode, listQuery, null, null);
    }

    private MainView(ViewMode viewMode, ListQuery listQuery, Long entityId, String searchQuery) {
        mViewMode = viewMode;
        mListQuery = listQuery;
        mEntityId = entityId;
        mSearchQuery = searchQuery;
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    public ListQuery getListQuery() {
        return mListQuery;
    }

    public Long getEntityId() {
        return mEntityId;
    }

    /**
     * Restoring from a saved state restores only the mode.
     *
     * @param inState
     */
    public static MainView handleRestore(Bundle inState) {
        if (inState == null) {
            return MainView.createView(ViewMode.UNKNOWN);
        }
        // Restore the previous view, and UNKNOWN if nothing exists.
        final String modeName = inState.getString(VIEW_MODE_KEY, ViewMode.UNKNOWN.name());
        ViewMode mode = ViewMode.valueOf(modeName);
        ListQuery listQuery = null;
        String queryName = inState.getString(LIST_QUERY_KEY, null);
        if (queryName != null) {
            listQuery = ListQuery.valueOf(queryName);
        }
        Long entityId = inState.getLong(ENTITIY_ID_KEY, -1);
        if (entityId == -1) {
            entityId = null;
        }
        String searchQuery = inState.getString(SEARCH_QUERY_KEY, null);
        return new MainView(mode, listQuery, entityId, searchQuery);
    }

    /**
     * Save the existing mode only. Does not save the existing listeners.
     * @param outState
     */
    public void handleSaveInstanceState(Bundle outState) {
        if (outState == null) {
            return;
        }
        outState.putString(VIEW_MODE_KEY, mViewMode.name());
        if (mListQuery != null) {
            outState.putString(LIST_QUERY_KEY, mListQuery.name());
        }
        if (mEntityId != null) {
            outState.putLong(ENTITIY_ID_KEY, mEntityId.longValue());
        }
        if (mSearchQuery != null) {
            outState.putString(SEARCH_QUERY_KEY, mSearchQuery);
        }
    }

    @Override
    public String toString() {
        return "MainView{" +
                "mViewMode=" + mViewMode +
                ", mListQuery=" + mListQuery +
                ", mEntityId=" + mEntityId +
                ", mSearchQuery='" + mSearchQuery + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MainView mainView = (MainView) o;

        if (mEntityId != null ? !mEntityId.equals(mainView.mEntityId) : mainView.mEntityId != null) return false;
        if (mListQuery != mainView.mListQuery) return false;
        if (mSearchQuery != null ? !mSearchQuery.equals(mainView.mSearchQuery) : mainView.mSearchQuery != null)
            return false;
        if (mViewMode != mainView.mViewMode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mViewMode.hashCode();
        result = 31 * result + (mListQuery != null ? mListQuery.hashCode() : 0);
        result = 31 * result + (mEntityId != null ? mEntityId.hashCode() : 0);
        result = 31 * result + (mSearchQuery != null ? mSearchQuery.hashCode() : 0);
        return result;
    }
}
