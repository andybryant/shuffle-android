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
        Location location = builder.build();
        return location;
    }

    public static Location newTaskWithContext(Id entityId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setListQuery(ListQuery.context);
        builder.setEntityId(entityId);
        Location location = builder.build();
        return location;
    }

    public static Location newTaskWithProject(Id entityId) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        builder.setListQuery(ListQuery.project);
        builder.setEntityId(entityId);
        Location location = builder.build();
        return location;
    }

    public static Location newTask() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(EditTask);
        Location location = builder.build();
        return location;
    }

    public static Location editTask(Id entityId) {
        return editEntity(entityId, EditTask);
    }

    public static Location editContext(Id entityId) {
        return editEntity(entityId, EditContext);
    }

    public static Location editProject(Id entityId) {
        return editEntity(entityId, EditProject);
    }

    public static Location viewTask(ListQuery listQuery, int selectedIndex) {
        return viewTask(listQuery, Id.NONE, selectedIndex);
    }

    public static Location viewTask(ListQuery listQuery, Id entityId, int selectedIndex) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(TaskList);
        builder.setListQuery(listQuery);
        builder.setSelectedIndex(selectedIndex);
        builder.setEntityId(entityId);
        Location location = builder.build();
        return location;
    }

    public static Location viewContextList() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(ContextList);
        builder.setListQuery(ListQuery.context);
        Location location = builder.build();
        return location;
    }

    public static Location viewProjectList() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(ProjectList);
        builder.setListQuery(ListQuery.project);
        Location location = builder.build();
        return location;
    }

    public static Location viewHelp() {
        return viewHelp(null);
    }

    public static Location viewHelp(ListQuery listQuery) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(Help);
        builder.setListQuery(listQuery);
        Location location = builder.build();
        return location;
    }

    public static Location viewSettings() {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(Preferences);
        Location location = builder.build();
        return location;
    }

    private static Location editEntity(Id entityId, LocationActivity locationActivity) {
        Location.Builder builder = Location.newBuilder();
        builder.setLocationActivity(locationActivity);
        builder.setEntityId(entityId);
        Location location = builder.build();
        return location;
    }
    
    
    @NonNull
    private ViewMode mViewMode = ViewMode.TASK_LIST;

    @NonNull
    private ListQuery mListQuery = ListQuery.inbox;

    @NonNull
    private Id mEntityId = Id.NONE;

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
        dest.writeLong(mEntityId.getId());
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
            Id entityId = Id.create(source.readLong());
            String searchQuery = source.readString();
            int selectedIndex = source.readInt();
            LocationActivity locationActivity = LocationActivity.valueOf(source.readString());
            Location.Builder builder = Location.newBuilder()
                    .setViewMode(viewMode)
                    .setListQuery(listQuery)
                    .setEntityId(entityId)
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

    public Id getEntityId() {
        return mEntityId;
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

    public boolean isChildView() {
        return mSelectedIndex >= 0 || mEntityId.isInitialised();
    }

    public boolean isSameActivity(Location location) {
        if (location == null) return false;
        return location.mLocationActivity == mLocationActivity;
    }

    @Override
    public String toString() {
        return "Location{" +
                "mViewMode=" + mViewMode +
                ", mListQuery=" + mListQuery +
                ", mEntityId=" + mEntityId +
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

        if (!mEntityId.equals(location.mEntityId)) return false;
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
        result = 31 * result + mEntityId.hashCode();
        result = 31 * result + (mSearchQuery != null ? mSearchQuery.hashCode() : 0);
        result = 31 * result + mSelectedIndex;
        result = 31 * result + mLocationActivity.hashCode();
        return result;
    }

    public Builder builderFrom() {
        return newBuilder().mergeFrom(this);
    }

    public static Builder newBuilder() {
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

        public Id getEntityId() {
            return mResult.mEntityId;
        }

        public Builder setEntityId(Id entityId) {
            mResult.mEntityId = entityId;
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
                    if (mResult.getEntityId().isInitialised()) {
                        mode = itemView ? ViewMode.TASK : ViewMode.TASK_LIST;
                    } else {
                        mode = ViewMode.PROJECT_LIST;
                    }
                    break;
                case context:
                    if (mResult.getEntityId().isInitialised()) {
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
            setEntityId(location.mEntityId);
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
            } else if (mResult.getEntityId().isInitialised()) {
                mResult.mEntityId = Id.NONE;
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
