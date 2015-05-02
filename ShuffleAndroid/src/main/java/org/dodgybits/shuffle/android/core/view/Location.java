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

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import org.dodgybits.shuffle.android.core.model.Id;
import org.dodgybits.shuffle.android.list.model.ListQuery;
import org.dodgybits.shuffle.android.list.view.task.TaskListContext;

import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.ContextList;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.EditContext;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.EditProject;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.EditTask;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.Help;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.Preferences;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.ProjectList;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.TaskList;
import static org.dodgybits.shuffle.android.core.view.Location.LocationActivity.TaskSearch;

/**
 * Immutable description of the current view of the main activity.
 */
public class Location implements Parcelable {
    private static final String TAG = "Location";

    public static final String QUERY_NAME = "queryName";
    public static final String SELECTED_INDEX = "selectedIndex";

    public static Location searchTasks(String searchQuery) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(TaskSearch);
        builder.setSearchQuery(searchQuery);
        return builder.build();
    }

    public static Location home() {
        return viewTaskList(ListQuery.inbox);
    }

    public static Location newTaskFromTaskListContext(TaskListContext listContext) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setListQuery(listContext.getListQuery());
        builder.setContextId(listContext.getContextId());
        builder.setProjectId(listContext.getProjectId());
        return builder.build();
    }

    public static Location newTaskWithContext(Id contextId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setListQuery(ListQuery.context);
        builder.setContextId(contextId);
        return builder.build();
    }

    public static Location newTaskWithProject(Id projectId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setListQuery(ListQuery.project);
        builder.setProjectId(projectId);
        return builder.build();
    }

    public static Location newTask() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        Location location = builder.build();
        return location;
    }

    public static Location editTask(Id taskId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setTaskId(taskId);
        return builder.build();
    }

    public static Location newContext() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditContext);
        return builder.build();
    }

    public static Location editContext(Id contextId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditContext);
        builder.setContextId(contextId);
        return builder.build();
    }

    public static Location newProject() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditProject);
        return builder.build();
    }

    public static Location editProject(Id projectId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditProject);
        builder.setProjectId(projectId);
        return builder.build();
    }

    public static Location viewTask(ListQuery listQuery, int selectedIndex) {
        return viewTask(listQuery, Id.NONE, Id.NONE, selectedIndex);
    }

    public static Location viewTask(TaskListContext listContext, int selectedIndex) {
        return viewTask(listContext.getListQuery(),
                listContext.getProjectId(), listContext.getContextId(), selectedIndex);
    }

    public static Location viewTask(ListQuery listQuery, Id projectId, Id contextId, int selectedIndex) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(TaskList);
        builder.setListQuery(listQuery);
        builder.setSelectedIndex(selectedIndex);
        builder.setProjectId(projectId);
        builder.setContextId(contextId);
        return builder.build();
    }

    public static Location viewTaskList(TaskListContext listContext) {
        return viewTaskList(listContext.getListQuery(),
                listContext.getProjectId(), listContext.getContextId());
    }

    public static Location viewTaskList(ListQuery listQuery) {
        return viewTaskList(listQuery, Id.NONE, Id.NONE);
    }

    public static Location viewTaskList(ListQuery listQuery, Id projectId, Id contextId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(TaskList);
        builder.setListQuery(listQuery);
        builder.setProjectId(projectId);
        builder.setContextId(contextId);
        return builder.build();
    }

    public static Location viewContextList() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(ContextList);
        builder.setListQuery(ListQuery.context);
        return builder.build();
    }

    public static Location viewProjectList() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(ProjectList);
        builder.setListQuery(ListQuery.project);
        return builder.build();
    }

    public static Location viewHelp() {
        return viewHelp(null);
    }

    public static Location viewHelp(ListQuery listQuery) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(Help);
        builder.setListQuery(listQuery);
        return builder.build();
    }

    public static Location viewSettings() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(Preferences);
        return builder.build();
    }

    @NonNull
    private ViewMode mViewMode = ViewMode.TASK_LIST;

    @NonNull
    private ListQuery mListQuery = ListQuery.inbox;

    @NonNull
    private Id mTaskId = Id.NONE;

    @NonNull
    private Id mContextId = Id.NONE;

    @NonNull
    private Id mProjectId = Id.NONE;

    private String mSearchQuery = null;

    private int mSelectedIndex = -1;

    @NonNull
    private LocationActivity mLocationActivity;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mViewMode.name());
        dest.writeString(mListQuery.name());
        dest.writeLong(mTaskId.getId());
        dest.writeLong(mContextId.getId());
        dest.writeLong(mProjectId.getId());
        dest.writeString(mSearchQuery);
        dest.writeInt(mSelectedIndex);
        dest.writeString(mLocationActivity.name());
    }

    public static final Parcelable.Creator<Location> CREATOR
            = new Parcelable.Creator<Location>() {

        @Override
        public Location createFromParcel(Parcel source) {
            ViewMode viewMode = ViewMode.valueOf(source.readString());
            ListQuery listQuery = ListQuery.valueOf(source.readString());
            Id taskId = Id.create(source.readLong());
            Id contextId = Id.create(source.readLong());
            Id projectId = Id.create(source.readLong());
            String searchQuery = source.readString();
            int selectedIndex = source.readInt();
            LocationActivity locationActivity = LocationActivity.valueOf(source.readString());
            Location.Builder builder = Location.newBuilder()
                    .setViewMode(viewMode)
                    .setListQuery(listQuery)
                    .setTaskId(taskId)
                    .setContextId(contextId)
                    .setProjectId(projectId)
                    .setSearchQuery(searchQuery)
                    .setSelectedIndex(selectedIndex)
                    .setLocationActivity(locationActivity);

            return builder.build();
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };

    private Location() {
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    public ListQuery getListQuery() {
        return mListQuery;
    }

    @NonNull
    public Id getTaskId() {
        return mTaskId;
    }

    @NonNull
    public Id getContextId() {
        return mContextId;
    }

    @NonNull
    public Id getProjectId() {
        return mProjectId;
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    @NonNull
    public LocationActivity getLocationActivity() {
        return mLocationActivity;
    }

    public boolean isListView() {
        return mViewMode.isListMode();
    }

    public boolean isSameActivity(Location location) {
        if (location == null) return false;
        return location.mLocationActivity == mLocationActivity;
    }

    public Location parent() {
        return builderFrom().parentView().build();
    }

    @Override
    public String toString() {
        return "Location{" +
                "mViewMode=" + mViewMode +
                ", mListQuery=" + mListQuery +
                ", mTaskId=" + mTaskId +
                ", mContextId=" + mContextId +
                ", mProjectId=" + mProjectId +
                ", mSearchQuery='" + mSearchQuery + '\'' +
                ", mSelectedIndex=" + mSelectedIndex +
                ", mLocationActivity=" + mLocationActivity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (!mTaskId.equals(location.mTaskId)) return false;
        if (!mContextId.equals(location.mContextId)) return false;
        if (!mProjectId.equals(location.mProjectId)) return false;
        if (mSelectedIndex != location.mSelectedIndex) return false;
        if (mListQuery != location.mListQuery) return false;
        if (mLocationActivity != location.mLocationActivity) return false;
        if (mSearchQuery != null ? !mSearchQuery.equals(location.mSearchQuery) : location.mSearchQuery != null)
            return false;
        if (mViewMode != location.mViewMode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mViewMode.hashCode();
        result = 31 * result + mListQuery.hashCode();
        result = 31 * result + mTaskId.hashCode();
        result = 31 * result + mContextId.hashCode();
        result = 31 * result + mProjectId.hashCode();
        result = 31 * result + (mSearchQuery != null ? mSearchQuery.hashCode() : 0);
        result = 31 * result + mSelectedIndex;
        result = 31 * result + mLocationActivity.hashCode();
        return result;
    }

    public Builder builderFrom() {
        return newBuilder().mergeFrom(this);
    }

    static Builder newBuilder() {
        return Builder.create();
    }

    public static class Builder {
        private Location mResult;

        private Builder() {
        }

        private static Builder create() {
            Builder builder = new Builder();
            builder.mResult = new Location();
            return builder;
        }

        private ViewMode getViewMode() {
            return mResult.mViewMode;
        }

        private Builder setViewMode(ViewMode viewMode) {
            mResult.mViewMode = viewMode;
            return this;
        }

        public ListQuery getListQuery() {
            return mResult.mListQuery;
        }

        public Builder setListQuery(ListQuery value) {
            mResult.mListQuery = value;
            return this;
        }

        public Id getTaskId() {
            return mResult.mTaskId;
        }

        public Builder setTaskId(Id entityId) {
            mResult.mTaskId = entityId;
            return this;
        }

        public Id getContextId() {
            return mResult.mContextId;
        }

        public Builder setContextId(Id entityId) {
            mResult.mContextId = entityId;
            return this;
        }

        public Id getProjectId() {
            return mResult.mProjectId;
        }

        public Builder setProjectId(Id entityId) {
            mResult.mProjectId = entityId;
            return this;
        }

        public String getSearchQuery() {
            return mResult.mSearchQuery;
        }

        public Builder setSearchQuery(String searchQuery) {
            mResult.mSearchQuery = searchQuery;
            return this;
        }

        public int getSelectedIndex() {
            return mResult.mSelectedIndex;
        }

        public Builder setSelectedIndex(int index) {
            mResult.mSelectedIndex = index;
            return this;
        }

        public LocationActivity getLocationActivity() {
            return mResult.mLocationActivity;
        }

        public Builder setLocationActivity(LocationActivity locationActivity) {
            mResult.mLocationActivity = locationActivity;
            return this;
        }

        private Builder deriveViewMode() {
            ViewMode mode;
            boolean itemView = mResult.mSelectedIndex >= 0;
            switch (mResult.mListQuery) {
                case search:
                    mode = itemView ? ViewMode.SEARCH_RESULTS_TASK : ViewMode.SEARCH_RESULTS_LIST;
                    break;
                case project:
                    if (mResult.getProjectId().isInitialised()) {
                        mode = itemView ? ViewMode.TASK : ViewMode.TASK_LIST;
                    } else {
                        mode = ViewMode.PROJECT_LIST;
                    }
                    break;
                case context:
                    if (mResult.getContextId().isInitialised()) {
                        mode = itemView ? ViewMode.TASK : ViewMode.TASK_LIST;
                    } else {
                        mode = ViewMode.CONTEXT_LIST;
                    }
                    break;
                default:
                    mode = itemView ? ViewMode.TASK : ViewMode.TASK_LIST;
                    break;
            }
            setViewMode(mode);
            return this;
        }

        public Builder mergeFrom(Location location) {
            setViewMode(location.mViewMode);
            setListQuery(location.mListQuery);
            setTaskId(location.mTaskId);
            setContextId(location.mContextId);
            setProjectId(location.mProjectId);
            setSearchQuery(location.mSearchQuery);
            setSelectedIndex(location.mSelectedIndex);
            setLocationActivity(location.mLocationActivity);

            return this;
        }

        public Builder parentView() {
            deriveViewMode();
            if (mResult.mViewMode == ViewMode.TASK) {
                mResult.mSelectedIndex = -1;
                deriveViewMode();
            } else if (mResult.getProjectId().isInitialised()) {
                mResult.mProjectId = Id.NONE;
                deriveViewMode();
            } else if (mResult.getContextId().isInitialised()) {
                mResult.mContextId = Id.NONE;
                deriveViewMode();
            } else {
                Log.w(TAG, "No parent view for top level view " + mResult);
            }
            return this;
        }

        public Location build() {
            if (mResult == null) {
                throw new IllegalStateException(
                        "build() has already been called on this Builder.");
            }
            deriveViewMode();
            Location returnMe = mResult;
            mResult = null;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, returnMe.toString());
            }

            return returnMe;
        }

    }

    public static enum LocationActivity {
        TaskSearch, TaskList, ProjectList, ContextList,
        Help, Preferences,
        EditTask, EditProject, EditContext
    }

}
