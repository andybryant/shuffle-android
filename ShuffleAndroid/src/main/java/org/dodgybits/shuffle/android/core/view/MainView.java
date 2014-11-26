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

/**
 * Immutable description of the current view of the main activity.
 */
public class MainView implements Parcelable {
    private static final String TAG = "MainView";

    public static final String QUERY_NAME = "queryName";

    @NonNull
    private ViewMode mViewMode = ViewMode.TASK_LIST;

    @NonNull
    private ListQuery mListQuery = ListQuery.inbox;

    @NonNull
    private Id mEntityId = Id.NONE;

    private String mSearchQuery = null;

    private int mSelectedIndex = -1;

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
    }

    public static final Parcelable.Creator<MainView> CREATOR
            = new Parcelable.Creator<MainView>() {

        @Override
        public MainView createFromParcel(Parcel source) {
            ViewMode viewMode = ViewMode.valueOf(source.readString());
            ListQuery listQuery = ListQuery.valueOf(source.readString());
            Id entityId = Id.create(source.readLong());
            String searchQuery = source.readString();
            int selectedIndex = source.readInt();
            MainView.Builder builder = MainView.newBuilder()
                    .setViewMode(viewMode)
                    .setListQuery(listQuery)
                    .setEntityId(entityId)
                    .setSearchQuery(searchQuery)
                    .setSelectedIndex(selectedIndex);

            return builder.build();
        }

        @Override
        public MainView[] newArray(int size) {
            return new MainView[size];
        }
    };

    private MainView() {
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

    @Override
    public String toString() {
        return "MainView{" +
                "mViewMode=" + mViewMode +
                ", mListQuery=" + mListQuery +
                ", mEntityId=" + mEntityId +
                ", mSearchQuery='" + mSearchQuery + '\'' +
                ", mSelectedIndex=" + mSelectedIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MainView mainView = (MainView) o;

        if (!mEntityId.equals(mainView.mEntityId)) return false;
        if (mSelectedIndex != mainView.mSelectedIndex) return false;
        if (mListQuery != mainView.mListQuery) return false;
        if (mSearchQuery != null ? !mSearchQuery.equals(mainView.mSearchQuery) : mainView.mSearchQuery != null)
            return false;
        if (mViewMode != mainView.mViewMode) return false;

        return true;
    }

    public Builder builderFrom() {
        return newBuilder().mergeFrom(this);
    }

    public static Builder newBuilder() {
        return Builder.create();
    }

    public static class Builder {
        private MainView mResult;

        private Builder() {
        }

        private static Builder create() {
            Builder builder = new Builder();
            builder.mResult = new MainView();
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

        private Builder deriveViewMode() {
            ViewMode mode;
            boolean itemView = mResult.mSelectedIndex >= 0;
            switch (mResult.mListQuery) {
                case search:
                    mode = itemView ? ViewMode.SEARCH_RESULTS_TASK : ViewMode.SEARCH_RESULTS_LIST;
                    break;
                case project:
                    mode = ViewMode.PROJECT_LIST;
                    break;
                case context:
                    mode = ViewMode.CONTEXT_LIST;
                    break;
                default:
                    mode = itemView ? ViewMode.TASK : ViewMode.TASK_LIST;
                    break;
            }
            setViewMode(mode);
            return this;
        }

        public Builder mergeFrom(MainView mainView) {
            setViewMode(mainView.mViewMode);
            setListQuery(mainView.mListQuery);
            setEntityId(mainView.mEntityId);
            setSearchQuery(mainView.mSearchQuery);
            setSelectedIndex(mainView.mSelectedIndex);

            return this;
        }

        public MainView build() {
            if (mResult == null) {
                throw new IllegalStateException(
                        "build() has already been called on this Builder.");
            }
            deriveViewMode();
            MainView returnMe = mResult;
            mResult = null;

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, returnMe.toString());
            }

            return returnMe;
        }

    }

}
